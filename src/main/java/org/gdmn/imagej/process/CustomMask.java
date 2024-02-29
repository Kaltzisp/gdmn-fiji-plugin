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
import ij.gui.Toolbar;
import ij.gui.WaitForUserDialog;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;

@Plugin(type = Command.class, menuPath = "2D Macro Tool>Custom Mask")
public class CustomMask extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Create custom mask</h2>";

    @Parameter(label = "Template type", persist = false, choices = { "Entire ROI", "Channel", "Mask" })
    private String templateType = Defaults.get("templateType", "Mask");

    @Parameter(label = "Template tissue", persist = false, choices = { "myo", "endo", "marker", "nuclei", "-" })
    private String templateTissue = Defaults.get("templateTissue", "myo");

    @Parameter(label = "Output mask name", persist = false)
    private String outputMask = Defaults.get("outputMask", "mask_myo_compact.tif");

    @Parameter(label = "Generate trabecular mask", persist = false)
    private Boolean createTrabecularMask = Boolean.parseBoolean(Defaults.get("createTrabecularMask", "true"));

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void process(String roiPath) {
        String filePath = roiPath;
        if (this.templateType.equals("Channel") && templateTissue != "-") {
            filePath = Filer.getPath(roiPath, "channels", this.templateTissue + ".tif");
        } else if (this.templateType.equals("Mask") && templateTissue != "-") {
            filePath = Filer.getPath(roiPath, "masks", "mask_" + this.templateTissue + ".tif");
        }

        // Opening drawing template and requesting drawing.
        ImagePlus imp = new ImagePlus(filePath);
        imp.killRoi();
        imp.show();
        IJ.setTool(Toolbar.POLYGON);
        WaitForUserDialog dialog = new WaitForUserDialog("Draw mask", "Select mask then hit OK.");
        dialog.show();

        ImagePlus mask;
        ImageProcessor maskIp;

        if (!templateTissue.equals("-")) {
            // If template tissue, combine template with selection.
            mask = new ImagePlus(Filer.getPath(roiPath, "masks", "mask_" + templateTissue + ".tif"));
            Roi roi = imp.getRoi();
            imp.close();
            maskIp = mask.getProcessor();
            mask.setRoi(roi);
            maskIp.setColor(0);
            maskIp.fillOutside(roi);
        } else {
            // If no template tissue, save the entire selection.
            mask = new ImagePlus("Mask", imp.createRoiMask());
            imp.close();
            maskIp = mask.getProcessor();
        }

        // Redrawing ROI.
        maskIp.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
        mask.setRoi(ThresholdToSelection.run(mask));

        // Saving and closing.
        Filer.save(mask, roiPath, "masks", outputMask);

        if (createTrabecularMask) {
            Roi roi = mask.getRoi();
            ImagePlus trabecularMask = new ImagePlus(
                    Filer.getPath(roiPath, "masks", "mask_" + templateTissue + ".tif"));
            ImageProcessor trabIp = trabecularMask.getProcessor();
            trabIp.fill(roi);
            trabIp.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
            trabecularMask.setRoi(ThresholdToSelection.run(trabecularMask));
            Filer.save(trabecularMask, roiPath, "masks", "mask_myo_trabecular.tif");
            trabecularMask.close();
        }

        mask.close();

    }
}