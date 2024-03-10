package org.gdmn.imagej.process;

import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.gdmn.imagej.utils.Filer;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.plugin.LutLoader;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

@Plugin(type = Command.class, menuPath = "2D Macro Tool>StarDist")
public class BatchStardist extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Batch StarDist</h2>";

    @Parameter(label = "Percentile low", persist = false, style = "slider, format:#.#   ", min = "0", max = "100", stepSize = "0.1")
    private Double percentileLow = Double.parseDouble(Defaults.get("percentileLow", "1.0"));

    @Parameter(label = "Percentile high", persist = false, style = "slider, format:#.#", min = "0", max = "100", stepSize = "0.1")
    private Double percentileHigh = Double.parseDouble(Defaults.get("percentileHigh", "99.8"));

    @Parameter(label = "Probability Threshold", persist = false, style = "slider, format:#.##", min = "0", max = "1", stepSize = "0.05")
    private Double probabilityThreshold = Double.parseDouble(Defaults.get("probabilityThreshold", "0.5"));

    @Parameter(label = "Overlap Threshold", persist = false, style = "slider, format:#.##", min = "0", max = "1", stepSize = "0.05")
    private Double overlapThreshold = Double.parseDouble(Defaults.get("overlapThreshold", "0.4"));

    @Parameter(label = "Area Threshold (µm^2)", persist = false, style = "slider", min = "0", max = "150", stepSize = "1")
    private int areaThreshold = Integer.parseInt(Defaults.get("areaThreshold", "10"));

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void process(String roiPath) {
        ImagePlus imp = new ImagePlus(Filer.getPath(roiPath, "channels", "nuclei.tif"));
        imp.show();

        // Running StarDist 2D.
        String[] args = {
                "'input':'" + imp.getTitle() + "'",
                "'modelChoice':'Versatile (fluorescent nuclei)'",
                "'normalizeInput':'true'",
                "'percentileBottom':'" + percentileLow.toString() + "'",
                "'percentileTop':'" + percentileHigh.toString() + "'",
                "'probThresh':'" + probabilityThreshold.toString() + "'",
                "'nmsThresh':'" + overlapThreshold.toString() + "'",
                "'outputType':'ROI Manager'",
                "'nTiles':'1'",
                "'excludeBoundary':'2'",
                "'roiPosition':'Automatic'",
                "'verbose':'false'",
                "'showCsbdeepProgress':'false'",
                "'showProbAndDist':'false'"
        };
        String argString = String.join(", ", args);
        IJ.runMacro("run(\"Command From Macro\", \"command=[de.csbdresden.stardist.StarDist2D], args=[" + argString
                + "], process=[false]\")");

        // Hiding image and roi manager.
        RoiManager roiManager = RoiManager.getInstance();
        roiManager.setVisible(false);
        imp.hide();

        // Post-processing.
        ImageProcessor ip = imp.getProcessor();
        ip.setColor(0);
        ip.fill();
        Roi[] rois = roiManager.getRoisAsArray();

        // Getting thresholded rois.
        for (int i = rois.length - 1; i >= 0; i--) {
            imp.setRoi(rois[i]);
            Double area = imp.getStatistics(Measurements.AREA).area;
            if (area < areaThreshold) {
                roiManager.select(i);
                roiManager.runCommand("Delete");
            }
        }

        // If more than 255 rois convert to 16 bit first.
        rois = roiManager.getRoisAsArray();
        if (rois.length > 255) {
            ImageConverter converter = new ImageConverter(imp);
            converter.convertToGray16();
        }

        // Re-filling rois.
        ip = imp.getProcessor();
        for (int i = 0; i < roiManager.getCount(); i++) {
            ip.setColor(i + 1);
            ip.fill(rois[i]);
        }

        // Resetting selection and applying LUT.
        imp.deleteRoi();
        ip.setColorModel(LutLoader.getLut("glasbey on dark"));

        // Saving and closing.
        Filer.save(imp, roiPath, "labels", "label_roi.tif");
        roiManager.runCommand("Save", Filer.getSavePath(roiPath, "zips", "zip_roi.zip"));
        roiManager.reset();
        roiManager.close();
        imp.close();

    }

}