package org.gdmn.imagej.process;

import ij.IJ;
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
@Plugin(type = Command.class, menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Create Masks", weight = 22)
})
public class CreateMask extends BatchCommand {
    private ImagePlus channelImp;
    private ImagePlus maskImp;

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Create tissue masks</h2>";

    @Parameter(label = "Channel", choices = { "myo", "endo", "marker", "nuclei" }, callback = "updatePreview")
    private String channelType = Defaults.get("channelType", "myo");

    @Parameter(label = "Multiplier:", callback = "updatePreview")
    private double multiplier = Double.parseDouble(Defaults.get("multiplier", "2"));

    @Parameter(label = "Median radius:", callback = "updatePreview")
    private double medianRadius = Double.parseDouble(Defaults.get("medianRadius", "6"));

    @Parameter(label = "Closing radius:", callback = "updatePreview")
    private int closingRadius = Integer.parseInt(Defaults.get("closingRadius", "2"));

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
            this.channelImp = new ImagePlus(Filer.getPath(previewDir, "channels", this.channelType + ".tif"));
            this.maskImp = new ImagePlus(Filer.getPath(previewDir, "channels", this.channelType + ".tif"));

            // Getting image dimensions.
            double screenWidth = IJ.getScreenSize().getWidth();
            double screenHeight = IJ.getScreenSize().getHeight();
            int impWidth = this.channelImp.getWidth();
            int impHeight = this.maskImp.getHeight();
            double scale = 1 / Math.max(2 * impWidth / screenWidth, 2 * impHeight / screenHeight);

            // Showing images.
            this.channelImp.show();
            this.channelImp.getWindow().setLocationAndSize((int) (screenWidth / 2 - impWidth * scale),
                    (int) (screenHeight / 2 - 100), (int) (impWidth * scale), (int) (impHeight * scale));
            this.maskImp.show();
            this.maskImp.getWindow().setLocationAndSize((int) (screenWidth / 2),
                    (int) (screenHeight / 2 - 100), (int) (impWidth * scale), (int) (impHeight * scale));
            updatePreview();

        } else {
            this.closePreview();
        }
    }

    private void closePreview() {
        if (this.channelImp != null) {
            this.channelImp.close();
        }
        if (this.maskImp != null) {
            this.maskImp.close();
        }
    }

    /**
     * Updates the preview image.
     */
    public void updatePreview() {
        if (this.channelImp != null && this.channelImp.isVisible() && this.maskImp.isVisible()) {
            String previewDir = Filer.getBasePaths(this.selectedDir, this.filePattern).get(0).getParent().toString();
            this.channelImp.setImage(new ImagePlus(Filer.getPath(previewDir, "channels", this.channelType + ".tif")));
            this.channelImp.killRoi();
            this.maskImp.killRoi();
            this.maskImp.setImage(this.channelImp);

            // Preview code.
            ImageProcessor ip = this.maskImp.getProcessor();

            // Applying transforms.
            ip.multiply(multiplier);
            new RankFilters().rank(ip, medianRadius, RankFilters.MEDIAN);
            ip = Morphology.closing(ip, Strel.Shape.DISK.fromRadius(closingRadius));
            ip.autoThreshold();
            this.maskImp.setProcessor(ip);

            // Setting threshold and creating selection.
            ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
            Roi roi = ThresholdToSelection.run(this.maskImp);
            this.maskImp.setRoi(roi);
            this.channelImp.setRoi(roi);
            this.maskImp.updateAndDraw();
        }
    }

    public void process(String basePath) {
        this.closePreview();
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
