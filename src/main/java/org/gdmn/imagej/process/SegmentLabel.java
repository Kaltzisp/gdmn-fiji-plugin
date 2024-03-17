package org.gdmn.imagej.process;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.LutLoader;
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
        @Menu(label = "Segment Label", weight = 5)
})
public class SegmentLabel extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Segment label images</h2>";

    @Parameter(label = "Segmentation", choices = {
            "roi -> myo/endo",
            "myo -> compact/trabecular",
            "endo -> endo/epi",
            "trabecular -> sublayers"
    })
    private String segmentationType = Defaults.get("segmentationType", "roi -> myo/endo");

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    /**
     * Running segmentLabel according to specified type.
     */
    public void process(String basePath) {
        if (segmentationType.equals("roi -> myo/endo")) {
            segmentLabel(basePath, "mask_myo.tif", "roi", "myo", "endo");
        } else if (segmentationType.equals("myo -> compact/trabecular")) {
            segmentLabel(basePath, "mask_myo_compact.tif", "myo", "myo_compact", "myo_trabecular");
        } else if (segmentationType.equals("endo -> endo/epi")) {
            segmentLabel(basePath, "mask_endo.tif", "endo", "endo", "epi");
        } else if (segmentationType.equals("trabecular -> sublayers")) {
            segmentLabel(basePath, "sublayer_myo_trabecular_1.tif", "myo_trabecular", "myo_trabecular_base",
                    "tmp_middle_apex");
            segmentLabel(basePath, "sublayer_myo_trabecular_2.tif", "tmp_middle_apex", "myo_trabecular_middle",
                    "myo_trabecular_apex");
            segmentLabel(basePath, "sublayer_myo_trabecular_1.tif", "endo", "endo_base", "tmp_middle_apex");
            segmentLabel(basePath, "sublayer_myo_trabecular_2.tif", "tmp_middle_apex", "endo_middle", "endo_apex");
            Filer.delete(basePath, "labels", "label_tmp_middle_apex.tif");
            Filer.delete(basePath, "zips", "zip_tmp_middle_apex.zip");
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
    private void segmentLabel(String basePath, String baseMask, String baseLabel, String innerLabel,
            String outerLabel) {
        // Opening mask and roi managers.
        ImagePlus maskImp = new ImagePlus(Filer.getPath(basePath, "masks", baseMask));
        final Roi mask = maskImp.getRoi();
        if (mask == null) {
            IJ.log("Mask not found: " + baseMask);
            return;
        }
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
