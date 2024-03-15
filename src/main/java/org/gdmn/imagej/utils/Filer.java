package org.gdmn.imagej.utils;

import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Filer class containing static methods for saving and retrieving images and
 * content.
 */
public class Filer {

    /**
     * Recursively searches a folder for image files that match the specified
     * pattern.
     *
     * @param pathString the path to the top-level directory containing the images
     *                   for analysis.
     * @param pattern    the target regex or string pattern for the base images
     *                   (i.e. "roi.tif").
     * @return the list of paths to each image folder.
     */
    public static List<String> getBasePaths(String pathString, String pattern) {
        List<String> pathList = new ArrayList<String>();
        Path parentDir = Paths.get(pathString);
        try (Stream<Path> paths = Files.walk(parentDir)) {
            paths.filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName().toString().equals(pattern))
                    .forEach(path -> pathList.add(path.getParent().toString()));
        } catch (IOException e) {
            return Collections.emptyList();
        }
        return pathList;
    }

    /**
     * Saves an IJ image to the specified location.
     *
     * @param imp       the ImagePlus object to be saved.
     * @param basePath   the path to the image folder.
     * @param subFolder the sub-folder to save to.
     * @param fileName  the file name to save as.
     */
    public static void save(ImagePlus imp, String basePath, String subFolder, String fileName) {
        String savePath = getPath(basePath, subFolder, fileName);
        IJ.save(imp, savePath);
    }

    /**
     * Gets a path from the specified location, creating a new sub-folder if needed.
     *
     * @param basePath  the path to the image folder.
     * @param subFolder the name of the sub-folder.
     * @param fileName  the name of the file.
     * @return the full path to the file.
     */
    public static String getPath(String basePath, String subFolder, String fileName) {
        String saveDir = Paths.get(basePath, subFolder).toString();
        File dir = new File(saveDir);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }
        return Paths.get(saveDir, fileName).toString();
    }

    /**
     * Deletes an file from the specified location.
     *
     * @param basePath  the path to the image folder.
     * @param subFolder the name of the subfolder.
     * @param fileName  the name of the file to delete.
     */
    public static void delete(String basePath, String subFolder, String fileName) {
        String filePath = getPath(basePath, subFolder, fileName);
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
    }

}
