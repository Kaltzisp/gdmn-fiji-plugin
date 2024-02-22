package org.gdmn.imagej.utils;

import ij.IJ;
import ij.plugin.PlugIn;

public class HelloWorld implements PlugIn {
    public void run(String arg) {
        IJ.log("Hello World!");
    }
}
