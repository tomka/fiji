import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

public class Color_Tool< T extends RealType< T > > implements PlugIn {
	protected enum Mode { Interior, Bound };
	protected String csvFilePath;
	protected Mode mode;

	public void run(String arg) {
       if ( arg.equals( "about" ) ) {
			showAbout();
			return;
		}

		// get the current image
		ImagePlus image = WindowManager.getCurrentImage();
		if ( showDialog() )
			process( image );
    }

	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Color Tool");

		gd.addStringField("CSV file", "");

		gd.showDialog();
		if ( gd.wasCanceled() )
			return false;

		// get entered values
		csvFilePath = gd.getNextString();
		mode = Mode.Interior;

		return true;
	}

	public Vector<String[]> readCSV( String path ) {
		Vector<String[]> table = new Vector<String[]>();
		String delim = ";";
		try {
			BufferedReader in = new BufferedReader( new FileReader( new File( path ) ) );
			String readString;
			while ((readString = in.readLine()) != null) {
				stringVektor.add( readString.split( delim ) );
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return table;
	}

	public void process(ImagePlus imp) {
		// read in CSV file
		Vector<String[]> table = readCSV( csvFilePath );
		if (table.length() == 0)
			IJ.log("Couldn't find any input data in the CSV file");
			return;
		// wrap it into an ImgLib image (no copying)
		final Img< T > input = ImagePlusAdapter.wrap( imp );
		// further processing depends on the mode
		if (mode == Mode.Interior)
			processCellCenters( input, table );
	}

	void processCellCenters( image, stringData ) {
		// parse the string data table in the following format:
		// [<frame>, <x-pos>, <y-pos>]
	}

	void showAbout() {
		IJ.showMessage("ColorTool",
			"use CSV files to color an image"
		);
	}
}
