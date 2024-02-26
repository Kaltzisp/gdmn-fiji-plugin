package org.gdmn.imagej.utils;

import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.command.Previewable;

import org.scijava.plugin.Parameter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.scijava.ItemVisibility;
import org.scijava.widget.Button;

import ij.IJ;
import ij.io.DirectoryChooser;

public class BatchCommand implements Command, Interactive, Previewable {
    private String defaultFilePattern = "roi.tif";
    private String defaultDir = org.gdmn.imagej.utils.Defaults.get("dir");
    private int nTargetFiles = Filer.getFiles(this.defaultDir, this.defaultFilePattern).size();

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String fileChooserMessage = "<h2 style='width: 500px'>Select the parent directory and target file pattern.</h2>";

    @Parameter(label = "Parent directory:", callback = "updateCollectorInfo")
    public String selectedDir = this.defaultDir;

    @Parameter(label = "Browse...", callback = "selectDir")
    private Button selectDir;

    @Parameter(label = "Open in File Explorer", callback = "openDir")
    private Button openDir;

    @Parameter(label = "File pattern:", callback = "updateCollectorInfo")
    public String filePattern = this.defaultFilePattern;

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String targetMessage = setTargetMessage();

    
    public void run() {
        // Do nothing.
    }

    public void preview() {
        // Do Nothing.
    }

    public void cancel() {
        IJ.log("Operation cancelled.");
    }

    public void updateCollectorInfo() {
        this.nTargetFiles = Filer.getFiles(this.selectedDir, this.filePattern).size();
        setTargetMessage();
    }

    public void openDir() {
        Path dirPath = Paths.get(selectedDir);
        String[] array = {"explorer.exe", dirPath.toString()};
        try {
            Runtime.getRuntime().exec(array);
        } catch(IOException e) {
            IJ.log(e.getMessage());
        }
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

    private String setTargetMessage() {
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
