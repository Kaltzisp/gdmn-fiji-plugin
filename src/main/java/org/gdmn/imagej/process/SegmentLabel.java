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

    @Parameter(label = "Base label", persist = false)
    public String baseLabel = Defaults.get("baseLabel", "roi");

    @Parameter(label = "Segmentation mask", persist = false)
    public String segmentationMask = Defaults.get("segmentationMask", "myo");

    @Parameter(label = "Inner label", persist = false)
    public String innerLabel = Defaults.get("innerLabel", "myo");

    @Parameter(label = "Outer label", persist = false)
    public String outerLabel = Defaults.get("outerLabel", "endo");

    @Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
    private String info = "<p style='width: 500px;'>"
            + "This command segments a label image into two new labels using a tissue mask. The inner_label will contain all nuclei within the mask "
            + "boundaries, while the outer_label will contain nuclei located outside the mask.<br><br>"
            + "This command can be used in combination with the <i>Draw Custom Mask</i> to manually separate data that does't have specific staining "
            + "(e.g. epicardial nuclei). It can also be used in a similar way to dispose of noisy data (e.g. blood) unuseful to analysis.<br><br>"
            + "Do not include 'label_' or '.tif' when using this tool; only specify the tissue name. Alternatively, "
            + "use the dropdown menu below to load values for a preset configuration.";

    @Parameter(label = "Segmentation", choices = {
            "roi -> myo/endo",
            "myo -> compact/trabecular",
            "endo -> endo/epi",
            "endo -> endo/coronaries"
    }, callback = "updateSegmentation")
    private String segmentationPreset = Defaults.get("segmentationPreset", "roi -> myo/endo");

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    /** Sets a segmentation preset. */
    public void updateSegmentation() {
        if (segmentationPreset.equals("roi -> myo/endo")) {
            baseLabel = "roi";
            segmentationMask = "myo";
            innerLabel = "myo";
            outerLabel = "endo";
        } else if (segmentationPreset.equals("myo -> compact/trabecular")) {
            baseLabel = "myo";
            segmentationMask = "myo_compact";
            innerLabel = "myo_compact";
            outerLabel = "myo_trabecular";
        } else if (segmentationPreset.equals("endo -> endo/epi")) {
            baseLabel = "endo";
            segmentationMask = "epi";
            innerLabel = "epi";
            outerLabel = "endo";
        } else if (segmentationPreset.equals("endo -> endo/coronaries")) {
            baseLabel = "endo";
            segmentationMask = "myo_compact";
            innerLabel = "coronaries";
            outerLabel = "endo";
        }
    }

    /** Running segmentLabel according to specified type. */
    public void process(String basePath) {
        if (innerLabel.equals("coronaries")) {
            segmentLabel(basePath, "mask_" + segmentationMask + ".tif", baseLabel, innerLabel, outerLabel, true);
        } else {
            segmentLabel(basePath, "mask_" + segmentationMask + ".tif", baseLabel, innerLabel, outerLabel, false);
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

        // Closing mask image and creating ROI Managers.
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
