package org.gdmn.imagej.utils;

import ij.IJ;
import ij.io.DirectoryChooser;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import org.scijava.widget.Button;

/**
 * Overarching BatchCommand class extended by each of the plugin commands.
 */
public abstract class BatchCommand implements Command, Interactive {

    protected String filePath;

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

    private int numTargetFiles = Filer.getBasePaths(this.selectedDir, this.filePattern).size();

    @Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
    private String targetMessage = setTargetMessage();

    /**
     * Allows the user to choose a target directory for analysis.
     */
    public void selectDir() {
        DirectoryChooser.setDefaultDirectory(this.selectedDir);
        DirectoryChooser dir = new DirectoryChooser("Select folder");
        String chosenDir = dir.getDirectory();
        if (chosenDir != null && chosenDir.length() != 0) {
            this.selectedDir = dir.getDirectory();
            Defaults.set("dir", this.selectedDir);
        }
    }

    /**
     * Opens the target dir in the user's file explorer.
     */
    public void openDir() {
        Path dirPath = Paths.get(this.selectedDir);
        String[] array = { "explorer.exe", dirPath.toString() };
        try {
            Runtime.getRuntime().exec(array);
        } catch (IOException e) {
            IJ.log(e.getMessage());
        }
    }

    /**
     * Updates the number of files detected by the plugin in the UI.
     */
    public void updateCollectorInfo() {
        this.numTargetFiles = Filer.getBasePaths(this.selectedDir, this.filePattern).size();
        this.targetMessage = setTargetMessage();
    }

    private String setTargetMessage() {
        String message = "";
        if (this.numTargetFiles > 1) {
            message = "<p style='color:#006600'>ImageJ has identified " + this.numTargetFiles
                    + " images which match these parameters.</p>";
        } else if (this.numTargetFiles == 1) {
            message = "<p style='color:#006600'>ImageJ has identified " + this.numTargetFiles
                    + " image which matches these parameters.</p>";
        } else if (this.numTargetFiles == 0) {
            message = "<p style='color:#bb0000'>No matching images found.</p>";
        } else {
            message = "<p style='color:#bb0000'>An error occurred.</p>";
        }
        return message;
    }

    public abstract void process(String basePath);

    /**
     * Loops through the list of selected files and runs the command on each.
     */
    public void runAll() {

        // Specifying self as the command instance.
        BatchCommand self = this;
        Logger.logProcess(self);

        // Getting target dir and files.
        List<Path> basePaths = new ArrayList<Path>();
        Filer.getBasePaths(self.selectedDir, self.filePattern)
                .forEach(path -> basePaths.add(path));
        int n = basePaths.size();

        // Creating a new thread to process the images.
        Thread runThread = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < n; i++) {
                    IJ.showStatus("!Processing image " + (i + 1) + " of " + n + ".");
                    self.filePath = basePaths.get(i).toString();
                    self.process(basePaths.get(i).getParent().toString());
                }
                IJ.showStatus("!Command finished: " + self.getClass().getSimpleName() + " on n=" + n + " images.");
            }
        });
        runThread.start();
    }

    public void run() {
        // Empty method - command is not runnable via this method.
    }

}
