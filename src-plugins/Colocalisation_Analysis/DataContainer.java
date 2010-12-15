import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * The DataContainer keeps all the source data, pre-processing results and
 * algorithms that have been executed. It allows a client to get most its
 * content and makes the source image and channel information available
 * to a client.

 * @param <T>
 */
public class DataContainer<T extends RealType<T>> {

	// some general image statistics
	double meanCh1, meanCh2, minCh1, maxCh1, minCh2, maxCh2, integralCh1, integralCh2;
	// The source images that the results are based on
	Image<T> sourceImage1, sourceImage2;
	// The channels of the source images that the result relate to
	int ch1, ch2;
	
	InputCheck inputCheck = null;
	AutoThresholdRegression autoThreshold = null;

	// a list that contains all added algorithms
	List< Algorithm > algorithms = new ArrayList< Algorithm >();

	/**
	 * Creates a new {@link DataContainer} for a specific set of image and
	 * channel combination.
	 * We create default thresholds here that are the max and min of the
	 * type of the source image channels.
	 * 
	 * @param src1 The channel one image source
	 * @param src2 The channel two image source
	 * @param ch1 The channel one image channel
	 * @param ch2 The channel two image channel
	 */
	public DataContainer(Image<T> src1, Image<T> src2, int ch1, int ch2) {
		sourceImage1 = src1;
		sourceImage2 = src2;
		this.ch1 = ch1;
		this.ch2 = ch2;
		
		meanCh1 = ImageStatistics.getImageMean(sourceImage1);
		meanCh2 = ImageStatistics.getImageMean(sourceImage2);
		minCh1 = ImageStatistics.getImageMin(sourceImage1).getRealDouble();
		minCh2 = ImageStatistics.getImageMin(sourceImage2).getRealDouble();
		maxCh1 = ImageStatistics.getImageMax(sourceImage1).getRealDouble();
		maxCh2 = ImageStatistics.getImageMax(sourceImage2).getRealDouble();
		integralCh1 = ImageStatistics.getImageIntegral(sourceImage1);
		integralCh2 = ImageStatistics.getImageIntegral(sourceImage2);
	}
	
	/**
	 * Creates a new {@link DataContainer} for a specific set of image and
	 * channel combination. It will give access to the image according to
	 * the region of interest (ROI) passed. Default thresholds, min, max and
	 * mean will be set according to the ROI as well.
	 * 
	 * @param src1 The channel one image source
	 * @param src2 The channel two image source
	 * @param ch1 The channel one image channel
	 * @param ch2 The channel two image channel
	 * @param offset The offset of the ROI in each dimension
	 * @param size The size of the ROI in each dimension
	 */
	public DataContainer(Image<T> src1, Image<T> src2, int ch1, int ch2,
			final int[] offset, final int size[]) {
		this(new RoiImage<T>(src1, offset, size),
			 new RoiImage<T>(src2, offset, size),
			 ch1, ch2);
	}

	public Image<T> getSourceImage1() {
		return sourceImage1;
	}

	public Image<T> getSourceImage2() {
		return sourceImage2;
	}

	public int getCh1() {
		return ch1;
	}

	public int getCh2() {
		return ch2;
	}
	public double getMeanCh1() {
		return meanCh1;
	}

	public double getMeanCh2() {
		return meanCh2;
	}

	public double getMinCh1() {
		return minCh1;
	}

	public double getMaxCh1() {
		return maxCh1;
	}

	public double getMinCh2() {
		return minCh2;
	}

	public double getMaxCh2() {
		return maxCh2;
	}
	
	public double getIntegralCh1() {
		return integralCh1;
	}
	
	public double getIntegralCh2() {
		return integralCh2;
	}
	
	public InputCheck getInputCheck() {
		return inputCheck;
	}

	public Algorithm setInputCheck(InputCheck inputCheck) {
		this.inputCheck = inputCheck;
		return inputCheck;
	}

	public AutoThresholdRegression getAutoThreshold() {
		return autoThreshold;
	}

	public Algorithm setAutoThreshold(AutoThresholdRegression autoThreshold) {
		this.autoThreshold = autoThreshold;
		return autoThreshold;
	}
}
