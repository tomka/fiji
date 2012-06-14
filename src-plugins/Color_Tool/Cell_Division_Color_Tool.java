import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.process.ImageProcessor;
import ij.WindowManager;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import fiji.util.gui.GenericDialogPlus;
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
import java.lang.StackOverflowError;

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

public class Cell_Division_Color_Tool< T extends RealType< T > >
extends Color_Tool implements PlugIn {
	protected FloatType[][] lut;

	public void run(String arg) {
			if ( arg.equals( "about" ) ) {
			showAbout();
			return;
		}

		if ( showDialog() )
			process();
	}

	private boolean showDialog() {
		// read in available look up tables
		String lutDir = IJ.getDirectory("luts");
		ArrayList<LUTInfo> lutInfos= getLUTs( lutDir );
		String[] luts = new String[ lutInfos.size() ];
		for (int i=0; i<lutInfos.size(); ++i) {
			luts[i] = lutInfos.get(i).name;
		}

		// set up the users preferences
		rawPath = Prefs.get(PREF_KEY+"rawPath", "");
		csvFilePath = Prefs.get(PREF_KEY+"csvFilePath", "");
		outputDirectory = Prefs.get(PREF_KEY+"outputDirectory", "");
		delimiter = Prefs.get(PREF_KEY+"delimiter", ",");
		patternStart = Prefs.get(PREF_KEY+"patternStart", "{");
		patternEnd = Prefs.get(PREF_KEY+"patternEnd", "}");
		outputFileType = Prefs.get(PREF_KEY+"outputFileType", "png");
		int lutIndex = (int) Prefs.get(PREF_KEY+"lutIndex", 0.0);

		// correct LUT index if needed
		if (lutIndex >= luts.length) {
			lutIndex = 0;
		}

		// create a dialog
		GenericDialogPlus gd = new GenericDialogPlus("Color Tool");

		gd.addFileField( "Image file pattern -- {nn}", rawPath );
		gd.addDirectoryField( "Output directory", outputDirectory );
		gd.addMessage( "CSV Data: Frame, ParentID, ParentX, ParentY, ChildID, ChildX, ChildY" );
		gd.addFileField( "CSV file", csvFilePath );
		gd.addStringField("Delimiter", delimiter);
		gd.addChoice("LUT", luts, luts[lutIndex]);

		gd.showDialog();
		if ( gd.wasCanceled() )
			return false;

		// get entered values
		rawPath = gd.getNextString();
		outputDirectory = gd.getNextString();
		csvFilePath = gd.getNextString();
		delimiter = gd.getNextString();
		lutIndex = gd.getNextChoiceIndex();
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
		Prefs.set(PREF_KEY+"lutIndex", lutIndex);

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
					cells.get(child_id), (long)child_x, (long)child_y, child_id );
				CellColorInfo parent = new CellColorInfo( frame,
					cells.get(parent_id), (long)parent_x, (long)parent_y, parent_id );
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
			if (img == null) {
				continue;
			}
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
					try {
						// flood fill exery channel with the approptiate color
						floodLoop( ra, width, height, old, fill[d] );
					} catch (StackOverflowError e) {
						IJ.log( "Encountered recursion limit for position " + Arrays.toString(pos) + " in frame " + frame);
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
