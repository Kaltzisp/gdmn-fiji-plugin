package org.gdmn.imagej.utils;

import ij.Prefs;

/**
 * Provides static methods to set and get parameter defaults.
 */
public class Defaults {
    private static String packageName = "fluoromap";

    public static String get(String key, String fallback) {
        return Prefs.get(packageName + "." + key, fallback);
    }

    public static void set(String key, Object value) {
        Prefs.set(packageName + "." + key, value.toString());
        Prefs.savePreferences();
    }

}
