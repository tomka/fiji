package tests;

import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.InputStream;

import mpicbg.imglib.algorithm.gauss.GaussianConvolution3;
import mpicbg.imglib.algorithm.math.function.Converter;
import mpicbg.imglib.algorithm.math.function.RealTypeConverter;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * A class containing some testing helper methods. It allows
 * to open Tiffs from within the Jar file and can generate noise
 * images.
 * 
 * @author Dan White & Tom Kazimiers
 */
public class TestImageAccessor {
	/* a static opener for opening images without the
	 * need for creating every time a new opener
	 */
	static Opener opener = new Opener();
	
	/**
	 * Loads a Tiff file from within the jar. The given path is treated
	 * as relative to this tests-package (i.e. "Data/test.tiff" refers
	 * to the test.tiff in sub-folder Data).
	 * 
	 * @param <T> The wanted output type.
	 * @param relPath The relative path to the Tiff file.
	 * @return The file as ImgLib image.
	 */
	static <T extends RealType<T>> Image<T> loadTiffFromJar(String relPath) {
		InputStream is = TestImageAccessor.class.getResourceAsStream(relPath);
		BufferedInputStream bis = new BufferedInputStream(is);
		
		ImagePlus imp = opener.openTiff(bis, "The Test Image");
		return ImagePlusAdapter.wrap(imp);
	}
	
	/**
	 * Creates a noisy image that is created by repeatedly adding points
	 * with random intensity to the canvas. That way it tries to mimic the
	 * way a microscope produces images. This convenience method uses the
	 * default values of a point size of 3.0 and produces 5000 points.
	 * After the creation the image is smoothed with a sigma of one in each
	 * direction.
	 *  
	 * @param <T> The wanted output type.
	 * @param width The image width.
	 * @param height The image height.
	 * @return The noise image.
	 */
	static <T extends RealType<T>> Image<T> produceNoiseImage(T type, int width, int height) {
		return produceNoiseImage(type, width, height, 3.0f, 5000, new double[] {1.0,1.0});
	}
	
	/**
	 * Creates a noisy image that is created by repeatedly adding points
	 * with random intensity to the canvas. That way it tries to mimic the
	 * way a microscope produces images.
	 * 
	 * @param <T> The wanted output type.
	 * @param width The image width.
	 * @param height The image height.
	 * @param dotSize The size of the dots.
	 * @param numDots The number of dots.
	 * @param smoothingSigma The two dimensional sigma for smoothing.
	 * @return The noise image.
	 */
	static <T extends RealType<T>> Image<T> produceNoiseImage(T type, int width,
			int height, float dotSize, int numDots, double[] smoothingSigma) {
		// create the new image
		ImageFactory<T> imgFactory = new ImageFactory<T>(type, new ArrayContainerFactory());
		Image<T> noiseImage = imgFactory.createImage( new int[] {width, height}, "Noise image");
		
		/* for now (probably until ImageJ2 is out) we must convert
		 * the ImgLib image to an Image one to draw circles on it.
		 */
		ImagePlus img = ImageJFunctions.displayAsVirtualStack( noiseImage );
		ImageProcessor imp = img.getProcessor();
		
		float dotRadius = dotSize * 0.5f;
		int dotIntSize = (int) dotSize;
		
		for (int i=0; i < numDots; i++) {
			int x = (int) (Math.random() * width - dotRadius);
			int y = (int) (Math.random() * height - dotRadius);
			imp.setColor(Color.WHITE);
			imp.fillOval(x, y, dotIntSize, dotIntSize);
		}
		// we changed the data, so update it
		img.updateImage();
		
		// create a Gaussian smoothing algorithm
		ImageFactory<FloatType> imgFactoryProcess
			= new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		OutOfBoundsStrategyFactory<FloatType> smootherOobFactory
			= new OutOfBoundsStrategyMirrorFactory<FloatType>();
		Converter<T, FloatType> typeConverterIn = new RealTypeConverter<T, FloatType>();
		Converter<FloatType, T> typeConverterOut = new RealTypeConverter<FloatType, T>();
		GaussianConvolution3<T, FloatType, T> smoother
			= new GaussianConvolution3<T, FloatType, T>(noiseImage, imgFactoryProcess, imgFactory,
					smootherOobFactory, typeConverterIn, typeConverterOut, smoothingSigma );
		
		// smooth the image
		if ( smoother.checkInput() && smoother.process() ) {
			return smoother.getResult();
		} else {
			throw new RuntimeException(smoother.getErrorMessage());
		}
	}
}
