package org.gdmn.imagej.process;

import java.util.List;

import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.FileFinder;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import org.scijava.plugin.Parameter;
import org.scijava.ItemVisibility;
import org.scijava.widget.Button;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;

import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;

@Plugin(type = Command.class, menuPath = "2D Macro Tool>Create Masks")
public class Masks extends BatchCommand {
    
    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String maskMessage = "<h2 style='width: 500px'>Create tissue masks</h2>";

    @Parameter(label = "Multiplier:")
    private float multiplier = 4;

    @Parameter(label = "Median Radius:")
    private float medianRadius = 20;

    @Parameter(label = "Closing Radius:")
    private int closingRadius = 4;

    @Parameter(label = "Run", callback = "execute")
    private Button runButton;

    public void execute() {
        List<String> filePaths = FileFinder.getFiles(this.selectedDir, this.filePattern);
        for (String filePath : filePaths) {
            ImagePlus imp = new ImagePlus(filePath);
            ImageProcessor ip = imp.getProcessor();
            ip.multiply(multiplier);
            new RankFilters().rank(ip, medianRadius, RankFilters.MEDIAN);
            ImageProcessor resultIP = Morphology.closing(ip, Strel.Shape.DISK.fromRadius(closingRadius));
            imp = new ImagePlus("Result", resultIP);
            imp.show();
        }

    }


}

// // Getting input.
// input = split(getArgument(), ",");
// path = input[0];
// type = input[1];
// multiplier = parseFloat(input[2]);
// radius = parseFloat(input[3]);
// closer = parseFloat(input[4]);
// output = input[5];

// // Creating mask.
// open(path+"channels/"+type+".tif");
// run("Multiply...", "value="+multiplier);
// run("Median...", "radius="+radius);
// run("Morphological Filters", "operation=Closing element=Square radius="+closer);
// run("Convert to Mask");
// run("Create Selection");
// save(path+"masks/mask_"+output+".tif");
// close(type+".tif");
// close(type+"-Closing");