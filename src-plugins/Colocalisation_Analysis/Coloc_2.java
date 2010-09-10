import java.util.ArrayList;
import java.util.List;
import java.awt.Rectangle;
import java.io.File;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.DoubleType;
import mpicbg.imglib.type.ComparableType;
import mpicbg.imglib.type.TypeConverter;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursor;
import imglib.mpicbg.imglib.cursor.special.meta.AboveThresholdPredicate;
import imglib.mpicbg.imglib.cursor.special.meta.BelowThresholdPredicate;
import imglib.mpicbg.imglib.cursor.special.meta.Predicate;

/**
 * A plugin which does colocalisation on two images.
 * 
 * @param <T>
 */
public class Coloc_2<T extends RealType<T>> implements PlugIn {

	// Allowed types of ROI configuration
	protected enum RoiConfiguration {None, Img1, Img2};
	// indicates if a ROI should be used
	boolean useRoi = false;
	
	// the images to work on
	Image<T> img1, img2;
	
	public void run(String arg0) {
		// Development code
		ImagePlus imp1 = WindowManager.getImage(1);
		if (imp1 == null)
			imp1 = IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/red.tif");
		img1 = ImagePlusAdapter.wrap(imp1);  
		
		ImagePlus imp2 =  WindowManager.getImage(2);
		if (imp2 == null)
			imp2 =IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/green.tif");
		img2 = ImagePlusAdapter.wrap(imp2); 
		
		int theImg1Channel = 1, theImg2Channel = 1;
		
		
		// configure the ROI
		RoiConfiguration roiConfig = RoiConfiguration.Img1;
		
		// Development code end
		
		// configure the ROI
		Rectangle roi = null;
		/* check if we have a valid ROI for the selected configuration
		 * and if so, get the ROI's bounds. Currently, only rectangular
		 * ROIs are supported.
		 */
		if (roiConfig == RoiConfiguration.Img1 && hasValidRoi(imp1)) {
			roi = imp1.getRoi().getBounds();
		} else if (roiConfig == RoiConfiguration.Img2 && hasValidRoi(imp2)) {
			roi = imp2.getRoi().getBounds();
		}
		useRoi = (roi != null);
		
		// create a new container for the selected images and channels
		DataContainer container;
		if (useRoi) {
			int roiOffset[] = new int[] {roi.x, roi.y};
			int roiSize[] = new int[] {roi.width, roi.height};
			container = new DataContainer(img1, img2, theImg1Channel, theImg2Channel,
					roiOffset, roiSize);
		} else {
			container = new DataContainer(img1, img2, theImg1Channel, theImg2Channel);
		}
		
		// this list contains the algorithms that will be run when the user clicks ok
		List<Algorithm> userSelectedJobs = new ArrayList<Algorithm>();
		
		// add some preprocessing jobs:
		userSelectedJobs.add( container.setInputCheck(
			new InputCheck()) );
		userSelectedJobs.add( container.setAutoThreshold(
			new AutoThresholdRegression()) );
		
		// add user selected algorithms
		userSelectedJobs.add( container.setPearsonsCorrelation(
			new PearsonsCorrelation(PearsonsCorrelation.Implementation.Fast) ) );
		userSelectedJobs.add( container.setLiHistogramCh1(
			new LiHistogram2D("Li - Ch1", true)) );
		userSelectedJobs.add( container.setLiHistogramCh2(
			new LiHistogram2D("Li - Ch2", false)) );
		userSelectedJobs.add( container.setLiICQ(
			new LiICQ()) );
		userSelectedJobs.add( container.setMandersCorrelation(
			new MandersCorrelation<T>()) );
		userSelectedJobs.add( container.setHistogram2D(
			new Histogram2D("hello")) );
		
		// performance test
		Image<T> i1 = container.getSourceImage1();
		Image<T> i2 = container.getSourceImage2();
		PearsonsCorrelation pc = new PearsonsCorrelation(PearsonsCorrelation.Implementation.Fast);

		int count = 200;
		
		double result = pc.fastPearsons(i1, i2);
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
	    	pc.fastPearsons(i1, i2);
	    }
		double mean = (System.currentTimeMillis() - startTime) / (double)count;

		System.out.println( "Total fast (" + mean + "ms): " + result );
		
