package org.gdmn.imagej.process;

import java.util.List;

import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Filer;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import org.scijava.plugin.Parameter;
import org.scijava.ItemVisibility;
import org.scijava.widget.Button;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.RankFilters;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;

import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;

@Plugin(type = Command.class, menuPath = "2D Macro Tool>Create Masks")
public class Masks extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String maskMessage = "<h2 style='width: 500px'>Create tissue masks</h2>";

    @Parameter(label = "Multiplier:")
    private float multiplier = 4;

    @Parameter(label = "Median radius:")
    private float medianRadius = 20;

    @Parameter(label = "Closing radius:")
    private int closingRadius = 4;

    @Parameter(label = "Output filename")
    private String outputFile = "mask_myo.tif";

    @Parameter(label = "Run", callback = "execute")
    private Button runButton;

    public void execute() {
        List<String> filePaths = Filer.getFiles(this.selectedDir, this.filePattern);
        for (String filePath : filePaths) {
            IJ.showProgress(closingRadius);
            ImagePlus imp = new ImagePlus(filePath);
            ImageProcessor ip = imp.getProcessor();

            // Creating mask.
            ip.multiply(multiplier);
            new RankFilters().rank(ip, medianRadius, RankFilters.MEDIAN);
            ip = Morphology.closing(ip, Strel.Shape.DISK.fromRadius(closingRadius));
            ip.autoThreshold();
            imp.setProcessor(ip);

            // Drawing selection.
            ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
            Roi roi = ThresholdToSelection.run(imp);
            imp.setRoi(roi);

            // Saving mask.
            Filer.save(imp, filePath, "masks", outputFile);
            imp.close();
        }

    }

}
