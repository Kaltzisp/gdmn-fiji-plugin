package org.gdmn.imagej.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ij.IJ;

public class Defaults {
    static Path defaultsPath = Paths.get(ij.Menus.getPlugInsPath(), "GdMN Plugin", ".defaults");

    public static String get(String key, String fallback) {
        if (Defaults.hasDefaultsFile()) {
            try {
                List<String> lines = Files.readAllLines(defaultsPath);
                for (String store : lines) {
                    if (key.equals(store.split("=")[0])) {
                        return store.split("=")[1];
                    }
                }
            } catch (IOException e) {
                IJ.log(e.getMessage());
            }
        }
        return fallback;
    }

    public static void set(String key, String value) {
        if (!Defaults.hasDefaultsFile()) {
            try {
                Files.write(defaultsPath, Collections.singleton(key + "=" + value));
            } catch (IOException e) {
                IJ.log(e.getMessage());
            }
        } else {
            try {
                List<String> lines = Files.readAllLines(defaultsPath);
                List<String> modifiedLines = new ArrayList<>();
                Boolean stored = false;
                for (String store : lines) {
                    if (key.equals(store.split("=")[0])) {
                        store = key + "=" + value;
                        stored = true;
                    }
                    modifiedLines.add(store);
                }
                if (!stored && value.charAt(0) != '<') {
                    modifiedLines.add(key + "=" + value);
                }
                Files.write(defaultsPath, modifiedLines);
            } catch (IOException e) {
                IJ.log(e.getMessage());
            }
        }
    }

    private static boolean hasDefaultsFile() {
        File f = new File(defaultsPath.toUri());
        return (f.exists() && !f.isDirectory());
    }

}