		result = pc.classicPearsons(container);
		startTime = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			pc.classicPearsons(container);
	    }
		mean = (System.currentTimeMillis() - startTime) / (double)count;

		System.out.println( "Total classic  (" + mean + "ms): " + result );
		
		TwinValueRangeCursor<T> tvc = new TwinValueRangeCursor<T>(
					i1.createCursor(), i2.createCursor());
		
		result = pc.fastPearsons(tvc);
		startTime = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			tvc.reset();
			pc.fastPearsons(tvc);
	    }
		tvc.close();
		mean = (System.currentTimeMillis() - startTime) / (double)count;

		System.out.println( "Total cursor  (" + mean + "ms): " + result );
		
		// Below threshold:
		
		AutoThresholdRegression<T> autoThreshold = container.getAutoThreshold(); 
		
		try {
			autoThreshold.execute(container);
		}
		catch (MissingPreconditionException e){
			System.out.println("Exception occured in Algorithm preconditions: " + e.getMessage());
		}
	
		result = pc.fastPearsons(i1, i2,
				autoThreshold.getCh1MaxThreshold(),
				autoThreshold.getCh2MaxThreshold(), false);
		startTime = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			pc.fastPearsons(i1, i2,
					autoThreshold.getCh1MaxThreshold(),
					autoThreshold.getCh2MaxThreshold(), false);
	    }
		mean = (System.currentTimeMillis() - startTime) / (double)count;

		System.out.println( "Below fast  (" + mean + "ms): " + result );
		
		T t1 = i1.createType();
		double threshold1 = autoThreshold.getCh1MaxThreshold();
		if ( t1.getMinValue() > threshold1 )
			t1.setReal( t1.getMinValue() );
		else if (t1.getMaxValue() < threshold1)
			t1.setReal( t1.getMaxValue() );
		else
			t1.setReal( threshold1 );
		
		T t2 = i2.createType();
		double threshold2 = autoThreshold.getCh2MaxThreshold();
		if ( t2.getMinValue() > threshold2 )
			t2.setReal( t2.getMinValue() );
		else if (t2.getMaxValue() < threshold2)
			t2.setReal( t2.getMaxValue() );
		else
			t2.setReal( threshold2 );
		
		Predicate<T> p1 = new BelowThresholdPredicate<T>( t1 );
		Predicate<T> p2 = new BelowThresholdPredicate<T>( t2 );
		
		tvc = new imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursor<T>(
				i1.createCursor(), i2.createCursor(), p1, p2);
	
		result = pc.fastPearsons(tvc);
		startTime = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			tvc.reset();
			pc.fastPearsons(tvc);
	    }
		tvc.close();
		mean = (System.currentTimeMillis() - startTime) / (double)count;
		
		System.out.println( "Below cursor  (" + mean + "ms): " + result );
		
		result = pc.fastPearsons(i1, i2,
				autoThreshold.getCh1MaxThreshold(),
				autoThreshold.getCh2MaxThreshold(), true);
		startTime = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			pc.fastPearsons(i1, i2,
					autoThreshold.getCh1MaxThreshold(),
					autoThreshold.getCh2MaxThreshold(), true);
	    }
		mean = (System.currentTimeMillis() - startTime) / (double)count;

		System.out.println( "Above fast  (" + mean + "ms): " + result );
		
		p1 = new AboveThresholdPredicate<T>( t1 );
		p2 = new AboveThresholdPredicate<T>( t2 );
		
		tvc = new imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursor<T>(
				i1.createCursor(), i2.createCursor(), p1, p2);
	
		result = pc.fastPearsons(tvc);
		startTime = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			tvc.reset();
			pc.fastPearsons(tvc);
	    }
		tvc.close();
		mean = (System.currentTimeMillis() - startTime) / (double)count;
		
		System.out.println( "Above cursor  (" + mean + "ms): " + result );
		
		// end performance test
		
		try {
			for (Algorithm a : userSelectedJobs){
				a.execute(container);
			}
		}
		catch (MissingPreconditionException e){
			System.out.println("Exception occured in Algorithm preconditions: " + e.getMessage());
		}
			
		Display theResultDisplay = new SingleWindowDisplay();
		//Display theResultDisplay = new EasyDisplay();
		theResultDisplay.display(container);
	}
	
	/**
	 * Returns true if a custom ROI has been selected, i.e if the current
	 * ROI does not have the extent of the whole image.
	 * @return true if custom ROI selected, false otherwise
	 */
	protected boolean hasValidRoi(ImagePlus imp) {
		Roi roi = imp.getRoi();
		if (roi == null)
			return false;
		
		Rectangle theROI = roi.getBounds();
		
		// if the ROI is the same size as the image (default ROI), return false
		return (theROI.height != imp.getHeight()
					|| theROI.width != imp.getWidth());
	}
}
