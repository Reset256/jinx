package org.java.indexer.core;

import lombok.SneakyThrows;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestFilesUtils {

    @SneakyThrows
    public static File createFile(String path, String content) {
        File newFile = new File(path);
        newFile.createNewFile();
        try (final FileWriter fileWriter = new FileWriter(newFile)) {
            fileWriter.write(content);
        }
        return newFile;
    }

    public static void deleteFiles(File... files) {
        for (File file : files) {
            file.delete();
        }
    }


    @SneakyThrows
    public static File createFolderAndFile(String folderPath, String filePath, String content) {
        File newFolder = new File(folderPath);
        File newFile = new File(filePath);
        newFolder.mkdir();
        newFile.createNewFile();
        try (final FileWriter fileWriter = new FileWriter(newFile)) {
            fileWriter.write(content);
        }
        return newFile;
    }

    @SneakyThrows
    public static String readContent(String filePath) {
        return Files.readString(Paths.get(filePath));
    }

    @SneakyThrows
    public static void appendNewLineToFile(String filePath, String content) {
        try (final FileWriter fileWriter = new FileWriter(filePath, true)) {
            fileWriter.write("\n" + content);
        }
    }

    @SneakyThrows
    public static void rewriteFileContent(String filePath, String content) {
        try (final FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(content);
        }
    }

    @SneakyThrows
    public static File moveFile(String filePath, String destinationFolder) {
        Path currentFilePath = Path.of(filePath);
        final Path newTarget = Files.move(currentFilePath,
                Paths.get(destinationFolder, currentFilePath.getFileName().toString()));
        return new File(newTarget.toUri());
    }

}
