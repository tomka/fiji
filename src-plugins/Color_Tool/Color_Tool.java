import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import fiji.util.gui.GenericDialogPlus;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.io.ImgSaver;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.numeric.ARGBType;

public class Color_Tool< T extends RealType< T > > implements PlugIn {
	protected enum Mode { Interior, Bound };
	protected String rawPath = "";
	protected String pathPattern;
	protected final String patternStart = "{";
	protected final String patternEnd = "}";
	protected String csvFilePath = "";
	protected String outputDirectory = "";
	protected Mode mode;
	protected String delimiter = ",";

	protected class ImageInfo {
		public String path;
		public String filename;
		public String outputPath;
		public boolean modified;

		public ImageInfo( String path ) {
			this.path = path;
			this.filename = new File( path ).getName();
			this.outputPath= outputDirectory + filename;
			this.modified = false;
		}
	}

	protected class CellColorInfo {
		public Integer frame;
		public long x;
		public long y;
		public CellColorInfo( Integer f, long x, long y ) {
			this.frame = f;
			this.x = x;
			this.y = y;
		}
	}

	public void run(String arg) {
			if ( arg.equals( "about" ) ) {
			showAbout();
			return;
		}

		if ( showDialog() )
			process();
	}

	private boolean showDialog() {
		GenericDialogPlus gd = new GenericDialogPlus("Color Tool");

		//gd.addStringField("Image directory", "/home/tom/dev/rapha/Coloring/SegMasks/Test_{nn}.png");
		gd.addFileField( "Image file pattern -- {nn}", rawPath );
		//gd.addStringField("Output directory", "/home/tom/dev/rapha/Coloring/output");
		gd.addDirectoryField( "Output directory", outputDirectory );
		//gd.addStringField("CSV file", "/home/tom/dev/rapha/Coloring/center.csv");
		gd.addFileField( "CSV file", csvFilePath );
		gd.addStringField("Delimiter", delimiter);

		gd.showDialog();
		if ( gd.wasCanceled() )
			return false;

		// get entered values
		rawPath = gd.getNextString();
		outputDirectory = gd.getNextString();
		csvFilePath = gd.getNextString();
		delimiter = gd.getNextString();
		mode = Mode.Interior;

		// correct information if needed
		String fileSep = System.getProperty("file.separator");
		if ( outputDirectory.lastIndexOf( fileSep ) != outputDirectory.length() - 1 ) {
			outputDirectory = outputDirectory + fileSep;
		}

		// set up some more path information
		try {
			pathPattern = rawPath.substring(
				rawPath.indexOf(patternStart) + 1,
				rawPath.lastIndexOf(patternEnd));
		} catch (java.lang.StringIndexOutOfBoundsException e ) {
			IJ.log( "Please specify source file information including a pattern {nn}" );
			return false;
		}
		return true;
	}

