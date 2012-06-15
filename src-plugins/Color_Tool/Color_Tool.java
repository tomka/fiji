import ij.IJ;
import ij.ImagePlus;
import ij.LookUpTable;
import ij.process.ImageProcessor;
import ij.util.StringSorter;

import java.util.ArrayList;
import java.util.Vector;
import java.awt.image.IndexColorModel;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.Img;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;

public class Color_Tool {
	protected static String PREF_KEY = "Color_Tool.";
	protected enum Mode { Interior, Bound };
	protected String rawPath = ""; // "/home/tom/.gvfs/project_raphael auf fileserver/111102_PW_from16h00_EcadGFP-27deg_25deg_SD3_FULL-SEGMENTATION/OptimizedMaxProj_30/Optimized_projection_{nnn}/handCorrection.png";
	protected String pathPattern;
	protected String patternStart = "{";
	protected String patternEnd = "}";
	protected String csvFilePath = ""; // "/home/tom/dev/rapha/Coloring/dividing_cells.csv";
	protected String outputDirectory = ""; // "/home/tom/dev/rapha/Coloring/output";
	protected Mode mode;
	protected String delimiter = ",";
	protected String outputFileType = "png";
	protected ArrayList<FloatType> boundaryMarker = null;
	
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

		public ImageInfo( String path ) {
			this.path = path;
			this.filename = new File( path ).getName();
			this.outputPath= outputDirectory + filename;
			this.modified = false;
		}
	}
	
	protected class CellColorInfo {
		public Integer cellId;
		public Integer frame;
		public Integer colorFrame;
		public long x;
		public long y;

		public CellColorInfo( Integer f, long x, long y, Integer cellId ) {
			this( f, f, x, y, cellId );
		}

		public CellColorInfo( Integer f, Integer cf, long x, long y, Integer cellId ) {
			this.frame = f;
			this.colorFrame = cf;
			this.x = x;
			this.y = y;
			this.cellId = cellId;
		}
	}

	public enum LUTType {
		Ice,
		Quarters,
		File
	}

	public class LUTInfo {
		public String name;
		public String path;
		public LUTType type;

		public LUTInfo( String name, String path ) {
			this.name = name;
			this.path = path;
			this.type = LUTType.File;
		}

		public LUTInfo( String name, LUTType type ) {
			this.name = name;
			this.path = "";
			this.type = type;
		}
	}

	public Color_Tool() {
		// Set up boundary marker -- use negative infinity
		// Don't use NaN, as it causes a severe performance drop
		this.boundaryMarker = new ArrayList<FloatType>(3);
		for (int c=0;c<3;++c) {
			this.boundaryMarker.add( new FloatType( Float.NEGATIVE_INFINITY ) );
		}
	}
	
	public ArrayList<LUTInfo> getLUTs(String path) {
		ArrayList<LUTInfo> luts = new ArrayList<LUTInfo>();
		// add plugin built-in LUTs
		luts.add( new LUTInfo( "Ice (built-in)", LUTType.Ice ) );
		luts.add( new LUTInfo( "Quarters (built-in)", LUTType.Quarters ) );
		// add Fiji build-in LUTs
		luts.add( new LUTInfo( "Fire", "Fire" ) );
		luts.add( new LUTInfo( "Grays", "Grays" ) );
		luts.add( new LUTInfo( "Ice", "Ice" ) );
		luts.add( new LUTInfo( "Spectrum", "Spectrum" ) );
		luts.add( new LUTInfo( "3-3-2 RGB", "3-3-2 RGB" ) );
		luts.add( new LUTInfo( "Red", "Red" ) );
		luts.add( new LUTInfo( "Green", "Green" ) );
		luts.add( new LUTInfo( "Blue", "Blue" ) );
		luts.add( new LUTInfo( "Cyan", "Cyan" ) );
		luts.add( new LUTInfo( "Magenta", "Magenta" ) );
		luts.add( new LUTInfo( "Yellow", "Yellow" ) );
		luts.add( new LUTInfo( "Red/Green", "Red/Green" ) );
	
		// add file LUTs
		if (path==null)
			return luts;
		File f = new File(path);
		String[] list = null;
		if (f.exists() && f.isDirectory())
			list = f.list();
		if (list==null)
			return luts;
		if (IJ.isLinux())
			StringSorter.sort(list);
		
		for (int i=0; i<list.length; i++) {
			String name = list[i];
			if (name.endsWith(".lut")) {
				String lutName = name.substring(0,name.length()-4);
				luts.add( new LUTInfo( lutName, f.getPath() + "/" + name ) );
			}
		}
		return luts;
	}
	
	public FloatType[][] loadLUT( LUTInfo li ) {
		int[] r,g,b;
		if (li.type == LUTType.Ice) {
			int[] _r = {0,0,0,0,0,0,19,29,50,48,79,112,134,158,186,201,217,229,242,250,250,250,250,251,250,250,250,250,251,251,243,230};
			int[] _g = {156,165,176,184,190,196,193,184,171,162,146,125,107,93,81,87,92,97,95,93,93,90,85,69,64,54,47,35,19,0,4,0};
			int[] _b = {140,147,158,166,170,176,209,220,234,225,236,246,250,251,250,250,245,230,230,222,202,180,163,142,123,114,106,94,84,64,26,27};
			r = _r; g = _g; b = _b;
		} else if (li.type == LUTType.Quarters) {
			int[] _r = {0,0,255,0};
			int[] _g = {0,255,255,0};
			int[] _b = {255,0,0,255};
			r = _r; g = _g; b = _b;
		} else {
			// read in the LUT
			ImagePlus imp = (ImagePlus)IJ.runPlugIn("ij.plugin.LutLoader", li.path);
			ImageProcessor ip = imp.getChannelProcessor();
			if (ip == null) {
				IJ.log("Couldn't load look up table: " + li.path);
				return null;
			}
			// get color model and LUT colors
			IndexColorModel cm = (IndexColorModel)ip.getColorModel();
			LookUpTable lut = new LookUpTable(cm);
			int mapSize = lut.getMapSize();
			byte[] _rB = lut.getReds();
			byte[] _gB = lut.getGreens();
			byte[] _bB = lut.getBlues();
			int[] _r = new int[mapSize];
			int[] _g = new int[mapSize];
			int[] _b = new int[mapSize];
			// convert to internal integer representation
			for (int i=0; i<mapSize; ++i) {
				_r[i] = _rB[i]&255;
				_g[i] = _gB[i]&255;
				_b[i] = _bB[i]&255;
			}
			r = _r; g = _g; b = _b;
		}

		int entries = r.length;
		FloatType[][] lut = new FloatType[ entries ][ 3 ];
		for (int i=0;i<entries;++i) {
			lut[i][0] = new FloatType( r[i] );
			lut[i][1] = new FloatType( g[i] );
			lut[i][2] = new FloatType( b[i] );
		}

		return lut;
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
	
	protected Img< ARGBType > convertToARGB( Img< FloatType > image ) {
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
	
	protected String getImagePath( int slice ) {
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

	/**
	 * Save the image to the specified output path. It is converted to
	 * an RGB image and saved as a PNG file.
	 */
	protected void saveImage( Img< FloatType > img, String outputPath ) {
		Img< ARGBType > result = convertToARGB( img );
		ImagePlus imp = ImageJFunctions.wrap( result, "Result" );
		IJ.saveAs( imp, "png", outputPath );
	}

	protected Img< FloatType > loadImage( String path ) {
		try {
			// load the image
			ArrayImgFactory< FloatType > imgFactory = new ArrayImgFactory< FloatType >();
			Img< FloatType > img = new ImgOpener().openImg( path,
						imgFactory, new FloatType() );
			// make sure we have three dimensions (X, Y, C)
			if (img.numDimensions() != 3) {
				IJ.log( "Currently, only images with 3 dimensions are supported (X,Y and C)." );
				return null;
			}
			return img;
		} catch (ImgIOException e) {
			IJ.log("Couldn't open image: " + path);
			return null;
		}
	}

	/**
	 * Replace all the occurences of one value with another one.
	 */
	protected <S extends RealType< S > > void replaceValue( Img< S > img, ArrayList<S> find, ArrayList<S> replace ) {
		long width = img.dimension( 0 );
		long height = img.dimension( 1 );
		long[] pos = new long[3];

		RandomAccess< S > ra = img.randomAccess();

		for (int x=0; x<width;++x) {
			for (int y=0; y<height; ++y) {
				pos[0] = x;
				pos[1] = y;
				// check if the current value is the one we look for
				boolean found = true;
				for (int c=0; c<3; ++c) {
					pos[2] = c;
					ra.setPosition(pos);
					found &= find.get(c).compareTo( ra.get() ) == 0;
				}
				// replace the value if found
				if (found) {
					for (int c=0; c<3; ++c) {
						ra.setPosition(c, 2);
						ra.get().set( replace.get(c).copy() );
					}
				}
			}
		}
	}

	/**
	 * Finds the value of which mark the bounding structures.
	 */
	protected static <S extends RealType< S > > ArrayList<S> findStructureValues( RandomAccess<S> ra ) {
		// get starting position
		long[] pos = new long[3];
		ra.localize( pos );

		// Walk throug the channels to get the interior values.
		ArrayList<S> interior = new ArrayList<S>(3);
		for (int c=0;c<3;++c) {
			ra.setPosition(c, 2);
			interior.add( ra.get().copy() );
		}

		/* Walk to the left until we see something different.
		 * If we reach zero, walk to the right.
		 */
		boolean searching = true;
		boolean walkLeft = true;
		long x = ra.getLongPosition( 0 );
		while (searching) {
			// walk to the left/right
			if (walkLeft) {
				x--;
			} else {
				x++;
			}
			// flip direction if below zero
			if (x < 0) {
				walkLeft = false;
				x++;
			}
			// set position
			ra.setPosition(x, 0);

			// check if the current value is different from the start
			boolean same = true;
			for (int c=0;c<3;++c) {
				ra.setPosition(c, 2);
				same &= interior.get(c).compareTo( ra.get() ) == 0;
			}
			searching = same;
		}
		// assume we found what we were looking for
		ArrayList<S> boundary = new ArrayList<S>(3);
		for (int c=0;c<3;++c) {
			ra.setPosition(c, 2);
			boundary.add( ra.get().copy() );
		}

		// reset to original position and return
		ra.setPosition( pos );
		return boundary;
	}

	/**
	 * Recursively fills surrounding pixels of the old color. It operates
	 * only in 2D. Based on code from:
	 * http://www.codecodex.com/wiki/Implementing_the_flood_fill_algorithm
	 */
	protected static <S extends RealType< S > > void floodLoop( RandomAccess<S> ra, long width, long height, S old, S fill ) {
		// return if old and new color are the same
		if (old.compareTo(fill) == 0)
			return;
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

	void log( String msg ) {
		System.out.println( msg );
	}
}
