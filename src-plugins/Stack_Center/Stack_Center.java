import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

/**
 * 1. Load single slice TIFFs as stack
 * 2. Identify first and last stlice with signal (maybe by
 * splitting the image in small sub-tiles and look for average
 * intensities in there. If at least one tile has a high average
 * consider the slice as valid. Alternatively, look at the a gradient
 * image, like Sobel, to test if there are structures in the sub-tile.
 * Another option would be the power spectrum / FFT. Option four: variances
 * in sub-tiles should be high.) If there is more than one continues
 * start-end slice interval, choose the longest one.
 * 3. 
 */
public class Stack_Center< T extends RealType< T > > implements PlugIn {
	protected double threshold;
	protected int startSlice;
	protected int endSlice;

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
		GenericDialog gd = new GenericDialog("Stack center");

		gd.addNumericField("Threshold", 0.00, 2);
		
		//gd.addStringField("name", "John");

		gd.showDialog();
		if ( gd.wasCanceled() )
			return false;

		// get entered values
		threshold = gd.getNextNumber();
		//name = gd.getNextString();

		return true;
	}

	public void process(ImagePlus imp) {
		// wrap it into an ImgLib image (no copying)
		final Img< T > input = ImagePlusAdapter.wrap( imp );
		final int numDimensions = input.numDimensions();
		long width = input.dimension( 0 );
		long height = input.dimension( 1 );

		// create the ImgFactory that will instantiate the output image
        final ImgFactory< IntType > imgFactory = new ArrayImgFactory< IntType >();
        // create an output image for the height map
        final Img< IntType > output = imgFactory.create( new long[] { width, height }, new IntType() );
		
		// iterate over the input
		Cursor< T > cursorInput = input.cursor();
		RandomAccess< IntType > randomAccessOutput = output.randomAccess();

		// init output with -1 to mark unvisited positions
		Cursor< T > cursorOutput = output.cursor();
		while ( cursorOutput.hasNext() )
		{
			cursorOutput.fwd();
			cursorOutput.get().set( -1);
		}

		/**
		 * ToDo:
		 *  - only look within start-end slices.
		 *  
		 */
 
		long[] inPos = new long[ numDimensions ];
		long[] outPos = new long[ 2 ];
        while ( cursorInput.hasNext() )
        {
            // move both cursors forward by one pixel
            cursorInput.fwd();

            // check if the current value is above or equal the threshold
            if ( cursorInput.get().getRealDouble() < threshold )
            	continue;

			// set output random access to projected 2D position
			cursorInput.localize( inPos );
			outPos[0] = inPos[0];
			outPos[1] = inPos[1];
			randomAccessOutput.setPosition( outPos );

			// check if there is already a value at this position
			RealType< UnsignedIntType > curVal = randomAccessOutput.get();
			if (curVal.get() == -1 )
			{
				
			}

            // set the value of this pixel of the output image to the same as the input,
            // every Type supports T.set( T type )
            //randomAccessOutput.get().set( cursorInput.get() );
        }
	}

	void showAbout() {
		IJ.showMessage("StackCenter",
			"find the center of a stack"
		);
	}
}
