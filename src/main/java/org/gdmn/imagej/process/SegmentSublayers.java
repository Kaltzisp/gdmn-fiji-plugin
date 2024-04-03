package org.gdmn.imagej.process;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.gdmn.imagej.utils.Filer;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

/**
 * Command to segment tissue labels into sublayers.
 */
@Plugin(type = Command.class, menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Segment Sublayers", weight = 26)
})
public class SegmentSublayers extends BatchCommand {
    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Segment into sublayers</h2>";

    @Parameter(label = "Number of sublayers:")
    private int numSublayers = Integer.parseInt(Defaults.get("numSublayers", "3"));

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    /**
     * Runs sublayer segmentation.
     */
    public void process(String basePath) {
        // Creating layer boundary masks.
        subSegment(basePath, "mask_myo_compact.tif", "label_myo_trabecular.tif", "sublayer_myo_trabecular");

        // Subsegmenting and saving labels.
        if (numSublayers == 2) {
            SegmentLabel.segmentLabel(basePath, "sublayer_myo_trabecular_1.tif", "myo_trabecular",
                    "myo_trabecular_base",
                    "myo_trabecular_apex", false);
            SegmentLabel.segmentLabel(basePath, "sublayer_myo_trabecular_1.tif", "endo", "endo_base",
                    "endo_apex", false);
        } else if (numSublayers == 3) {
            SegmentLabel.segmentLabel(basePath, "sublayer_myo_trabecular_1.tif", "myo_trabecular",
                    "myo_trabecular_base",
                    "tmp_middle_apex", false);
            SegmentLabel.segmentLabel(basePath, "sublayer_myo_trabecular_2.tif", "tmp_middle_apex",
                    "myo_trabecular_middle",
                    "myo_trabecular_apex", false);
            SegmentLabel.segmentLabel(basePath, "sublayer_myo_trabecular_1.tif", "endo", "endo_base",
                    "tmp_middle_apex", false);
            SegmentLabel.segmentLabel(basePath, "sublayer_myo_trabecular_2.tif", "tmp_middle_apex", "endo_middle",
                    "endo_apex", false);
            Filer.delete(basePath, "labels", "label_tmp_middle_apex.tif");
            Filer.delete(basePath, "zips", "zip_tmp_middle_apex.zip");
        }
    }

    /**
     * Segments a tissue nuclei label into spatial sublayers.
     *
     * @param basePath  the path to the image folder.
     * @param baseMask  the name of the base layer mask.
     * @param baseLabel the label to use for segmentation.
     * @param sublayer  the name of the sublayer output.
     */
    private void subSegment(String basePath, String baseMask, String baseLabel, String sublayer) {
        // Opening mask.
        ImagePlus maskImp = new ImagePlus(Filer.getPath(basePath, "masks", baseMask));
        final Roi mask = maskImp.getRoi();
        ImageProcessor maskIp = maskImp.getProcessor();
        maskImp.deleteRoi();
        maskIp.setColor(0);
        maskIp.fill();

        // Opening label and getting total non-zero area.
        ImagePlus labelImp = new ImagePlus(Filer.getPath(basePath, "labels", baseLabel));
        double totalArea = labelImp.getStatistics(Measurements.AREA_FRACTION).areaFraction
                * labelImp.getStatistics().area;
        labelImp.setRoi(mask);

        // Initialising loop variables.
        double expansion = 0;
        double stepSize = 128;

        // Looping through to get 95% coverage.
        while (stepSize >= 1) {
            double selectedArea = 0;
            while (selectedArea < 0.95 * totalArea) {
                expansion += stepSize;
                Roi enlargedRoi = RoiEnlarger.enlarge(mask, expansion);
                selectedArea = this.getArea(labelImp, enlargedRoi);
            }
            expansion = expansion - stepSize;
            stepSize = stepSize * 0.5;
        }
        expansion += 1;

        // Loop through layers to create output masks.
        for (int i = 1; i <= this.numSublayers; i++) {
            // Filling outer ROI white.
            ImagePlus outputMask = new ImagePlus("Mask", maskIp.duplicate());
            Roi enlargedRoi = RoiEnlarger.enlarge(mask, (expansion * i / this.numSublayers));
            outputMask.setRoi(enlargedRoi);
            ImageProcessor ip = outputMask.getProcessor();
            ip.setColor(255);
            ip.fill(enlargedRoi);
            // If final layer, fill all unclassified area.
            if (i == this.numSublayers) {
                ip.fill();
            }
            // Filling inner ROI black and drawing roi.
            Roi innerRoi = RoiEnlarger.enlarge(mask, (expansion * (i - 1) / this.numSublayers));
            outputMask.setRoi(innerRoi);
            ip.setColor(0);
            ip.fill(innerRoi);
            ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
            Roi roi = ThresholdToSelection.run(outputMask);
            outputMask.setRoi(roi);
            // Saving mask.
            Filer.save(outputMask, basePath, "masks", sublayer + "_" + i + ".tif");
            outputMask.close();
        }
    }

    private double getArea(ImagePlus imp, Roi roi) {
        imp.setRoi(roi);
        return imp.getStatistics(Measurements.AREA_FRACTION).areaFraction * imp.getStatistics(Measurements.AREA).area;
    }
}
