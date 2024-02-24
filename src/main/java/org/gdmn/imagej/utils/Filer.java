package org.gdmn.imagej.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import ij.IJ;
import ij.ImagePlus;

public class Filer {

    public static List<String> getFiles(String pathString, String pattern) {
        List<String> pathList = new ArrayList<String>();
        Path parentDir = Paths.get(pathString);
        try (Stream<Path> paths = Files.walk(parentDir)) {
            paths.filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName().toString().equals(pattern))
                    .forEach(path -> pathList.add(path.toString()));
        } catch (IOException e) {
            System.out.println(e);
            return Collections.emptyList();
        }
        return pathList;
    }

    public static void save(ImagePlus imp, String roiPath, String folder, String saveName) {
        String dirPath = Paths.get(roiPath).getParent().toString();
        String saveDir = Paths.get(dirPath, folder).toString();
        File dir = new File(saveDir);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }
        String savePath = Paths.get(saveDir, saveName).toString();
        IJ.save(imp, savePath);
    }

}
