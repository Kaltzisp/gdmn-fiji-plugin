package org.gdmn.imagej.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.gdmn.imagej.utils.Filer;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;

import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import ij.plugin.ImageCalculator;
import ij.process.ImageConverter;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;

@Plugin(type = Command.class, menuPath = "2D Macro Tool>Clean Channels")
public class CleanChannels extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String maskMessage = "<h2 style='width: 500px'>Clean channels</h2>";

    @Parameter(label = "Channel 1", choices = {"myo", "endo", "marker", "nuclei", "-"})
    private String channel1 = Defaults.get("channel1", "myo");

    @Parameter(label = "Channel 2", choices = {"myo", "endo", "marker", "nuclei", "-"})
    private String channel2 = Defaults.get("channel2", "endo");

    @Parameter(label = "Channel 3", choices = {"myo", "endo", "marker", "nuclei", "-"})
    private String channel3 = Defaults.get("channel3", "marker");

    @Parameter(label = "Channel 4", choices = {"myo", "endo", "marker", "nuclei", "-"})
    private String channel4 = Defaults.get("channel4", "nuclei");

    @Parameter(label = "Crosstalk suppression", style = NumberWidget.SLIDER_STYLE, min = "0", max = "10", stepSize = "0.1")
    private double crosstalkSuppression = 1;

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void process(String roiPath) {

        ImagePlus imp = new ImagePlus(roiPath);

        // Converting to grayscale.
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();

        // Declarations.
        ImagePlus myoImp = null;
        ImagePlus endoImp = null;
        ImagePlus markerImp = null;
        ImagePlus nucleiImp = null;

        // Splitting image.
        ImagePlus[] imps = ChannelSplitter.split(imp);
        List<String> channels = new ArrayList<>(Arrays.asList(channel1, channel2, channel3, channel4));
        int myoIndex = channels.indexOf("myo");
        int endoIndex = channels.indexOf("endo");
        int markerIndex = channels.indexOf("marker");
        int nucleiIndex = channels.indexOf("nuclei");
        imp.close();

        // Assigning channels.
        if (myoIndex >= 0) {
            myoImp = imps[myoIndex];
        }
        if (endoIndex >= 0) {
            endoImp = imps[endoIndex];
        }
        if (markerIndex >= 0) {
            markerImp = imps[markerIndex];
        }
        if (nucleiIndex >= 0) {
            nucleiImp = imps[nucleiIndex];
        }

        // Cleaning nuclear channel.
        if (nucleiImp != null) {
            if (myoImp != null) {
                nucleiImp = passClean(nucleiImp, myoImp);
            }
            if (endoImp != null) {
                nucleiImp = passClean(nucleiImp, endoImp);
            }
            Filer.save(nucleiImp, roiPath, "channels", "nuclei.tif");
            nucleiImp.close();
        }

        // Cleaning myo and endo channels.
        if (myoImp != null && endoImp != null) {
            ImagePlus endoSuppressed = endoImp.duplicate();
            endoSuppressed.getProcessor().multiply(crosstalkSuppression);
            ImagePlus cleanedMyo = ImageCalculator.run(myoImp, endoSuppressed, "subtract create");
            Filer.save(cleanedMyo, roiPath, "channels", "myo.tif");
            endoSuppressed.close();
            cleanedMyo.close();
            ImagePlus cleanedEndo = ImageCalculator.run(endoImp, myoImp, "subtract create");
            Filer.save(cleanedEndo, roiPath, "channels", "endo.tif");
            myoImp.close();
            endoImp.close();
            cleanedEndo.close();
        }

        // Saving marker channel.
        if (markerImp != null) {
            Filer.save(markerImp, roiPath, "channels", "marker.tif");
            markerImp.close();
        }
    }

    private ImagePlus passClean(ImagePlus baseImage, ImagePlus passImage) {
        ImagePlus dilationImage = baseImage.duplicate();
        dilationImage.setProcessor(Morphology.dilation(dilationImage.getProcessor(), Strel.Shape.DISK.fromRadius(2)));
        ImagePlus dulledPasser = ImageCalculator.run(passImage, dilationImage, "subtract create");
        dulledPasser.getProcessor().multiply(0.5);
        ImagePlus cleanedImage = ImageCalculator.run(baseImage, dulledPasser, "subtract");
        dilationImage.close();
        dulledPasser.close();
        return cleanedImage;
    }

}
