package org.gdmn.imagej.process;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.gui.WaitForUserDialog;
import ij.plugin.Duplicator;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Filer;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

/**
 * Command to select ROI from an image file.
 */
@Plugin(type = Command.class, menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Select Roi", weight = 20)
})
public class SelectRoi extends BatchCommand {
    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Select ROI from image:</h2>";

    @Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
    private String info = "<p style='width: 500px;'>"
            + "Prompts selection of a ROI for each image. "
            + "For images with multiple z-slices, an option is given to use the max-projection of all slices. "
            + "If max-projection is set to false, the ROI will instead be saved from the active z-slice.";

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void process(String basePath) {
        selectRoi(basePath);
    }

    /**
     * Prompts the user to select a roi and saves to roi.tif.
     *
     * @param basePath the path to the image folder.
     */
    public void selectRoi(String basePath) {

        // Opening image and requesting drawing.
        ImagePlus imp = new ImagePlus(this.filePath);
        imp.setOpenAsHyperStack(true);
        imp.show();
        IJ.setTool(Toolbar.POLYGON);

        // If multiple z-slices then check whether to use max-projection.
        boolean maxProjection = false;
        if (imp.getNSlices() > 1) {
            NonBlockingGenericDialog dialog = new NonBlockingGenericDialog("Draw ROI");
            dialog.addMessage("Select ROI then hit OK.");
            dialog.addCheckbox("Use max projection", false);
            dialog.showDialog();
            maxProjection = dialog.getNextBoolean();
        } else {
            WaitForUserDialog dialog = new WaitForUserDialog("Draw ROI", "Select ROI then hit OK.");
            dialog.show();
        }

        // Getting ROI and hiding image.
        final Roi roi = imp.getRoi();
        imp.hide();
        imp.killRoi();

        // Applying max projection if necessary and duplicating image.
        if (maxProjection) {
            imp = ZProjector.run(imp, "max");
        }
        imp = new Duplicator().run(imp, 1, imp.getNChannels(), imp.getSlice(), imp.getSlice(), 1, 1);

        // Setting roi and clearing outside.
        imp.setRoi(roi);
        for (int i = 0; i <= imp.getNChannels(); i++) {
            imp.setC(i);
            ImageProcessor ip = imp.getChannelProcessor();
            ip.setColor(0);
            ip.fillOutside(roi);
        }
        imp = imp.crop("stack");

        // Making image grayscale.
        imp.setDisplayMode(IJ.GRAYSCALE);

        // Saving and closing image.
        Filer.save(imp, basePath, "", "roi.tif");
        imp.close();
    }

}