	public Vector<String[]> readCSV( String path ) {
		Vector<String[]> table = new Vector<String[]>();
		try {
			BufferedReader in = new BufferedReader( new FileReader( new File( path ) ) );
			String readString;
			while ((readString = in.readLine()) != null) {
				String[] fields = readString.split( delimiter );
				// Trim whitespace and remove quotes
				for (int i=0; i<fields.length; i++) {
					fields[i] = fields[i].trim().replace("\"", "");
				}
				table.add( fields );
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return table;
	}

	public void process() {
		// read in CSV file
		Vector<String[]> table = readCSV( csvFilePath );
		if (table.size() == 0) {
			IJ.log("Couldn't find any input data in the CSV file");
			return;
		}
		// make sure the output path exists
		File outputDir = new File( outputDirectory );
		if ( !outputDir.exists() ) {
			outputDir.mkdirs();
		} else if ( outputDir.isFile() ) {
			IJ.log( "The output directory path exists and points to a file" );
			return;
		}

		// get the current foreground color
		Color fgColor = Toolbar.getForegroundColor();
		float[] fillcolor = fgColor.getRGBColorComponents( null );
		for (int i=0;i<fillcolor.length;i++)
			fillcolor[i] = fillcolor[i] * 255.0f;

		// further processing depends on the mode
		if (mode == Mode.Interior)
			processCellCenters( table, fillcolor );
	}

	HashMap< Integer, Vector< CellColorInfo > > getCellColoringPoints( Vector<String[]> data ) {
		// parse the string data table in the following format:
		// [<frame>, <x-pos>, <y-pos>]
		Iterator<String[]> itr = data.iterator();
		HashMap< Integer, Vector< CellColorInfo > > points =
			new HashMap< Integer, Vector< CellColorInfo > >();
		while ( itr.hasNext() ) {
			String[] element = itr.next();
			int numElements = element.length;
			if ( numElements < 3 ) {
				IJ.log( "A data element had less than three elements (" + numElements + "), ignoring it." );
				continue;
			}
			// get the data
			Integer frame = Integer.valueOf( element[0] );
			float x = Float.parseFloat( element[1] );
			float y = Float.parseFloat( element[2] );
			if ( !points.containsKey( frame ) )
				points.put( frame, new Vector< CellColorInfo >() );
			points.get( frame ).add( new CellColorInfo( frame, (long)x, (long)y ) );
		}

		return points;
	}

	void processCellCenters( Vector<String[]> data, float[] fillcolor ) {
		FloatType fill[] = new FloatType[3];
		for (int i=0;i<3;i++) {
			fill[i] = new FloatType();
			fill[i].setReal( fillcolor[i] );
		}

		HashMap< Integer, Vector< CellColorInfo > > points =
			getCellColoringPoints( data );

		// iterate over all the frames we found
		Iterator< Integer > frames = points.keySet().iterator();
		while ( frames.hasNext() ) {
			Integer frame = frames.next();

			// open image for this frame
			String path = getImagePath( frame.intValue() );
			ImageInfo info = new ImageInfo( path );
			Img< FloatType > img = loadImage( info.path );
			long width = img.dimension( 0 );
			long height = img.dimension( 1 );

			// make sure we got three positions
			if ( img.numDimensions() != 3 ) {
				IJ.log( "Currently, only images with 3 dimensions are supported (X,Y and C)." );
				return;
			}

			// iterate over all thepoints of that frame
			Iterator< CellColorInfo > itr = points.get( frame ).iterator();
			while ( itr.hasNext() ) {
				CellColorInfo cci = itr.next();
				// go to defined position
				RandomAccess< FloatType > ra = img.randomAccess();
				long[] pos = new long[img.numDimensions()];
				pos[0] = cci.x;
				pos[1] = cci.y;
				// make sure the position is in bounds
				if ( pos[0] < 0 && pos[0] >= img.dimension(0) ||
					pos[1] < 0 && pos[1] >= img.dimension(1)) {
						IJ.log( "Requested position is out of bounds." );
						return;
				}
				ra.setPosition( pos );
				// expect to be in a cell -- flood fill!
				FloatType old = ra.get().copy();
				
				floodLoop( ra, width, height, old, fill );
				info.modified = true;
			}

			// Create an RGB image out of it and save the file back to disk
			Img< ARGBType > result = convertToARGB( img );
			ImagePlus imp = ImageJFunctions.wrap( result, "Result" );
			IJ.saveAs( imp, "png", info.outputPath );
		}
	}

	Img< ARGBType > convertToARGB( Img< FloatType > image ) {
		final Img< ARGBType > result = new ArrayImgFactory< ARGBType >()
	    			.create( new long[] {image.dimension(0), image.dimension(1)},
	    				new ARGBType() );
	    	Cursor< ARGBType > output = result.cursor();
	    	RandomAccess< FloatType > input = image.randomAccess();

	    	final ARGBType tmp = new ARGBType();

	    	while ( output.hasNext() ) {
	    		output.next();
	    		input.setPosition(output.getIntPosition(0), 0);
	    		input.setPosition(output.getIntPosition(1), 1);
	    		input.setPosition(0, 2);
	    		float r = input.get().getRealFloat();
	    		input.setPosition(1, 2);
	    		float g = input.get().getRealFloat();
	    		input.setPosition(2, 2);
	    		float b = input.get().getRealFloat();
	    		output.get().set( output.get().rgba( r, g, b, 1.0f ) );
	    	}

	    	return result;
	}

	String getImagePath( int slice ) {
		String num = Integer.toString( slice );
		int numDigits = num.length();
		int patternLength = pathPattern.length();
		String number = "";
		// prefix zeros, if needed
		while (numDigits < patternLength) {
			number += "0";
			numDigits++;
		}
		// create final image path
		number += num;
		String path = rawPath.replace(
			patternStart + pathPattern + patternEnd,
			number);
		return path;
	}

	Img< FloatType > loadImage( String path ) {
		try {
			Img< FloatType > img = new ImgOpener().openImg( path,
						new ArrayImgFactory< FloatType >(), new FloatType() );
			return img;
		} catch (ImgIOException e) {
			IJ.log("Couldn't open image: " + path);
			return null;
		}
	}

	void showAbout() {
		IJ.showMessage("ColorTool",
			"use CSV files to color an image"
		);
	}

	/**
	 * Recursively fills surrounding pixels of the old color.
	 * Based on code from:
	 * http://www.codecodex.com/wiki/Implementing_the_flood_fill_algorithm
	 */
	private static <S extends RealType< S > > void floodLoop( RandomAccess<S> ra, long width, long height, S old, S[] fill ) {
		// start position is the current position of the RandomAccess
		long x = ra.getLongPosition( 0 );
		long y = ra.getLongPosition( 1 );
		// finds the left side, filling along the way
		long fillL = x;
		ra.setPosition( x, 0);
		do {
			// fill all three channels
			ra.get().set( fill[0] );
			ra.setPosition( 1, 2);
			ra.get().set( fill[1] );
			ra.setPosition( 2, 2);
			ra.get().set( fill[2] );
			ra.setPosition( 0, 2);
			fillL--;
			ra.setPosition( fillL, 0);
		} while ( fillL >= 0 && ra.get().compareTo( old ) == 0 );
		fillL++;

		// find the right right side, filling along the way
		long fillR = x;
		ra.setPosition( x, 0);
		do {
			// fill all three channels
			ra.get().set( fill[0] );
			ra.setPosition( 1, 2);
			ra.get().set( fill[1] );
			ra.setPosition( 2, 2);
			ra.get().set( fill[2] );
			ra.setPosition( 0, 2);
			fillR++;
			ra.setPosition( fillR, 0);
		} while (fillR < width - 1 && ra.get().compareTo( old ) == 0 );
		fillR--;

		// checks if applicable up or down
		for (long i = fillL; i <= fillR; i++) {
			if (y > 0 ) {
				ra.setPosition( i, 0);
				ra.setPosition( y - 1, 1);
				if ( ra.get().compareTo( old ) == 0 )
					floodLoop( ra, width, height, old, fill);
			}
			if (y < height - 1 ) {
				ra.setPosition( i, 0);
				ra.setPosition( y + 1, 1);
				if ( ra.get().compareTo( old ) == 0 )
					floodLoop( ra, width, height, old, fill);
			}
		}
	}
}
