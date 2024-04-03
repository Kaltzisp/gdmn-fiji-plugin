package org.gdmn.imagej.process;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.gdmn.imagej.utils.Filer;
import org.gdmn.imagej.utils.Masks;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

/**
 * Command to generate quantifications.
 */
@Plugin(type = Command.class, menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Analyze Marker", weight = 27)
})
public class Quantify extends BatchCommand {
    private ImagePlus intensityImp;
    private ImagePlus activityImp;
    private RoiManager roiManager;

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Generate quantifications</h2>";

    @Parameter(label = "Threshold:", style = "slider, format:#.####", min = "0", max = "1", stepSize = "0.0125", callback = "updatePreview")
    private double markerThreshold = Double.parseDouble(Defaults.get("markerThreshold", "0.3"));

    @Parameter(label = "Show preview:", callback = "showPreview")
    private boolean showPreview = false;

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    /**
     * Shows a preview of the thresholded marker image.
     */
    public void showPreview() {
        if (this.showPreview) {

            // Getting preview image.
            String previewDir = Filer.getBasePaths(this.selectedDir, this.filePattern).get(0).getParent().toString();

            // Opening ROI Manager and images.
            this.roiManager = new RoiManager(false);
            this.roiManager.runCommand("Open", Filer.getPath(previewDir, "zips", "zip_roi.zip"));
            this.intensityImp = new ImagePlus(Filer.getPath(previewDir, "channels", "marker.tif"));
            this.activityImp = new ImagePlus(Filer.getPath(previewDir, "labels", "label_roi.tif"));
            ImageConverter converter = new ImageConverter(this.activityImp);
            converter.convertToGray8();

            // Getting image dimensions.
            double screenWidth = IJ.getScreenSize().getWidth();
            double screenHeight = IJ.getScreenSize().getHeight();
            int impWidth = this.intensityImp.getWidth();
            int impHeight = this.intensityImp.getHeight();
            double scale = 1 / Math.max(2 * impWidth / screenWidth, 2 * impHeight / screenHeight);

            // Showing images.
            this.intensityImp.show();
            this.intensityImp.getWindow().setLocationAndSize((int) (screenWidth / 2 - impWidth * scale),
                    (int) (screenHeight / 2 - 100), (int) (impWidth * scale), (int) (impHeight * scale));
            this.activityImp.show();
            this.activityImp.getWindow().setLocationAndSize((int) (screenWidth / 2),
                    (int) (screenHeight / 2 - 100), (int) (impWidth * scale), (int) (impHeight * scale));
            updatePreview();

        } else {
            this.closePreview();
        }
    }

    private void closePreview() {
        if (this.roiManager != null) {
            this.roiManager.close();
        }
        if (this.intensityImp != null) {
            this.intensityImp.close();
        }
        if (this.activityImp != null) {
            this.activityImp.close();
        }
    }

    /**
     * Updates the preview image.
     */
    public void updatePreview() {
        Roi roi = this.intensityImp.getRoi();
        if (this.intensityImp.isVisible() && this.activityImp.isVisible()) {
            Roi[] rois = this.roiManager.getRoisAsArray();
            // Checking intensities.
            for (int i = 0; i < rois.length; i++) {
                this.intensityImp.setRoi(rois[i]);
                ImageProcessor ip = this.activityImp.getProcessor();
                double mean = this.intensityImp.getStatistics(Measurements.MEAN).mean;
                if (mean > markerThreshold * 255) {
                    // Set colour to red.
                    ip.setColor(106);
                    ip.fill(rois[i]);
                } else {
                    // Set colour to blue.
                    ip.setColor(46);
                    ip.fill(rois[i]);
                }
            }
        }
        this.activityImp.updateAndDraw();
        this.intensityImp.setRoi(roi);
    }

    /**
     * Extracts and saves quantifications for an image folder and saves marker
     * labels.
     */
    public void process(String basePath) {
        this.closePreview();

        // Initialising output file.
        final List<String> data = new ArrayList<>();

        // Opening label image.
        ImagePlus labelImp = new ImagePlus(Filer.getPath(basePath, "labels", "label_roi.tif"));
        ImageConverter converter = new ImageConverter(labelImp);
        converter.convertToGray8();
        ImageProcessor labelIp = labelImp.getProcessor();
        labelIp.setColor(0);
        labelIp.fill();

        // Opening intensity image.
        ImagePlus imp = new ImagePlus(Filer.getPath(basePath, "channels", "marker.tif"));

        // Getting list of zips.
        File dir = new File(Filer.getPath(basePath, "zips", ""));
        File[] files = dir.listFiles();
        for (File file : files) {
            String filePath = file.getPath();
            String fileName = file.getName().substring(4, file.getName().length() - 4);
            String fileExtension = filePath.substring(filePath.length() - 4);
            if (fileExtension.equals(".zip")) {
                labelIp.setColor(0);
                labelIp.fill();
                // Opening ROI manager.
                RoiManager roiManager = new RoiManager(false);
                roiManager.runCommand("Open", filePath);
                Roi[] rois = roiManager.getRoisAsArray();
                // Checking intensities.
                int numberActive = 0;
                for (int i = 0; i < rois.length; i++) {
                    imp.setRoi(rois[i]);
                    double mean = imp.getStatistics(Measurements.MEAN).mean;
                    if (mean > markerThreshold * 255) {
                        // Set colour to red.
                        labelIp.setColor(106);
                        labelIp.fill(rois[i]);
                        numberActive += 1;
                    } else {
                        // Set colour to blue.
                        labelIp.setColor(46);
                        labelIp.fill(rois[i]);
                    }
                }
                Filer.save(labelImp, basePath, "marker", "marker_" + fileName + ".tif");
                data.add("count_" + fileName + "_active=" + numberActive);
                data.add("count_" + fileName + "_total=" + rois.length);
                roiManager.close();
            }
        }

        // Closing images.
        imp.close();
        labelImp.close();

        // Getting mask areas.
        dir = new File(Filer.getPath(basePath, "masks", ""));
        files = dir.listFiles();
        for (File file : files) {
            String filePath = file.getPath();
            String[] fileName = file.getName().split("_");
            String fileType = fileName[0];

            if (fileType.equals("mask")) {
                // If mask get the entire area.
                imp = new ImagePlus(filePath);
                data.add(file.getName() + "=" + imp.getStatistics(Measurements.AREA).area);
                imp.close();

            } else if (fileType.equals("sublayer")) {
                // If sublayer, combine with masks first.
                ImagePlus sublayer = new ImagePlus(filePath);
                ImagePlus myo = new ImagePlus(Filer.getPath(basePath, "masks", "mask_myo.tif"));
                ImagePlus endo = new ImagePlus(Filer.getPath(basePath, "masks", "mask_endo.tif"));
                myo = Masks.and(sublayer, myo);
                data.add(file.getName() + "(myo)=" + myo.getStatistics(Measurements.AREA).area);
                endo = Masks.and(sublayer, endo);
                data.add(file.getName() + "(endo)=" + endo.getStatistics(Measurements.AREA).area);
                sublayer.close();
                myo.close();
                endo.close();
            }
        }

        // Writing data.
        Path dataPath = Paths.get(Filer.getPath(basePath, "", "data.txt"));
        try {
            Files.write(dataPath, data);
        } catch (IOException e) {
            IJ.log(e.getMessage());
        }
    }
}