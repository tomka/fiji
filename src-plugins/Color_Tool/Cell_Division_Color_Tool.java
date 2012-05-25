import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.WindowManager;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import fiji.util.gui.GenericDialogPlus;
import ij.util.StringSorter;
import ij.plugin.LutLoader;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.awt.image.IndexColorModel;

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

public class Cell_Devision_Color_Tool< T extends RealType< T > > implements PlugIn {
	protected enum Mode { Interior, Bound };
	protected String rawPath = "/home/tom/.gvfs/project_raphael auf fileserver/111102_PW_from16h00_EcadGFP-27deg_25deg_SD3_FULL-SEGMENTATION/OptimizedMaxProj_30/Optimized_projection_{nnn}/handCorrection.png";
	protected String pathPattern;
	protected final String patternStart = "{";
	protected final String patternEnd = "}";
	protected String csvFilePath = "/home/tom/dev/rapha/Coloring/dividing_cells_small.csv";
	protected String outputDirectory = "/home/tom/dev/rapha/Coloring/output";
	protected Mode mode;
	protected String delimiter = ",";
	protected String outputFileType = "png";
	protected FloatType[][] lut;

	protected class ImageInfo {
		public String path;
		public String filename;
		public String outputPath;
		public boolean modified;

		public ImageInfo( String path, Integer frame ) {
			this.path = path;
			this.filename = new File( path ).getName();
			String filenameNoExt = filename.substring(0, filename.lastIndexOf('.'));
			this.outputPath= outputDirectory + filenameNoExt + "_" + frame.toString() + "." + outputFileType;
			this.modified = false;
		}
	}

	protected class CellColorInfo {
		public Integer frame;
		public Integer colorFrame;
		public long x;
		public long y;

		public CellColorInfo( Integer f, long x, long y ) {
			this( f, f, x, y );
		}

		public CellColorInfo( Integer f, Integer cf, long x, long y ) {
			this.frame = f;
			this.colorFrame = cf;
			this.x = x;
			this.y = y;
		}
	}

	public class LUTInfo {
		public String name;
		public String path;

