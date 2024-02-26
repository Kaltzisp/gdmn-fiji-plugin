package org.gdmn.imagej.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Defaults {
    static Path defaultsPath = Paths.get(ij.Menus.getPlugInsPath(), "GdMN Plugin", ".defaults");

    public static String get(String key) {
        if (!Defaults.hasDefaultsFile()) {
            return "";
        }
        try {
            List<String> lines = Files.readAllLines(defaultsPath);
            for (String store : lines) {
                if (key.equals(store.split("=")[0])) {
                    return store.split("=")[1];
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return "";
    }

    public static void set(String key, String value) {
        if (!Defaults.hasDefaultsFile()) {
            try {
                Files.write(defaultsPath, Collections.singleton(key + "=" + value));
            } catch (IOException e) {
                System.err.println(e);
            }
        } else {
            try {
                List<String> lines = Files.readAllLines(defaultsPath);
                List<String> modifiedLines = new ArrayList<>();
                for (String store : lines) {
                    if (key.equals(store.split("=")[0])) {
                        store = key + "=" + value;
                    }
                    modifiedLines.add(store);
                }
                Files.write(defaultsPath, modifiedLines);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean hasDefaultsFile() {
        File f = new File(defaultsPath.toUri());
        return (f.exists() && !f.isDirectory());
    }

}
