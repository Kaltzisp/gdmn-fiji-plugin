package org.gdmn.imagej.utils;

import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.scijava.ItemVisibility;
import org.scijava.widget.Button;

import ij.IJ;
import ij.io.DirectoryChooser;

public abstract class BatchCommand implements Command, Interactive {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String fileChooserMessage = "<h2 style='width: 500px'>Select the parent directory and target file pattern.</h2>";

    @Parameter(label = "Parent directory:", persist = false, callback = "updateCollectorInfo")
    public String selectedDir = Defaults.get("dir", "");

    @Parameter(label = "Browse...", callback = "selectDir")
    private Button selectDir;

    @Parameter(label = "Open in File Explorer", callback = "openDir")
    private Button openDir;

    @Parameter(label = "File pattern:", persist = false, callback = "updateCollectorInfo")
    public String filePattern = Defaults.get("filePattern", "roi.tif");
    
    private int nTargetFiles = Filer.getFiles(this.selectedDir, this.filePattern).size();

    @Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
    private String targetMessage = setTargetMessage();

    public void selectDir() {
        DirectoryChooser.setDefaultDirectory(this.selectedDir);
        DirectoryChooser dir = new DirectoryChooser("Select folder");
        String chosenDir = dir.getDirectory();
        if (chosenDir != null && chosenDir.length() != 0) {
            this.selectedDir = dir.getDirectory();
            Defaults.set("dir", this.selectedDir);
        }
    }

    public void openDir() {
        Path dirPath = Paths.get(this.selectedDir);
        String[] array = { "explorer.exe", dirPath.toString() };
        try {
            Runtime.getRuntime().exec(array);
        } catch (IOException e) {
            IJ.log(e.getMessage());
        }
    }

    public void updateCollectorInfo() {
        this.nTargetFiles = Filer.getFiles(this.selectedDir, this.filePattern).size();
        this.targetMessage = setTargetMessage();
    }

    private String setTargetMessage() {
        String message = "";
        if (this.nTargetFiles > 1) {
            message = "<p style='color:#006600'>ImageJ has identified " + this.nTargetFiles
                    + " images which match these parameters.</p>";
        } else if (this.nTargetFiles == 1) {
            message = "<p style='color:#006600'>ImageJ has identified " + this.nTargetFiles
                    + " image which matches these parameters.</p>";
        } else if (this.nTargetFiles == 0) {
            message = "<p style='color:#bb0000'>No matching images found.</p>";
        } else {
            message = "<p style='color:#bb0000'>An error occurred.</p>";
        }
        return message;
    }

    public abstract void process(String roiPath);

    public void runAll() {
        BatchCommand self = this;
        Logger.logProcess(this);
        List<String> roiPaths = Filer.getFiles(this.selectedDir, this.filePattern);
        int n = roiPaths.size();
        Thread runThread = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < n; i++) {
                    IJ.showStatus("!Processing image " + (i + 1) + " of " + n + ".");
                    self.process(roiPaths.get(i));
                }
                IJ.showStatus("!Command finished: " + self.getClass().getSimpleName() + " on n=" + n + " images.");
            }
        });
        runThread.start();
    }

    public void run() {
        // Do nothing.
    }

    public void preview() {
        // Do nothing.
    }

    public void cancel() {
        // Do nothing.
    }

}
