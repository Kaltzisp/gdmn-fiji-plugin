package org.gdmn.imagej.debug;

import ij.IJ;
import ij.plugin.PlugIn;

public class HelloWorld implements PlugIn {
    public void run(String arg) {
        IJ.log("Hello World!");
    }
}
