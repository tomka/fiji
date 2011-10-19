import ij.IJ;
import ij.ImagePlus;

import ij.gui.GenericDialog;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

/** ProcessPixels
  *
  * A template for processing each pixel of either
  * GRAY8, GRAY16, GRAY32 or COLOR_RGB images.
  */
public class Gaussian_Dot2 implements PlugInFilter {
	protected ImagePlus image;

	// image property members
	private int width;
	private int height;

	// plugin parameters
	public double fwhm;
	public String name;

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		image = imp;
		return DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		// get width and height
		width = ip.getWidth();
		height = ip.getHeight();

		if (showDialog()) {
			process(ip);
			image.updateAndDraw();
		}
	}

	private boolean showDialog() {
		System.out.println("Start");
		GenericDialog gd = new GenericDialog("Process pixels");

		// default value is 0.00, 2 digits right of the decimal point
		gd.addNumericField("FWHM", 0.00, 2);
		gd.addStringField("name", "John");

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		fwhm = gd.getNextNumber();
		name = gd.getNextString();

		return true;
	}

	public void process(ImagePlus ip) {
		System.out.println("Process");
		// slice numbers start with 1 for historical reasons
		for (int i = 1; i <= image.getStackSize(); i++)
			process(image.getStack().getProcessor(i));
	}

	// Select processing method depending on image type
	public void process(ImageProcessor ip) {
		int type = image.getType();
		if (type == ImagePlus.GRAY8)
			process( (byte[]) ip.getPixels() );
		else if (type == ImagePlus.GRAY16)
			process( (short[]) ip.getPixels() );
		else if (type == ImagePlus.GRAY32)
			process( (float[]) ip.getPixels() );
		else if (type == ImagePlus.COLOR_RGB)
			process( (int[]) ip.getPixels() );
		else {
			throw new RuntimeException("not supported");
		}
	}

	public double getValue(int x, int y) {
		double sigma = fwhm / 2.35482;
		x -= (width - 1) / 2.0f;
		y -= (height - 1) / 2.0f;
		double dist = Math.sqrt(x*x + y*y);
		double val = (1 / (sigma * Math.sqrt(2*Math.PI * sigma * sigma))) *
			Math.exp( ( -(dist*dist) / (2*sigma*sigma) ) );
		return val;
	}

	// processing of GRAY8 images
	public void process(byte[] pixels) {
		double scale = 254 / getValue((int) (width * 0.5),
			(int) (height * 0.5));
		System.out.println("Scale: " + scale);
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				double val = Math.min(255.0, scale * getValue(x,y));
				if (val < 7.0) val = 0.0;
				pixels[x + y * width] += (byte) val;
			}
		}
	}

	// processing of GRAY16 images
	public void process(short[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				pixels[x + y * width] += (short)fwhm;
			}
		}
	}

	// processing of GRAY32 images
	public void process(float[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				pixels[x + y * width] += (float)fwhm;
			}
		}
	}

	// processing of COLOR_RGB images
	public void process(int[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				pixels[x + y * width] += (int)fwhm;
			}
		}
	}

	void showAbout() {
		IJ.showMessage("ProcessPixels",
			"a template for processing each pixel of an image"
		);
	}
}
