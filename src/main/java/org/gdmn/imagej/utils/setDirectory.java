package org.gdmn.imagej.utils;

import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;

public class setDirectory implements PlugIn {

    public void run(String arg) {
        // Getting default directory.
        String defaultPath = Defaults.get("dir");
        DirectoryChooser.setDefaultDirectory(defaultPath);

        // Opening new directory.
        DirectoryChooser dir = new DirectoryChooser("Choose a top-level directory for batch processing.");
        Defaults.set("dir", dir.getDirectory());
    }

}
