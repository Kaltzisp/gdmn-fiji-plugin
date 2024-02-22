package org.gdmn.imagej.utils;

import ij.IJ;

public class HelloWorld implements ij.plugin.PlugIn {
    public void run(String arg) {
        IJ.log("Hello World!");
    }
}
