package org.java.indexer.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j
public class FileUtils {

    public static Set<Path> listFolders(Path folderPath) {
        final Set<Path> paths = new HashSet<>();

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

    public static Set<Path> listFiles(Path folderPath, Set<String> ignoredNames) {
        final Set<Path> paths = new HashSet<>();
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


    public static void parseAndConsume(Path path, Consumer<String> stringConsumer, Charset charset, Pattern regEx) throws IOException {
        try (final BufferedReader reader = Files.newBufferedReader(path, charset)) {
            String line = reader.readLine();
            while (line != null) {
                Arrays.stream(regEx.split(line))
                        .filter(token -> !token.isBlank())
                        .forEach(stringConsumer);
                line = reader.readLine();
            }
        }
    }

    public static Boolean isIgnoredFile(Path file, Set<String> ignoredNames) {
        return ignoredNames.contains(file.toString());
    }


}
