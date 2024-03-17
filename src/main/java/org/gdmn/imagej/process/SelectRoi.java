package org.gdmn.imagej.process;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.gui.WaitForUserDialog;
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
        @Menu(label = "Select Roi", weight = 0)
})
public class SelectRoi extends BatchCommand {
    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Select ROI from image:</h2>";

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
        ImagePlus imp = new ImagePlus(this.filePath);
        imp.show();
        IJ.setTool(Toolbar.POLYGON);
        WaitForUserDialog dialog = new WaitForUserDialog("Draw ROI", "Select ROI then hit OK.");
        dialog.show();
        ImageProcessor ip = imp.getProcessor();
        Roi roi = imp.getRoi();
        ip.setColor(0);
        ip.fillOutside(roi);
        Filer.save(imp, basePath, "", "roi.tif");
        imp.close();
    }

}