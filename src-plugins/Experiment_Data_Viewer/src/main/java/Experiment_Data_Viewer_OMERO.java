import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ImageProcessor;

import java.awt.Checkbox;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
   Copyright 2012 Tom Kazimiers and the Fiji project. Fiji is just
   imageJ - batteries included.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see http://www.gnu.org/licenses/ .
 */

/**
 * A plugin which provides convenient access to a set of experiments.
 * It displays different files belonging to one experiment in a single
 * user interface. The set of experiments can currently be defined in a
 * file or retrieved from an OMERO server.
 *
 * @param <T>
 */
public class Experiment_Data_Viewer_OMERO implements PlugIn {

	public void run(String arg0) {
		if (showDialog()) {
            // do something
		}
	}

	public boolean showDialog() {
		/* create a new generic dialog for the
		 * display of various options.
		 */
		final GenericDialog gd
			= new GenericDialog("Experiment data viewer (OMERO based)");

		// set up the users preferences

        // add user-interface elements
        gd.addMessage("Please provide the OMERO server information.");



//		displayImages = Prefs.get(PREF_KEY+"displayImages", false);
//		autoSavePdf = Prefs.get(PREF_KEY+"autoSavePdf", true);
//		boolean displayShuffledCostes = Prefs.get(PREF_KEY+"displayShuffledCostes", false);
//		boolean useLiCh1 = Prefs.get(PREF_KEY+"useLiCh1", true);
//		boolean useLiCh2 = Prefs.get(PREF_KEY+"useLiCh2", true);
//		boolean useLiICQ = Prefs.get(PREF_KEY+"useLiICQ", true);
//		boolean useSpearmanRank = Prefs.get(PREF_KEY+"useSpearmanRank", true);
//		boolean useManders = Prefs.get(PREF_KEY+"useManders", true);
//		boolean useScatterplot = Prefs.get(PREF_KEY+"useScatterplot", true);
//		boolean useCostes = Prefs.get(PREF_KEY+"useCostes", true);
//		int psf = (int) Prefs.get(PREF_KEY+"psf", 3);
//		int nrCostesRandomisations = (int) Prefs.get(PREF_KEY+"nrCostesRandomisations", 10);
//
//		/* make sure the default indices are no bigger
//		 * than the amount of images we have
//		 */
//		index1 = clip( index1, 0, titles.length );
//		index2 = clip( index2, 0, titles.length );
//		indexMask = clip( indexMask, 0, roisAndMasks.length - 1);
//
//		gd.addChoice("Channel_1", titles, titles[index1]);
//		gd.addChoice("Channel_2", titles, titles[index2]);
//		gd.addChoice("ROI_or_mask", roisAndMasks, roisAndMasks[indexMask]);
//		//gd.addChoice("Use ROI", roiLabels, roiLabels[indexRoi]);
//
//		gd.addCheckbox("Show_\"Save_PDF\"_Dialog", autoSavePdf);
//		gd.addCheckbox("Display_Images_in_Result", displayImages);
//		gd.addCheckbox("Display_Shuffled_Images", displayShuffledCostes);
//		final Checkbox shuffleCb = (Checkbox) gd.getCheckboxes().lastElement();
//		// Add algorithm options
//		gd.addMessage("Algorithms:");
//		gd.addCheckbox("Li_Histogram_Channel_1", useLiCh1);
//		gd.addCheckbox("Li_Histogram_Channel_2", useLiCh2);
//		gd.addCheckbox("Li_ICQ", useLiICQ);
//		gd.addCheckbox("Spearman's_Rank_Correlation", useSpearmanRank);
//		gd.addCheckbox("Manders'_Correlation", useManders);
//		gd.addCheckbox("2D_Instensity_Histogram", useScatterplot);
//		gd.addCheckbox("Costes'_Significance_Test", useCostes);
//		final Checkbox costesCb = (Checkbox) gd.getCheckboxes().lastElement();
//		gd.addNumericField("PSF", psf, 1);
//		gd.addNumericField("Costes_randomisations", nrCostesRandomisations, 0);
//
//		// disable shuffle checkbox if costes checkbox is set to "off"
//		shuffleCb.setEnabled(useCostes);
//		costesCb.addItemListener(new ItemListener() {
//			@Override
//			public void itemStateChanged(ItemEvent e) {
//				shuffleCb.setEnabled(costesCb.getState());
//			}
//		});
//
		// show the dialog, finally
		gd.showDialog();
		// do nothing if dialog has been canceled
		if (gd.wasCanceled())
			return false;
//
//		ImagePlus imp1 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);
//		ImagePlus imp2 = WindowManager.getImage(gd.getNextChoiceIndex() + 1);
//
//		// make sure both images have the same bit-depth
//		if (imp1.getBitDepth() != imp2.getBitDepth()) {
//			IJ.showMessage("Both images must have the same bit-depth.");
//			return false;
//		}
//
//		// get information about the mask/ROI to use
//		indexMask = gd.getNextChoiceIndex();
//		if (indexMask == 0)
//			roiConfig = RoiConfiguration.None;
//		else if (indexMask == 1)
//			roiConfig = RoiConfiguration.Img1;
//		else if (indexMask == 2)
//			roiConfig = RoiConfiguration.Img2;
//		else if (indexMask == 3)
//			roiConfig = RoiConfiguration.RoiManager;
//		else {
//			roiConfig = RoiConfiguration.Mask;
//			/* Make indexMask the reference to the mask image to use.
//			 * To do this we reduce it by three for the first three
//			 * entries in the combo box.
//			 */
//			indexMask = indexMask - 4;
//		}
//
//		// save the ImgLib wrapped images as members
//		img1 = ImagePlusAdapter.wrap(imp1);
//		img2 = ImagePlusAdapter.wrap(imp2);
//
//		/* check if we have a valid ROI for the selected configuration
//		 * and if so, get the ROI's bounds. Alternatively, a mask can
//		 * be selected (that is basically all, but a rectangle).
//		 */
//		if (roiConfig == RoiConfiguration.Img1 && hasValidRoi(imp1)) {
//			createMasksFromImage(imp1);
//		} else if (roiConfig == RoiConfiguration.Img2 && hasValidRoi(imp2)) {
//			createMasksFromImage(imp2);
//		} else if (roiConfig == RoiConfiguration.RoiManager) {
//			if (!createMasksFromRoiManager(imp1.getWidth(), imp1.getHeight()))
//				return false;
//		} else if (roiConfig == RoiConfiguration.Mask) {
//			// get the image to be used as mask
//			ImagePlus maskImp = WindowManager.getImage(windowList[indexMask]);
//			Img<T> maskImg = ImagePlusAdapter.<T>wrap( maskImp );
//			// get a valid mask info for the image
//			MaskInfo mi = getBoundingBoxOfMask(maskImg);
//			masks.add( mi ) ;
//		} else {
//			/* if no ROI/mask is selected, just add an empty MaskInfo
//			 * to colocalise both images without constraints.
//			 */
//			masks.add(new MaskInfo(null, null));
//		}
//
//		// read out GUI data
//		autoSavePdf = gd.getNextBoolean();
//		displayImages = gd.getNextBoolean();
//		displayShuffledCostes = gd.getNextBoolean();
//		useLiCh1 = gd.getNextBoolean();
//		useLiCh2 = gd.getNextBoolean();
//		useLiICQ = gd.getNextBoolean();
//		useSpearmanRank = gd.getNextBoolean();
//		useManders = gd.getNextBoolean();
//		useScatterplot = gd.getNextBoolean();
//		useCostes = gd.getNextBoolean();
//		psf = (int) gd.getNextNumber();
//		nrCostesRandomisations = (int) gd.getNextNumber();
//
//		// save user preferences
//		Prefs.set(PREF_KEY+"autoSavePdf", autoSavePdf);
//		Prefs.set(PREF_KEY+"displayImages", displayImages);
//		Prefs.set(PREF_KEY+"displayShuffledCostes", displayShuffledCostes);
//		Prefs.set(PREF_KEY+"useLiCh1", useLiCh1);
//		Prefs.set(PREF_KEY+"useLiCh2", useLiCh2);
//		Prefs.set(PREF_KEY+"useLiICQ", useLiICQ);
//		Prefs.set(PREF_KEY+"useSpearmanRank", useSpearmanRank);
//		Prefs.set(PREF_KEY+"useManders", useManders);
//		Prefs.set(PREF_KEY+"useScatterplot", useScatterplot);
//		Prefs.set(PREF_KEY+"useCostes", useCostes);
//		Prefs.set(PREF_KEY+"psf", psf);
//		Prefs.set(PREF_KEY+"nrCostesRandomisations", nrCostesRandomisations);
//
//		// Parse algorithm options
//		pearsonsCorrelation = new PearsonsCorrelation<T>(PearsonsCorrelation.Implementation.Fast);
//
//		if (useLiCh1)
//			liHistogramCh1 = new LiHistogram2D<T>("Li - Ch1", true);
//		if (useLiCh2)
//			liHistogramCh2 = new LiHistogram2D<T>("Li - Ch2", false);
//		if (useLiICQ)
//			liICQ = new LiICQ<T>();
//		if (useSpearmanRank)
//		    SpearmanRankCorrelation = new SpearmanRankCorrelation<T>();
//		if (useManders)
//			mandersCorrelation = new MandersColocalization<T>();
//		if (useScatterplot)
//			histogram2D = new Histogram2D<T>("2D intensity histogram");
//		if (useCostes) {
//			costesSignificance = new CostesSignificanceTest<T>(pearsonsCorrelation,
//					psf, nrCostesRandomisations, displayShuffledCostes);
//		}

		return true;
	}
}
