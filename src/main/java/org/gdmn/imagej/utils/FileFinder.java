package org.gdmn.imagej.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class FileFinder {
    public static int count(String pathString, String pattern) {
        Path parentDir = Paths.get(pathString);
        try (Stream<Path> paths = Files.walk(parentDir)) {
            return (int) paths.filter(path -> Files.isRegularFile(path))
                .filter(path -> path.getFileName().toString().equals(pattern))
                .count();
        } catch(IOException e) {
            System.out.println(e);
            return -1;
        }
    }

    public static List<String> getFiles(String pathString, String pattern) {
        List<String> pathList = new ArrayList<String>();
        Path parentDir = Paths.get(pathString);
        try (Stream<Path> paths = Files.walk(parentDir)) {
            paths.filter(path -> Files.isRegularFile(path))
                 .filter(path -> path.getFileName().toString().equals(pattern))
                 .forEach(path -> pathList.add(path.toString()));
        } catch(IOException e) {
            System.out.println(e);
            return Collections.emptyList();
        }
        return pathList;
    }
    
}
