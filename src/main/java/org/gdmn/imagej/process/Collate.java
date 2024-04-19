package org.gdmn.imagej.process;

import ij.IJ;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.gdmn.imagej.utils.Filer;
import org.gdmn.imagej.utils.Logger;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

/**
 * Command to generate quantifications.
 */
@Plugin(type = Command.class, menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Collate Data", weight = 28)
})
public class Collate extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Collate data to CSV</h2>";

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    private String headers;
    private String output;

    /** Extracts and saves quantifications. */
    public void process(String basePath) {
        try {
            // Getting filepath array.
            String[] filePath = basePath.substring(this.selectedDir.length()).split("[\\\\/]");
            String title = "Type,Stage,Group,Embryo,Region,Image";

            // Reading content from data.txts.
            String content = new String(Files.readAllBytes(Paths.get(basePath, "data.txt")), StandardCharsets.UTF_8);
            String[] lines = content.split("\n");
            String headings = title + "," + String.join(",",
                    Arrays.stream(lines).map(line -> line.split("=")[0]).toArray(String[]::new));
            String data = String.join(",", filePath) + "," + String.join(",",
                    Arrays.stream(lines).map(line -> {
                        String l = line.split("=")[1];
                        return l.substring(0, l.length() - 1);
                    }).toArray(String[]::new));

            // If first file then update headings.
            if (this.output == null) {
                this.output = headings + "\n";
                this.headers = headings;
            }

            // If headings match, add to output.
            if (this.headers.equals(headings)) {
                this.output = this.output + data + "\n";
            } else {
                IJ.log("SKIPPED: different headings found for image at " + basePath);
                this.output = this.output + "SKIPPED: different headings found for image at " + basePath + "\n";
            }
        } catch (Exception e) {
            IJ.log(e.getMessage());
        }
    }

    @Override
    public void runAll() {
        Defaults.set("dir", this.selectedDir);
        Defaults.set("filePattern", this.filePattern);

        BatchCommand self = this;
        Logger.logProcess(self);

        List<Path> basePaths = new ArrayList<Path>();
        Filer.getBasePaths(this.selectedDir, this.filePattern)
                .forEach(path -> basePaths.add(path));
        int n = basePaths.size();
        for (int i = 0; i < n; i++) {
            IJ.showStatus("!Processing image " + (i + 1) + " of " + n + ".");
            String basePath = basePaths.get(i).getParent().toString();
            process(basePath);
        }
        try {
            Files.write(
                    Paths.get(this.selectedDir, "Data.csv"),
                    this.output.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

        } catch (Exception e) {
            IJ.log(e.getMessage());
        }
        IJ.showStatus("!Command finished: " + this.getClass().getSimpleName() + " on n=" + n + " images.");
    }

}