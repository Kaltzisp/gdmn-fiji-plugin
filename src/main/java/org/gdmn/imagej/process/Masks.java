package org.gdmn.imagej.process;

import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Filer;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import org.scijava.plugin.Parameter;
import org.scijava.ItemVisibility;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;

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

    @Parameter(label = "Channel", style = NumberWidget.SLIDER_STYLE, min = "1", max = "4", stepSize = "1")
    private int channelIndex;

    @Parameter(label = "Multiplier:")
    private float multiplier = 2;

    @Parameter(label = "Median radius:")
    private float medianRadius = 6;

    @Parameter(label = "Closing radius:")
    private int closingRadius = 2;

    @Parameter(label = "Output filename")
    private String outputFile = "mask_myo.tif";

    @Parameter(label = "Run", callback = "execute")
    private Button runButton;

    public void process(String filePath) {
        ImagePlus imp = new ImagePlus(filePath);
        imp.setSlice(channelIndex);
        ImageProcessor ip = imp.getProcessor();

        // Creating mask.
        ip.multiply(multiplier);
        new RankFilters().rank(ip, medianRadius, RankFilters.MEDIAN);
        ip = Morphology.closing(ip, Strel.Shape.DISK.fromRadius(closingRadius));
        ip.autoThreshold();

        // Creating single channel image.
        ImagePlus slice = new ImagePlus("slice", ip);
        imp.close();

        // Drawing selection.
        ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
        Roi roi = ThresholdToSelection.run(slice);
        slice.setRoi(roi);

        // Saving mask.
        Filer.save(slice, filePath, "masks", outputFile);
        imp.close();
    }

}
