import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import fiji.util.gui.GenericDialogPlus;

import java.awt.Color;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgSaver;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;

public class Basic_Cell_Color_Tool< T extends RealType< T > >
extends Color_Tool implements PlugIn
{
	public void run(String arg) {
			if ( arg.equals( "about" ) ) {
			showAbout();
			return;
		}

		if ( showDialog() )
			process();
	}

	private boolean showDialog() {
		// set up the users preferences
		rawPath = Prefs.get(PREF_KEY+"rawPath", "");
		csvFilePath = Prefs.get(PREF_KEY+"csvFilePath", "");
		outputDirectory = Prefs.get(PREF_KEY+"outputDirectory", "");
		delimiter = Prefs.get(PREF_KEY+"delimiter", ",");
		patternStart = Prefs.get(PREF_KEY+"patternStart", "{");
		patternEnd = Prefs.get(PREF_KEY+"patternEnd", "}");
		outputFileType = Prefs.get(PREF_KEY+"outputFileType", "png");

		GenericDialogPlus gd = new GenericDialogPlus("Color Tool");

		gd.addFileField( "Image file pattern -- {nn}", rawPath );
		gd.addDirectoryField( "Output directory", outputDirectory );
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

		// save user preferences
		Prefs.set(PREF_KEY+"rawPath", rawPath);
		Prefs.set(PREF_KEY+"csvFilePath", csvFilePath);
		Prefs.set(PREF_KEY+"outputDirectory", outputDirectory);
		Prefs.set(PREF_KEY+"delimiter", delimiter);
		Prefs.set(PREF_KEY+"patternStart", patternStart);
		Prefs.set(PREF_KEY+"patternEnd", patternEnd);
		Prefs.set(PREF_KEY+"outputFileType", outputFileType);

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
			processCellCenters( table, fillcolor );

		IJ.showStatus( "All images have been processed." );
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
		int numImages = points.size();
		int imageCounter = 0;
		while ( frames.hasNext() ) {
			imageCounter++;
			Integer frame = frames.next();
			IJ.showStatus( "Processing file " + imageCounter + "/" + numImages);
			IJ.showProgress( imageCounter, numImages );

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

	void showAbout() {
		IJ.showMessage("ColorTool",
			"use CSV files to color an image"
		);
	}
}
