package org.java.indexer.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileUtils {

    public static List<Path> listFolders(Path folderPath) {
        final ArrayList<Path> paths = new ArrayList<>();

        final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                paths.add(dir);
                return super.preVisitDirectory(dir, attrs);
            }
        };
        walkWithVisitor(folderPath, visitor);
        return paths;
    }

    public static List<Path> listFiles(Path folderPath, List<String> ignoredNames) {
        final ArrayList<Path> paths = new ArrayList<>();
        final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!isIgnoredFile(file, ignoredNames)) {
                    paths.add(file);
                }
                return super.visitFile(file, attrs);
            }
        };
        walkWithVisitor(folderPath, visitor);
        return paths;
    }

    private static void walkWithVisitor(Path folderPath, FileVisitor<Path> visitor) {
        if (Files.isRegularFile(folderPath)) {
            throw new RuntimeException("Listing allowed from folders only");
        }
        try {
            Files.walkFileTree(folderPath, visitor);
        } catch (IOException e) {
            throw new RuntimeException("Directory parsing went wrong", e);
        }
    }

    public static String readFile(Path path) {
        try {
            log.info("Reading file {}", path.toString());
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("File cannot be read", e);
        }
    }

    public static Boolean isIgnoredFile(Path file, List<String> ignoredNames) {
        return ignoredNames.stream().anyMatch(file::endsWith);
    }


}
