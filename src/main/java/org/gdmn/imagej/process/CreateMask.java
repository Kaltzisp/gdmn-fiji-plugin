package org.gdmn.imagej.process;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.RankFilters;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
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
 * Command to create light-based masks from channels.
 */
@Plugin(type = Command.class, label = "Create Masks", menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Create Masks", weight = 2)
})
public class CreateMask extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Create tissue masks</h2>";

    @Parameter(label = "Channel", choices = { "myo", "endo", "marker", "nuclei" })
    private String channelType = Defaults.get("channelType", "myo");

    @Parameter(label = "Multiplier:")
    private double multiplier = Double.parseDouble(Defaults.get("multiplier", "2"));

    @Parameter(label = "Median radius:")
    private double medianRadius = Double.parseDouble(Defaults.get("medianRadius", "6"));

    @Parameter(label = "Closing radius:")
    private int closingRadius = Integer.parseInt(Defaults.get("closingRadius", "2"));

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void process(String basePath) {
        this.createMask(basePath, this.channelType, this.multiplier, this.medianRadius, this.closingRadius);
    }

    /**
     * Creates a mask from a fluorescence image (single-channel).
     *
     * @param basePath      the path to the image folder.
     * @param channel       the name of the channel to create a mask from.
     * @param multiplier    the degree of amplification.
     * @param meadianRadius the radius to apply for the median filter.
     * @param closingRadius the radius to apply for the closing filter.
     */
    private void createMask(String basePath, String channel, double multiplier, double medianRadius,
            int closingRadius) {

        // Opening image and getting processor.
        String imagePath = Filer.getPath(basePath, "channels", channel + ".tif");
        ImagePlus imp = new ImagePlus(imagePath);
        ImageProcessor ip = imp.getProcessor();

        // Applying transforms.
        ip.multiply(multiplier);
        new RankFilters().rank(ip, medianRadius, RankFilters.MEDIAN);
        ip = Morphology.closing(ip, Strel.Shape.DISK.fromRadius(closingRadius));
        ip.autoThreshold();
        imp.setProcessor(ip);

        // Setting threshold and creating selection.
        ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
        Roi roi = ThresholdToSelection.run(imp);
        imp.setRoi(roi);

        // Saving mask.
        Filer.save(imp, basePath, "masks", "mask_" + channel + ".tif");
        imp.close();
    }

}
