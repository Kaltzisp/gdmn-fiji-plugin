package org.gdmn.imagej.utils;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;

/** Masks utility class. */
public class Masks {
    /**
     * Combines a set of masks using AND.
     *
     * @param imps the mask images with ROIs selected.
     * @return the combined mask.
     */
    public static ImagePlus and(ImagePlus... imps) {
        ImagePlus imp = imps[0].duplicate();
        ImageProcessor ip = imp.getProcessor();
        ip.setColor(0);
        for (int i = 1; i < imps.length; i++) {
            Roi roi = imps[i].getRoi();
            imp.setRoi(roi);
            ip.fillOutside(roi);
        }
        ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
        Roi roi = ThresholdToSelection.run(imp);
        imp.setRoi(roi);
        return imp;
    }
}
