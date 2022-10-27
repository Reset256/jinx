package org.java.indexer.core.index;

import lombok.SneakyThrows;
import org.java.indexer.core.Indexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

class IndexerTest {

    private Indexer indexer;
    private String outerFolderPath;
    private String innerFolderPath;

    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();

    @BeforeEach
    public void beforeEach() {
        indexer = new Indexer(List.of(".DS_Store"));
        outerFolderPath = IndexerTest.class.getResource(SEPARATOR + "jinx").getPath();
        innerFolderPath = outerFolderPath + SEPARATOR + "inner";
    }

    @Test
    public void indexInnerAndOuterFolder() {
        //given
        indexer.indexFolder(outerFolderPath);

        //when
        final List<IndexedFile> result = indexer.queryToken("dependency");

        //then
        assertThat(result.stream().map(IndexedFile::getPath).map(Path::toString).collect(Collectors.toList()),
                containsInAnyOrder("/Users/victor/IdeaProjects/jinx/core/target/test-classes/jinx/abl.bla",
                        "/Users/victor/IdeaProjects/jinx/core/target/test-classes/jinx/inner/bla.bla"
                ));
    }

    @Test
    public void indexInnerFolder() {
        //given
        indexer.indexFolder(outerFolderPath);

        //when
        final List<IndexedFile> result = indexer.queryToken("logback");

        //then
        assertThat(result.stream().map(IndexedFile::getPath).map(Path::toString).collect(Collectors.toList()),
                containsInAnyOrder("/Users/victor/IdeaProjects/jinx/core/target/test-classes/jinx/inner/bla.bla"));
    }


    @Test
    public void doubleDotFilePath() {
        //given
        String innerFolderDoubleDot = innerFolderPath + SEPARATOR + ".." + SEPARATOR + "inner";
        indexer.indexFolder(innerFolderPath);
        indexer.indexFolder(innerFolderDoubleDot);

        //when
        final List<IndexedFile> result = indexer.queryToken("logback");

        //then
        assertThat(result, hasSize(1));
        assertThat(result.stream().map(IndexedFile::getPath).map(Path::toString).collect(Collectors.toList()),
                containsInAnyOrder("/Users/victor/IdeaProjects/jinx/core/target/test-classes/jinx/inner/bla.bla"));

    }

    @Test
    public void indexOuterFolder() {
        //given
        indexer.indexFolder(outerFolderPath);

        //when
        final List<IndexedFile> result = indexer.queryToken("classic1");

        //then
        assertThat(result.stream().map(IndexedFile::getPath).map(Path::toString).collect(Collectors.toList()),
                containsInAnyOrder("/Users/victor/IdeaProjects/jinx/core/target/test-classes/jinx/abl.bla"));
    }

    @Test
    public void outerFolderFileCreation() {
        //given
        indexer.indexFolder(outerFolderPath);

        //when
        final String newFilePath = outerFolderPath + SEPARATOR + "outer.txt";
        File newFile = new File(newFilePath);
        try {
            newFile.createNewFile();
            try (final FileWriter fileWriter = new FileWriter(newFile)) {
                fileWriter.write("newOuterFile");
            }
            Thread.sleep(15000);
            final List<IndexedFile> result = indexer.queryToken("newOuterFile");

            //then
            assertThat(result.stream().map(IndexedFile::getPath).map(Path::toString).collect(Collectors.toList()),
                    containsInAnyOrder(newFilePath));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            newFile.delete();
        }
    }

    @Test
    public void innerFolderFileCreation() {
        //given
        indexer.indexFolder(outerFolderPath);

        //when
        final String newFilePath = innerFolderPath + SEPARATOR + "inner.txt";
        File newFile = new File(newFilePath);
        try {
            newFile.createNewFile();
            try (final FileWriter fileWriter = new FileWriter(newFile)) {
                fileWriter.write("newInnerFile");
            }
            Thread.sleep(15000);
            final List<IndexedFile> result = indexer.queryToken("newInnerFile");

            //then
            assertThat(result.stream().map(IndexedFile::getPath).map(Path::toString).collect(Collectors.toList()),
                    containsInAnyOrder(newFilePath));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            newFile.delete();
        }
    }

    @Test
    @SneakyThrows
    public void outerFolderFileChange() {
        //given
        indexer.indexFolder(outerFolderPath);

        //when
        final String filePath = outerFolderPath + SEPARATOR + "abl.bla";
        final String content = Files.readString(Paths.get(filePath));
        try {
            try (final FileWriter fileWriter = new FileWriter(filePath, true)) {
                fileWriter.write("\nnewLineText");
            }
            Thread.sleep(15000);
            final List<IndexedFile> result = indexer.queryToken("newLineText");

            //then
            assertThat(result.stream().map(IndexedFile::getPath).map(Path::toString).collect(Collectors.toList()),
                    containsInAnyOrder(filePath));

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try (final FileWriter fileWriter = new FileWriter(filePath)) {
                fileWriter.write(content);
            }
        }
    }


    @Test
    @SneakyThrows
    public void innerFolderFileChange() {
        //given
        indexer.indexFolder(outerFolderPath);

        //when
        final String filePath = innerFolderPath + SEPARATOR + "bla.bla";
        final String content = Files.readString(Paths.get(filePath));
        try {
            try (final FileWriter fileWriter = new FileWriter(filePath, true)) {
                fileWriter.write("\nnewLineText");
            }
            Thread.sleep(15000);
            final List<IndexedFile> result = indexer.queryToken("newLineText");

            //then
            assertThat(result.stream().map(IndexedFile::getPath).map(Path::toString).collect(Collectors.toList()),
                    containsInAnyOrder(filePath));

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try (final FileWriter fileWriter = new FileWriter(filePath)) {
                fileWriter.write(content);
            }
        }
    }

}
