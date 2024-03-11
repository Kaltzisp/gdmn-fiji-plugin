package org.gdmn.imagej.process;

import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.gdmn.imagej.utils.Filer;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.LutLoader;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

@Plugin(type = Command.class, menuPath = "2D Macro Tool>Segment Labels")
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

    public void process(String roiPath) {
        if (segmentationType.equals("roi -> myo/endo")) {
            segmentLabel(roiPath, "mask_myo.tif", "roi", "myo", "endo");
        } else if (segmentationType.equals("myo -> compact/trabecular")) {
            segmentLabel(roiPath, "mask_myo_compact.tif", "myo", "myo_compact", "myo_trabecular");
        } else if (segmentationType.equals("endo -> endo/epi")) {
            segmentLabel(roiPath, "mask_endo.tif", "endo", "endo", "epi");
        } else if (segmentationType.equals("trabecular -> sublayers")) {
            segmentLabel(roiPath, "sublayer_myo_trabecular_1.tif", "myo_trabecular", "myo_trabecular_base", "tmp_middle_apex");
            segmentLabel(roiPath, "sublayer_myo_trabecular_2.tif", "tmp_middle_apex", "myo_trabecular_middle", "myo_trabecular_apex");
            segmentLabel(roiPath, "sublayer_myo_trabecular_1.tif", "endo", "endo_base", "tmp_middle_apex");
            segmentLabel(roiPath, "sublayer_myo_trabecular_2.tif", "tmp_middle_apex", "endo_middle", "endo_apex");
            Filer.delete(roiPath, "labels", "label_tmp_middle_apex.tif");
            Filer.delete(roiPath, "zips", "zip_tmp_middle_apex.zip");
        }

    }

    private void segmentLabel(String roiPath, String baseMask, String baseLabel, String innerLabel, String outerLabel) {
        // Opening mask and roi managers.
        ImagePlus maskImp = new ImagePlus(Filer.getPath(roiPath, "masks", baseMask));
        Roi mask = maskImp.getRoi();
        maskImp.close();
        RoiManager baseRoiManager = new RoiManager(false);
        RoiManager altRoiManager = new RoiManager(false);
        baseRoiManager.runCommand("Open", Filer.getPath(roiPath, "zips", "zip_"+baseLabel+".zip"));

        // Setting up output labels.
        ImagePlus baseImp = new ImagePlus(Filer.getPath(roiPath, "labels", "label_"+baseLabel+".tif"));
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
        Filer.save(baseImp, roiPath, "labels", "label_"+innerLabel+".tif");
        baseImp.close();
        baseRoiManager.runCommand("Save", Filer.getSavePath(roiPath, "zips", "zip_"+innerLabel+".zip"));
        baseRoiManager.close();

        altIp.setColorModel(LutLoader.getLut("glasbey on dark"));
        altImp.setProcessor(altIp);
        Filer.save(altImp, roiPath, "labels", "label_"+outerLabel+".tif");
        altImp.close();
        altRoiManager.runCommand("Save", Filer.getSavePath(roiPath, "zips", "zip_"+outerLabel+".zip"));
        altRoiManager.close();
    }

}
