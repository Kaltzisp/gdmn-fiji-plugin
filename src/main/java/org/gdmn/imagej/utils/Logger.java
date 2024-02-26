package org.gdmn.imagej.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ij.IJ;

public class Logger {
    public static void logProcess(BatchCommand instance) {

        // Setting up log text.
        String logText = instance.getClass().getName() + "\n";
        logText += "runDirectory=" + instance.selectedDir + "\n";
        logText += "filePattern=" + instance.filePattern + "\n\n";

        // Adding argument fields.
        Field[] fields = instance.getClass().getDeclaredFields();
        for (int i = 1; i < fields.length - 1; i++) {
            fields[i].setAccessible(true);
            String fieldName = fields[i].getName();
            try {
                String fieldValue = fields[i].get(instance).toString();
                logText += fieldName + "=" + fieldValue + "\n";
                Defaults.set(fieldName, fieldValue);
            } catch (IllegalAccessException e) {
                logText += "ERR:VALUE_NOT_LOGGED";
            }
        }

        // Saving logfile.
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));
        String logFile = dateTime + "_" + instance.getClass().getSimpleName();
        Path filePath = Paths.get(logDir(), logFile);
        try {
            Files.write(filePath, logText.getBytes());
        } catch (IOException e) {
            IJ.log(e.getMessage());
        }
    }

    private static String logDir() {
        String logDirString = Paths.get(ij.Menus.getPlugInsPath(), "GdMN Plugin", "Logs").toString();
        File dir = new File(logDirString);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }
        return logDirString;
    }
}