		public LUTInfo( String name, String path ) {
			this.name = name;
			this.path = path;
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

	public ArrayList<LUTInfo> getLUTs(String path) {
		if (path==null) return null;
		File f = new File(path);
		String[] list = null;
		if (f.exists() && f.isDirectory())
			list = f.list();
		if (list==null) return null;
		if (IJ.isLinux())
			StringSorter.sort(list);
		ArrayList<LUTInfo> luts = new ArrayList<LUTInfo>();
		for (int i=0; i<list.length; i++) {
			String name = list[i];
			if (name.endsWith(".lut")) {
				String lutName = name.substring(0,name.length()-4);
				luts.add( new LUTInfo( lutName, f.getPath() + "/" + name ) );
			}
		}
		return luts;
	}

	private boolean showDialog() {
		// read in available look up tables
		String lutDir = IJ.getDirectory("luts");
		ArrayList<LUTInfo> lutInfos= getLUTs( lutDir );
		String[] luts = new String[ lutInfos.size() ];
		for (int i=0; i<lutInfos.size(); ++i) {
			luts[i] = lutInfos.get(i).name;
		}

		// create a dialog
		GenericDialogPlus gd = new GenericDialogPlus("Color Tool");

		gd.addFileField( "Image file pattern -- {nn}", rawPath );
		gd.addDirectoryField( "Output directory", outputDirectory );
		gd.addFileField( "CSV file", csvFilePath );
		gd.addStringField("Delimiter", delimiter);
		gd.addChoice("LUT", luts, luts[0]);

		gd.showDialog();
		if ( gd.wasCanceled() )
			return false;

		// get entered values
		rawPath = gd.getNextString();
		outputDirectory = gd.getNextString();
		csvFilePath = gd.getNextString();
		delimiter = gd.getNextString();
		int lutIndex =gd.getNextChoiceIndex();
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

		// load the look up table
		this.lut = loadLUT( lutInfos.get( lutIndex ) );
		if (lut == null) {
			IJ.log( "Couldn't load LUT." );
			return false;
		} else {
			return true;
		}
	}

	public FloatType[][] loadLUT( LUTInfo li ) {
		// for now create an ice LUT
		int[] r = {0,0,0,0,0,0,19,29,50,48,79,112,134,158,186,201,217,229,242,250,250,250,250,251,250,250,250,250,251,251,243,230};
		int[] g = {156,165,176,184,190,196,193,184,171,162,146,125,107,93,81,87,92,97,95,93,93,90,85,69,64,54,47,35,19,0,4,0};
		int[] b = {140,147,158,166,170,176,209,220,234,225,236,246,250,251,250,250,245,230,230,222,202,180,163,142,123,114,106,94,84,64,26,27};

		int entries = r.length;
		FloatType[][] lut = new FloatType[ entries ][ 3 ];
		for (int i=0;i<entries;++i) {
			lut[i][0] = new FloatType( r[i] );
			lut[i][1] = new FloatType( g[i] );
			lut[i][2] = new FloatType( b[i] );
		}

		return lut;

		// read in the LUT
		//ImagePlus imp = (ImagePlus)IJ.runPlugIn("ij.plugin.LutLoader", li.path);
		//imp.show();

		//ImageProcessor ip = imp.getProcessor();
		//if (imp == null || ip == null) {
		//	IJ.log("Couldn't load look up table: " + li.path);
		//	return null;
		//}
		// store the values in a FloatType array

		//int width = ip.getWidth();
		//int height = ip.getHeight();

		//int[] rgb = new int[3];
		//for (int x=0; x<width; x++) {
		//	ip.getPixel(x, 0, rgb);
		//	System.out.println( rgb[0] + " " + rgb[1] + " " + rgb[2] );
		//}
		//return null;
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
		IJ.showStatus( "Reading CSV file" );
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
			processCellCenters( table );

		IJ.showStatus( "All images have been processed.");
	}

	HashMap< Integer, Vector< CellColorInfo > > getCellColoringPoints( Vector<String[]> data ) {
		IJ.showStatus( "Sorting input data per frame" );
		// parse the string data table in the following format:
		// [<frame>, <x-pos>, <y-pos>]
		Iterator<String[]> itr = data.iterator();
		// a hash map to keep track of the cells that divided
		HashMap< Integer, Integer > cells =
			new HashMap< Integer, Integer >();
		// create color frame information
		while ( itr.hasNext() ) {
			String[] element = itr.next();
			int numElements = element.length;
			if ( numElements < 7 ) {
				IJ.log( "A data element had less than seven elements (" + numElements + "), ignoring it." );
				continue;
			}
			try {
				// get the data
				Integer frame = Integer.valueOf( element[0] );
				Integer parent_id = Integer.valueOf( element[1] );
				Integer child_id = Integer.valueOf( element[4] );

				/* check if know the child already or if we have
				 *  found an earlier time point.
				 */
				if ( !cells.containsKey( parent_id ) ) {
					cells.put( parent_id, frame );
				} else {
					Integer colorFrame = cells.get(parent_id);
					if (frame < colorFrame)
						cells.put( parent_id, frame );
				}
				/* check if know the child already or if we have
				 *  found an earlier time point.
				 */
				if ( !cells.containsKey( child_id ) ) {
					cells.put( child_id, frame );
				} else {
					Integer colorFrame = cells.get(child_id);
					if (frame < colorFrame)
						cells.put( child_id, frame );
				}
			}
			catch (NumberFormatException e) {
				// ignore the line if we can't read it
			}
		}
		// create color infos
		IJ.showStatus( "Creating data structures" );
		// a hash map to keep coloring information for each frame
		HashMap< Integer, Vector< CellColorInfo > > frames =
			new HashMap< Integer, Vector< CellColorInfo > >();
		itr = data.iterator();
		while ( itr.hasNext() ) {
			String[] element = itr.next();
			int numElements = element.length;
			if ( numElements < 7 ) {
				IJ.log( "A data element had less than seven elements (" + numElements + "), ignoring it." );
				continue;
			}
			try {
				// get the data
				Integer frame = Integer.valueOf( element[0] );
				Integer parent_id = Integer.valueOf( element[1] );
				float parent_x = Float.parseFloat( element[2] );
				float parent_y = Float.parseFloat( element[3] );
				Integer child_id = Integer.valueOf( element[4] );
				float child_x = Float.parseFloat( element[5] );
				float child_y = Float.parseFloat( element[6] );

				// make sure we have a list of cells for the current frame
				if ( !frames.containsKey( frame ) ) {
					frames.put( frame, new Vector< CellColorInfo >() );
				}

				// add parent and child cell info to current frame
				CellColorInfo child = new CellColorInfo( frame,
					cells.get(child_id), (long)child_x, (long)child_y );
				CellColorInfo parent = new CellColorInfo( frame,
					cells.get(parent_id), (long)parent_x, (long)parent_y );
				frames.get( frame ).add( child );
				frames.get( frame ).add( parent );
			}
			catch (NumberFormatException e) {
				// ignore the line if we can't read it
			}
		}

		return frames;
	}

	FloatType[] getFillColor( int frame, int maxFrames ) {
		float scale = (float)frame / (float)maxFrames;
		int idx = (int)( (this.lut.length - 1) * scale );

		return this.lut[idx];
	}

	void processCellCenters( Vector<String[]> data ) {
		HashMap< Integer, Vector< CellColorInfo > > points =
			getCellColoringPoints( data );

		// iterate over all the frames we found
		Iterator< Integer > frames = points.keySet().iterator();
		int numImages = points.size();
		int imageCounter = 0;
		while ( frames.hasNext() ) {
			imageCounter++;
			Integer frame = frames.next();
			IJ.showStatus( "Processing file " + imageCounter + "/" + numImages);
			IJ.showProgress( imageCounter, numImages );

			// open image for this frame
			String path = getImagePath( frame.intValue() );
			ImageInfo info = new ImageInfo( path, frame );
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

				// get fill color
				FloatType[] fill = getFillColor(
					cci.colorFrame.intValue(), numImages );

				// iterate over every channel
				for (int d=0; d<3; d++) {
					// set channel
					pos[2] = d;
					ra.setPosition( pos );
					// expect to be in a cell -- flood fill!
					FloatType old = ra.get().copy();
					// check if we need to fill at all
					if ( old.compareTo( fill[d] ) != 0 ) {
						// flood fill exery channel with the approptiate color
						floodLoop( ra, width, height, old, fill[d] );
					}
				}
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
	 * Recursively fills surrounding pixels of the old color. It operates
	 * only in 2D. Based on code from:
	 * http://www.codecodex.com/wiki/Implementing_the_flood_fill_algorithm
	 */
	private static <S extends RealType< S > > void floodLoop( RandomAccess<S> ra, long width, long height, S old, S fill ) {
		// start position is the current position of the RandomAccess
		long x = ra.getLongPosition( 0 );
		long y = ra.getLongPosition( 1 );
		// finds the left side, filling along the way
		long fillL = x;
		ra.setPosition( x, 0);
		do {
			// fill the current channel
			ra.get().set( fill );
			// move to the left
			fillL--;
			if (fillL < 0)
				break;
			ra.setPosition( fillL, 0 );
		} while ( ra.get().compareTo( old ) == 0 );
		fillL++;

		// find the right right side, filling along the way
		long fillR = x;
		ra.setPosition( x, 0);
		do {
			// fill the current channel
			ra.get().set( fill );
			// move to the right
			fillR++;
			if (fillR >= width)
				break;
			ra.setPosition( fillR, 0);
		} while ( ra.get().compareTo( old ) == 0 );
		fillR--;

		// checks if applicable up or down
		for (long i = fillL; i <= fillR; i++) {
			if ( y > 0 ) {
				ra.setPosition( i, 0);
				ra.setPosition( y - 1, 1);
				if ( ra.get().compareTo( old ) == 0 )
					floodLoop( ra, width, height, old, fill );
			}
			if ( y < height - 1 ) {
				ra.setPosition( i, 0);
				ra.setPosition( y + 1, 1);
				if ( ra.get().compareTo( old ) == 0 )
					floodLoop( ra, width, height, old, fill );
			}
		}
	}
}
