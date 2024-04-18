package org.gdmn.imagej.utils;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/** Displays some helpful info to the user. */
@Plugin(type = Command.class, menu = {
        @Menu(label = "2D Macro Tool"),
        @Menu(label = "Help", weight = 100)
})
public class Help implements Command, Interactive {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String header = "<h2 style='width: 500px'>Help and FAQ:</h2>";

    @Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
    private String info = "<div style='width: 500px;'>"

            + "<b style='color: red;'>When trying to run the plugin I get an error: <i> Exception in thread ... "
            + "java.lang.NoClassDefFoundError?</i></b><br>"
            + "Ensure that you have the following plugins installed (Fiji Menu > Help > Update > Manage Update Sites):<br>"
            + "<ul><li>CSBDeep</li><li>IPJB-Plugins</li><li>StarDist</li></ul><br><br>"

            + "<b style='color: red;'>The file label_endo.tif includes both endocardial and epicardial nuclei?</b><br>"
            + "As there is no specific epicardial staining, the plugin is not able to automatically differentiate endocardial and epicardial nuclei "
            + "(and also coronaries, etc.). You must instead use <i>Draw Custom Mask</i> to create a mask that covers all of the epicardial nuclei, "
            + "and then use <i>Segment Label</i> to split the original endocardial label into two new labels: (1) an epicardial label, and (2) a cleaned "
            + "endocardial label. You can use this same workflow to segment other non-specifically stained data.<br><br>"

            + "<b style='color: red;'>Many blood cells are being identified as nuclei; how can I remove them from my analysis?</b><br>"
            + "See above Q; first create a custom mask that covers all the blood cells, and then use <i>Segment Label</i> to filter out the noisy data."

            + "</div>";

    public void run() {
        // Empty method - command is not runnable via this method.
    }

}
