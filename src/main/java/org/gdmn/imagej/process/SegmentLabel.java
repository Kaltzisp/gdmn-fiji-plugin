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
    })
    private String segmentationType = Defaults.get("segmentationType", "roi -> myo/endo");

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void process(String roiPath) {
        // Getting arguments.
        String baseMask;
        String baseZip;
        String baseLabel;
        String innerLabel;
        String outerLabel;
        String innerRois;
        String outerRois;
        if (segmentationType.equals("roi -> myo/endo")) {
            baseMask = "mask_myo.tif";
            baseZip = "zip_roi.zip";
            baseLabel = "label_roi.tif";
            innerLabel = "label_myo.tif";
            outerLabel = "label_endo.tif";
            innerRois = "zip_myo.zip";
            outerRois = "zip_endo.zip";
        } else if (segmentationType.equals("myo -> compact/trabecular")) {
            baseMask = "mask_myo_compact.tif";
            baseZip = "zip_myo.zip";
            baseLabel = "label_myo.tif";
            innerLabel = "label_myo_compact.tif";
            outerLabel = "label_myo_trabecular.tif";
            innerRois = "zip_myo_compact.zip";
            outerRois = "zip_myo_trabecular.zip";
        } else if (segmentationType.equals("endo -> endo/epi")) {
            baseMask = "mask_endo.tif";
            baseZip = "zip_endo.zip";
            baseLabel = "label_endo.tif";
            innerLabel = "label_endo.tif";
            outerLabel = "label_epi.tif";
            innerRois = "zip_endo.zip";
            outerRois = "zip_epi.zip";
        } else {
            return;
        }

        // Opening mask and roi managers.
        ImagePlus maskImp = new ImagePlus(Filer.getPath(roiPath, "masks", baseMask));
        Roi mask = maskImp.getRoi();
        maskImp.close();
        RoiManager baseRoiManager = new RoiManager(false);
        RoiManager altRoiManager = new RoiManager(false);
        baseRoiManager.runCommand("Open", Filer.getPath(roiPath, "zips", baseZip));

        // Setting up output labels.
        ImagePlus baseImp = new ImagePlus(Filer.getPath(roiPath, "labels", baseLabel));
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
        Filer.save(baseImp, roiPath, "labels", innerLabel);
        baseImp.close();
        baseRoiManager.runCommand("Save", Filer.getSavePath(roiPath, "zips", innerRois));
        baseRoiManager.close();

        altIp.setColorModel(LutLoader.getLut("glasbey on dark"));
        altImp.setProcessor(altIp);
        Filer.save(altImp, roiPath, "labels", outerLabel);
        altImp.close();
        altRoiManager.runCommand("Save", Filer.getSavePath(roiPath, "zips", outerRois));
        altRoiManager.close();

    }

}
