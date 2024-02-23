package org.gdmn.imagej.utils;

import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.command.Previewable;

import org.scijava.plugin.Parameter;
import org.scijava.ItemVisibility;
import org.scijava.widget.Button;

import ij.IJ;
import ij.io.DirectoryChooser;

public class BatchCommand implements Command, Interactive, Previewable {
    private String defaultFilePattern = "roi.tif";
    private String defaultDir = org.gdmn.imagej.utils.Defaults.get("dir");
    private int nTargetFiles = FileFinder.count(this.defaultDir, this.defaultFilePattern);

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String fileChooserMessage = "<h2 style='width: 500px'>Select the parent directory and target file pattern.</h2>";

    @Parameter(label = "Parent directory:", callback = "updateCollectorInfo")
    private String selectedDir = this.defaultDir;

    @Parameter(label = "Browse...", callback = "selectDir")
    private Button selectDir;

    @Parameter(label = "File pattern:", callback = "updateCollectorInfo")
    private String filePattern = this.defaultFilePattern;

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String targetMessage = setTargetMessage();

    
    public void run() {
        IJ.log("Running");
    }

    public void preview() {
        IJ.log("Previewing");
    }

    public void cancel() {
        IJ.log("Cancelled");
    }

    public void updateCollectorInfo() {
        this.nTargetFiles = FileFinder.count(this.selectedDir, this.filePattern);
        setTargetMessage();
    }

    public void selectDir() {
        DirectoryChooser.setDefaultDirectory(this.selectedDir);
        DirectoryChooser dir = new DirectoryChooser("Select folder");
        String chosenDir = dir.getDirectory();
        if (chosenDir != null && chosenDir.length() != 0) {
            this.selectedDir = dir.getDirectory();
            Defaults.set("dir", this.selectedDir);
        }
    }

    public String setTargetMessage() {
        if (this.nTargetFiles > 1 ) {
            this.targetMessage = "<p style='color:#006600'>ImageJ has identified " + this.nTargetFiles + " images which match these parameters.</p>";
        } else if (this.nTargetFiles == 1) {
            this.targetMessage = "<p style='color:#006600'>ImageJ has identified " + this.nTargetFiles + " image which matches these parameters.</p>";
        } else if (this.nTargetFiles == 0) {
            this.targetMessage = "<p style='color:#bb0000'>No matching images found.</p>";
        } else {
            this.targetMessage = "<p style='color:#bb0000'>An error occurred.</p>";
        }
        return this.targetMessage;
    }

}
