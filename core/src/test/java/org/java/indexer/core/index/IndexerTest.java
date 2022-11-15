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
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

class IndexerTest {

    private Indexer indexer;
    private final String outerFolderPath = IndexerTest.class.getResource(SEPARATOR + "jinx").getPath();
    private final String innerFolderPath = outerFolderPath + SEPARATOR + "inner";

    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();

    @BeforeEach
    public void beforeEach() {
        indexer = new Indexer(List.of(".DS_Store"));
    }

    @SneakyThrows
    @Test
    public void indexInnerAndOuterFolder() {
        //given
        indexer.index(List.of(outerFolderPath));

        //when
        Thread.sleep(1000);
        final QueryResult result = indexer.queryToken("dependency");

        //then
        assertThat(result.getOccurrences().keySet(),
                contains(containsString("target/test-classes/jinx/abl.bla"),
                        containsString("target/test-classes/jinx/inner/bla.bla"))
        );
    }

    @SneakyThrows
    @Test
    public void indexInnerFolder() {
        //given
        indexer.index(List.of(outerFolderPath));

        //when
        Thread.sleep(1000);
        final QueryResult result = indexer.queryToken("logback");

        //then
        assertThat(result.getOccurrences().keySet(),
                contains(containsString(("target/test-classes/jinx/inner/bla.bla"))));
    }


    @SneakyThrows
    @Test
    public void doubleDotFilePath() {
        //given
        String innerFolderDoubleDot = innerFolderPath + SEPARATOR + ".." + SEPARATOR + "inner";
        indexer.index(List.of(innerFolderPath));
        indexer.index(List.of(innerFolderDoubleDot));

        //when
        Thread.sleep(1000);
        final QueryResult result = indexer.queryToken("logback");

        //then
        assertThat(result.getOccurrences().keySet(), hasSize(1));
        assertThat(result.getOccurrences().keySet(),
                contains(containsString(("target/test-classes/jinx/inner/bla.bla"))));

    }

    @SneakyThrows
    @Test
    public void indexOuterFolder() {
        //given
        indexer.index(List.of(outerFolderPath));

        //when
        Thread.sleep(1000);
        final QueryResult result = indexer.queryToken("classic1");

        //then
        assertThat(result.getOccurrences().keySet(),
                contains(containsString(("target/test-classes/jinx/abl.bla"))));
    }

    @Test
    public void outerFolderFileCreation() {
        //given
        indexer.index(List.of(outerFolderPath));

        //when
        final String newFilePath = outerFolderPath + SEPARATOR + "outer.txt";
        File newFile = new File(newFilePath);
        try {
            newFile.createNewFile();
            try (final FileWriter fileWriter = new FileWriter(newFile)) {
                fileWriter.write("newOuterFile");
            }
            Thread.sleep(15000);
            final QueryResult result = indexer.queryToken("newOuterFile");

            //then
            assertThat(result.getOccurrences().keySet(),
                    containsInAnyOrder(newFilePath));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            newFile.delete();
        }
    }

    @Test
    public void creationOfFolderAndFile() {
        //given
        indexer.index(List.of(outerFolderPath));

        //when
        final String newFolderPath = outerFolderPath + SEPARATOR + "newInner";
        final String newFilePath = newFolderPath + SEPARATOR + "newInner.txt";
        File newFolder = new File(newFolderPath);
        File newFile = new File(newFilePath);
        try {
            newFolder.mkdir();
            newFile.createNewFile();
            try (final FileWriter fileWriter = new FileWriter(newFile)) {
                fileWriter.write("newOuterFile");
            }
            Thread.sleep(15000);
            final QueryResult result = indexer.queryToken("newOuterFile");

            //then
            assertThat(result.getOccurrences().keySet(),
                    containsInAnyOrder(newFilePath));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            newFile.delete();
            newFolder.delete();
        }
    }

    @Test
    public void innerFolderFileCreation() {
        //given
        indexer.index(List.of(outerFolderPath));

        //when
        final String newFilePath = innerFolderPath + SEPARATOR + "inner.txt";
        File newFile = new File(newFilePath);
        try {
            newFile.createNewFile();
            try (final FileWriter fileWriter = new FileWriter(newFile)) {
                fileWriter.write("newInnerFile");
            }
            Thread.sleep(15000);
            final QueryResult result = indexer.queryToken("newInnerFile");

            //then
            assertThat(result.getOccurrences().keySet(),
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
        indexer.index(List.of(outerFolderPath));

        //when
        final String filePath = outerFolderPath + SEPARATOR + "abl.bla";
        final String content = Files.readString(Paths.get(filePath));
        try {
            try (final FileWriter fileWriter = new FileWriter(filePath, true)) {
                fileWriter.write("\nnewLineText");
            }
            Thread.sleep(15000);
            final QueryResult result = indexer.queryToken("newLineText");

            //then
            assertThat(result.getOccurrences().keySet(),
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
        indexer.index(List.of(outerFolderPath));

        //when
        final String filePath = innerFolderPath + SEPARATOR + "bla.bla";
        final String content = Files.readString(Paths.get(filePath));
        try {
            try (final FileWriter fileWriter = new FileWriter(filePath, true)) {
                fileWriter.write("\nnewLineText");
            }
            Thread.sleep(15000);
            final QueryResult result = indexer.queryToken("newLineText");

            //then
            assertThat(result.getOccurrences().keySet(),
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
