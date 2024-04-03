package org.gdmn.imagej.process;

import ij.IJ;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.gdmn.imagej.utils.BatchCommand;
import org.gdmn.imagej.utils.Defaults;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

/**
 * Command to split fluorescence image into distinct channels with minimal
 * crosstalk.
 */
@Plugin(type = Command.class, menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Create Folders", weight = 1)
})
public class CreateFolders extends BatchCommand {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Create folder hierarchy</h2>";

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String info = "Creates a folder hierarchy from a set of image files. "
            + "Images should all be within the same folder and named in the style below:<br><br>"
            + "<b>IMAGING_INFO-STAGE-TEST_GROUP-EMBRYO_NUMBER-REGION-SECTION.tif</b><br>"
            + "PK_IF5_Batch_1-E11.5-KLF8_WT-Embryo_32-LV-001.tif";

    @Parameter(label = "Delimiter")
    private String delimiter = Defaults.get("delimiter", "-");

    @Parameter(label = "Run", callback = "runAll")
    private Button runButton;

    /**
     * Builds a folder hierarchy based on the images present.
     */
    public void process(String basePath) {

        // Getting delimited URI and building path.
        String currentDir = "";
        String filePath = this.filePath.substring(0, this.filePath.lastIndexOf("."));
        String[] dirPath = filePath.split(delimiter);
        for (int i = 0; i < dirPath.length; i++) {
            currentDir = Paths.get(currentDir, dirPath[i]).toString();
            File dir = new File(currentDir);
            if (!dir.exists()) {
                dir.mkdir();
            }
        }

        // Copying files and deleting.
        try {
            Files.copy(Paths.get(this.filePath), Paths.get(currentDir, "Image.tif"));
            Files.delete(Paths.get(this.filePath));
        } catch (Exception e) {
            IJ.log(e.getMessage());
        }
    }
}
