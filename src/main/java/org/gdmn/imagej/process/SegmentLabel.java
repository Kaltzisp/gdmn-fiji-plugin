package org.gdmn.imagej.process;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.LutLoader;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
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
 * Command to segment label image.
 */
@Plugin(type = Command.class, menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Segment Label", weight = 25)
})
public class SegmentLabel extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Segment label images</h2>";

    @Parameter(label = "Segmentation", choices = {
            "roi -> myo/endo",
            "myo -> compact/trabecular",
            "endo -> endo/epi",
            "endo -> endo/coronary"
    })
    private String segmentationType = Defaults.get("segmentationType", "roi -> myo/endo");

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    /**
     * Running segmentLabel according to specified type.
     */
    public void process(String basePath) {
        if (segmentationType.equals("roi -> myo/endo")) {
            segmentLabel(basePath, "mask_myo.tif", "roi", "myo", "endo", false);
        } else if (segmentationType.equals("myo -> compact/trabecular")) {
            segmentLabel(basePath, "mask_myo_compact.tif", "myo", "myo_compact", "myo_trabecular", false);
        } else if (segmentationType.equals("endo -> endo/epi")) {
            segmentLabel(basePath, "mask_epi.tif", "endo", "epi", "endo", false);
        } else if (segmentationType.equals("endo -> endo/coronary")) {
            segmentLabel(basePath, "mask_myo_compact.tif", "endo", "coronaries", "endo", true);
        }
    }

    /**
     * Segments a nuclei label into sublabels on a mask.
     *
     * @param basePath   the path to the image folder.
     * @param baseMask   the mask to segment on.
     * @param baseLabel  the base label.
     * @param innerLabel the name of the output inner label.
     * @param outerLabel the name of the output outer label.
     */
    public static void segmentLabel(String basePath, String baseMask, String baseLabel, String innerLabel,
            String outerLabel, boolean closeMask) {
        // Opening mask and roi managers.
        ImagePlus maskImp = new ImagePlus(Filer.getPath(basePath, "masks", baseMask));
        Roi mask;
        if (closeMask) {
            maskImp.killRoi();
            IJ.run(maskImp, "Fill Holes", "");
            maskImp.getProcessor().setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
            mask = ThresholdToSelection.run(maskImp);
        } else {
            mask = maskImp.getRoi();
        }
        if (mask == null) {
            IJ.log("Mask not found: " + baseMask);
            return;
        }
        // maskImp.setRoi(mask);
        // maskImp.show();
        // WaitForUserDialog dialog = new WaitForUserDialog("Draw mask", "Select mask then hit OK.");
        // dialog.show();
        maskImp.close();
        RoiManager baseRoiManager = new RoiManager(false);
        final RoiManager altRoiManager = new RoiManager(false);
        baseRoiManager.runCommand("Open", Filer.getPath(basePath, "zips", "zip_" + baseLabel + ".zip"));

        // Setting up output labels.
        ImagePlus baseImp = new ImagePlus(Filer.getPath(basePath, "labels", "label_" + baseLabel + ".tif"));
        ImageProcessor baseIp = baseImp.getProcessor();
        baseIp = baseImp.getProcessor();
        baseIp.setColor(0);
        baseIp.fill();
        ImagePlus altImp = baseImp.duplicate();
        ImageProcessor altIp = altImp.getProcessor();

        // Checking centroids.
        Roi[] rois = baseRoiManager.getRoisAsArray();
        for (int i = baseRoiManager.getCount() - 1; i >= 0; i--) {
            double[] centroid = rois[i].getContourCentroid();
            if (mask.containsPoint(centroid[0], centroid[1])) {
                baseIp.setColor(i + 1);
                baseIp.fill(rois[i]);
            } else {
                baseRoiManager.select(i);
                baseRoiManager.runCommand("Delete");
                altRoiManager.addRoi(rois[i]);
                altIp.setColor(i + 1);
                altIp.fill(rois[i]);
            }
        }

        // Setting colours and saving.
        baseIp.setColorModel(LutLoader.getLut("glasbey on dark"));
        baseImp.setProcessor(baseIp);
        Filer.save(baseImp, basePath, "labels", "label_" + innerLabel + ".tif");
        baseImp.close();
        baseRoiManager.runCommand("Save", Filer.getPath(basePath, "zips", "zip_" + innerLabel + ".zip"));
        baseRoiManager.close();

        altIp.setColorModel(LutLoader.getLut("glasbey on dark"));
        altImp.setProcessor(altIp);
        Filer.save(altImp, basePath, "labels", "label_" + outerLabel + ".tif");
        altImp.close();
        altRoiManager.runCommand("Save", Filer.getPath(basePath, "zips", "zip_" + outerLabel + ".zip"));
        altRoiManager.close();
    }

}
