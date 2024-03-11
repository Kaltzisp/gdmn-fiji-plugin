package org.gdmn.imagej.process;

import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.gdmn.imagej.utils.Filer;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;

@Plugin(type = Command.class, label = "Segment sublayers", menu = {
    @Menu(label = "2D Macro Tool"),
    @Menu(label = "Segment sublayers", weight = 6)
})
public class SegmentSublayers extends BatchCommand {
    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Segment into sublayers</h2>";

    @Parameter(label = "Number of sublayers:")
    private int numSublayers = Integer.parseInt(Defaults.get("numSublayers", "3"));

    @Parameter(label = "Subsegment trabeculae:")
    private boolean subsegmentTrabeculae = true;

    @Parameter(label = "Subsegment endocardium:")
    private boolean subsegmentEndocardium = true;

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void process(String roiPath) {
        // Opening files.
        if (subsegmentEndocardium) {
            subSegment(roiPath, "mask_myo_compact.tif", "label_myo_trabecular.tif", "sublayer_myo_trabecular");
        }
        if (subsegmentTrabeculae) {
            // NEED TO FIX THIS LINE. SHOULD PROBABLY JUST USE THE SAME FOR ENDOCARDIAL SEGMENTATION.
            // Instead of running again here, simply add an option for users in the SegmentLabel command to segment according to generated sublayers.
            // subSegment(roiPath, "mask_myo_compact.tif", "label_");
        }
    }

    private void subSegment(String roiPath, String baseMask, String baseLabel, String sublayer) {
        // Opening mask.
        ImagePlus maskImp = new ImagePlus(Filer.getPath(roiPath, "masks", baseMask));
        Roi mask = maskImp.getRoi();
        maskImp.deleteRoi();
        ImageProcessor maskIp = maskImp.getProcessor();
        maskIp.setColor(0);
        maskIp.fill();

        // Opening label and getting total non-zero area.
        ImagePlus labelImp = new ImagePlus(Filer.getPath(roiPath, "labels", baseLabel));
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
            Filer.save(outputMask, roiPath, "masks", sublayer + "_" + i + ".tif");
            outputMask.close();
        }
    }

    private double getArea(ImagePlus imp, Roi roi) {
        imp.setRoi(roi);
        return imp.getStatistics(Measurements.AREA_FRACTION).areaFraction * imp.getStatistics(Measurements.AREA).area;
    }
}
