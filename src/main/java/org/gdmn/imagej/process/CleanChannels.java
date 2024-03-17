package org.gdmn.imagej.process;

import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import ij.plugin.ImageCalculator;
import ij.process.ImageConverter;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.gdmn.imagej.utils.Filer;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;

/**
 * Command to split fluorescence image into distinct channels with minimal
 * crosstalk.
 */
@Plugin(type = Command.class, menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Clean Channels", weight = 1)
})
public class CleanChannels extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Clean channels</h2>";

    @Parameter(label = "Channel 1", persist = false, choices = { "myo", "endo", "marker", "nuclei", "-" })
    private String channel1 = Defaults.get("channel1", "myo");

    @Parameter(label = "Channel 2", persist = false, choices = { "myo", "endo", "marker", "nuclei", "-" })
    private String channel2 = Defaults.get("channel2", "endo");

    @Parameter(label = "Channel 3", persist = false, choices = { "myo", "endo", "marker", "nuclei", "-" })
    private String channel3 = Defaults.get("channel3", "marker");

    @Parameter(label = "Channel 4", persist = false, choices = { "myo", "endo", "marker", "nuclei", "-" })
    private String channel4 = Defaults.get("channel4", "nuclei");

    @Parameter(label = "Crosstalk suppression", persist = false, style = NumberWidget.SLIDER_STYLE, min = "0", max = "10", stepSize = "0.1")
    private double crosstalkSuppression = 1;

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    public void process(String basePath) {
        String[] channels = { this.channel1, this.channel2, this.channel3, this.channel4 };
        this.cleanChannels(basePath, "roi.tif", channels, this.crosstalkSuppression);
    }

    /**
     * Splits a fluorescence image into cleaned output channels.
     *
     * @param basePath             the path to the image folder.
     * @param roiName              the name of the image file.
     * @param channelNames         an array of the channel names.
     * @param crosstalkSuppression the degree of crosstalk suppression to use.
     */
    private void cleanChannels(String basePath, String roiName, String[] channelNames, double crosstalkSuppression) {

        // Opening image and converting to grayscale.
        String roiPath = Paths.get(basePath, roiName).toString();
        ImagePlus imp = new ImagePlus(roiPath);
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();

        // Splitting image into channels and getting channel indexes.
        ImagePlus[] imps = ChannelSplitter.split(imp);
        List<String> channels = new ArrayList<>(Arrays.asList(channelNames));
        final int myoIndex = channels.indexOf("myo");
        final int endoIndex = channels.indexOf("endo");
        final int markerIndex = channels.indexOf("marker");
        final int nucleiIndex = channels.indexOf("nuclei");
        imp.close();

        // Assigning channels to ImagePlus objects.
        ImagePlus myoImp = (myoIndex >= 0) ? imps[myoIndex] : null;
        ImagePlus endoImp = (endoIndex >= 0) ? imps[endoIndex] : null;
        ImagePlus markerImp = (markerIndex >= 0) ? imps[markerIndex] : null;
        ImagePlus nucleiImp = (nucleiIndex >= 0) ? imps[nucleiIndex] : null;

        // Cleaning nuclear channel.
        if (nucleiImp != null) {
            if (myoImp != null) {
                nucleiImp = passClean(nucleiImp, myoImp);
            }
            if (endoImp != null) {
                nucleiImp = passClean(nucleiImp, endoImp);
            }
            Filer.save(nucleiImp, basePath, "channels", "nuclei.tif");
            nucleiImp.close();
        }

        // Cleaning myo and endo channels.
        if (myoImp != null && endoImp != null) {
            ImagePlus endoSuppressed = endoImp.duplicate();
            endoSuppressed.getProcessor().multiply(crosstalkSuppression);
            ImagePlus cleanedMyo = ImageCalculator.run(myoImp, endoSuppressed, "subtract create");
            Filer.save(cleanedMyo, basePath, "channels", "myo.tif");
            endoSuppressed.close();
            cleanedMyo.close();
            ImagePlus cleanedEndo = ImageCalculator.run(endoImp, myoImp, "subtract create");
            Filer.save(cleanedEndo, basePath, "channels", "endo.tif");
            myoImp.close();
            endoImp.close();
            cleanedEndo.close();
        }

        // Saving marker channel.
        if (markerImp != null) {
            Filer.save(markerImp, basePath, "channels", "marker.tif");
            markerImp.close();
        }
    }

    /**
     * Cleans crosstalk from an image by performing a double-pass clean.
     *
     * @param baseImage the image to be cleaned.
     * @param passImage the image to filter crosstalk from.
     * @return the cleaned image.
     */
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
