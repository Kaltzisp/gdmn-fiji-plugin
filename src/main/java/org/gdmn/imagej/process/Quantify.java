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
        @Menu(label = "Analyze Marker", weight = 7)
})
public class Quantify extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Generate quantifications</h2>";

    @Parameter(label = "Threshold:")
    private double markerthreshold = Double.parseDouble(Defaults.get("markerthreshold", "0.30"));

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    /**
     * Extracts and saves quantifications for an image folder and saves marker
     * labels.
     */
    public void process(String basePath) {
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
                    if (mean > markerthreshold * 255) {
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
                data.add(fileName + "=" + numberActive + "/" + rois.length);
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
            String fileType = file.getName().substring(0, 4);
            if (fileType.equals("mask")) {
                imp = new ImagePlus(filePath);
                data.add(file.getName() + "=" + imp.getStatistics(Measurements.AREA).area);
                imp.close();
            }
        }

        // Writing data.
        Path dataPath = Paths.get(Filer.getPath(basePath, "", "quant.txt"));
        try {
            Files.write(dataPath, data);
        } catch (IOException e) {
            IJ.log(e.getMessage());
        }
    }
}