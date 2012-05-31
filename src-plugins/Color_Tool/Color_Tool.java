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
	
	public ArrayList<LUTInfo> getLUTs(String path) {
		ArrayList<LUTInfo> luts = new ArrayList<LUTInfo>();
		// add built-in LUTs
		luts.add( new LUTInfo( "Ice (built-in)", LUTType.Ice ) );
		luts.add( new LUTInfo( "Quarters (built-in)", LUTType.Quarters ) );
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

	protected Img< FloatType > loadImage( String path ) {
		try {
			Img< FloatType > img = new ImgOpener().openImg( path,
						new ArrayImgFactory< FloatType >(), new FloatType() );
			return img;
		} catch (ImgIOException e) {
			IJ.log("Couldn't open image: " + path);
			return null;
		}
	}
	
	/**
	 * Recursively fills surrounding pixels of the old color. It operates
	 * only in 2D. Based on code from:
	 * http://www.codecodex.com/wiki/Implementing_the_flood_fill_algorithm
	 */
	protected static <S extends RealType< S > > void floodLoop( RandomAccess<S> ra, long width, long height, S old, S fill ) {
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
