package org.gdmn.imagej.process;

import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.gdmn.imagej.utils.Filer;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Menu;
import org.scijava.ItemVisibility;
import org.scijava.widget.Button;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.RankFilters;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;

import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;

@Plugin(type = Command.class, label = "Create Masks", menu = {
    @Menu(label = "2D Macro Tool"),
    @Menu(label = "Create Masks", weight = 2)
})
public class CreateMask extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Create tissue masks</h2>";

    @Parameter(label = "Channel", choices = {"myo", "endo", "marker", "nuclei"})
    private String channelType = Defaults.get("channelType", "myo");

    @Parameter(label = "Multiplier:")
    private float multiplier = Float.parseFloat(Defaults.get("multiplier", "2"));

    @Parameter(label = "Median radius:")
    private float medianRadius = Float.parseFloat(Defaults.get("medianRadius", "6"));

    @Parameter(label = "Closing radius:")
    private int closingRadius = Integer.parseInt(Defaults.get("closingRadius", "2"));

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void process(String roiPath) {
        String filePath = Filer.getPath(roiPath, "channels", this.channelType + ".tif");
        ImagePlus imp = new ImagePlus(filePath);
        ImageProcessor ip = imp.getProcessor();

        // Creating mask.
        ip.multiply(this.multiplier);
        new RankFilters().rank(ip, this.medianRadius, RankFilters.MEDIAN);
        ip = Morphology.closing(ip, Strel.Shape.DISK.fromRadius(this.closingRadius));
        ip.autoThreshold();
        imp.setProcessor(ip);

        // Drawing selection.
        ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
        Roi roi = ThresholdToSelection.run(imp);
        imp.setRoi(roi);

        // Saving mask.
        Filer.save(imp, roiPath, "masks", "mask_" + this.channelType + ".tif");
        imp.close();
    }

}
