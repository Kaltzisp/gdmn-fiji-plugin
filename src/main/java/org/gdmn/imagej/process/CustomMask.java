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
import ij.gui.Toolbar;
import ij.gui.WaitForUserDialog;

@Plugin(type = Command.class, menuPath = "2D Macro Tool>Custom Mask")
public class CustomMask extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Create custom mask</h2>";

    @Parameter(label = "Template type", choices = { "Entire ROI", "Channel",
            "Mask" }, callback = "updateTemplateTissues")
    private String templateType = Defaults.get("templateType", "Mask");

    @Parameter(label = "Template tissue", choices = { "myo", "endo", "marker", "nuclei", "-" })
    private String templateTissue = Defaults.get("templateTissue", "myo.tif");

    @Parameter(label = "Output mask name")
    private String outputMask = Defaults.get("outputMask", "mask_myo_compact.tif");

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void updateTemplateTissues() {
        if (this.templateType == "Entire ROI") {
            this.templateTissue = "-";
        }
    }

    public void process(String roiPath) {
        String filePath = roiPath;
        if (this.templateType == "Channel" && templateTissue != "-") {
            filePath = Filer.getPath(roiPath, "channels", this.templateTissue + ".tif");
        } else if (this.templateType == "Mask" && templateTissue != "-") {
            filePath = Filer.getPath(roiPath, "masks", "mask_" + this.templateTissue + ".tif");
        }

        // Opening template and requesting drawing.
        ImagePlus imp = new ImagePlus(filePath);
        imp.show();
        IJ.setTool(Toolbar.POLYGON);
        WaitForUserDialog dialog = new WaitForUserDialog("Draw mask", "Select mask then hit OK.");
        dialog.show();

        // Copying mask to new image.
        ImagePlus mask = new ImagePlus("Mask", imp.createRoiMask());
        Filer.save(mask, roiPath, "masks", outputMask);
        imp.close();
        mask.close();

    }

}