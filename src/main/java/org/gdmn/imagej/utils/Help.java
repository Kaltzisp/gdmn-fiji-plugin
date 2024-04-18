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
    private String header = "<h2 style='width: 500px'>Help and Common Errors:</h2>";

    @Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
    private String info = "<p style='width: 500px;'>"
            + "<b style='color: red;'>Exception in thread ... java.lang.NoClassDefFoundError: inra/ipjb/morphology/Strel$Shape</b><br>"
            + "Fix: Add IPJB-Plugins and StarDist to the list of installed Fiji plugins (Fiji Menu>Help>Update>Manage Update Sites)";

    public void run() {
        // Empty method - command is not runnable via this method.
    }

}
