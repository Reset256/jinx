package org.java.indexer.core.index;

import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.java.indexer.core.Indexer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.java.indexer.core.TestFilesUtils.appendNewLineToFile;
import static org.java.indexer.core.TestFilesUtils.createFile;
import static org.java.indexer.core.TestFilesUtils.createFolderAndFile;
import static org.java.indexer.core.TestFilesUtils.deleteFiles;
import static org.java.indexer.core.TestFilesUtils.moveFile;
import static org.java.indexer.core.TestFilesUtils.readContent;
import static org.java.indexer.core.TestFilesUtils.rename;
import static org.java.indexer.core.TestFilesUtils.rewriteFileContent;

class IndexerTest {

    private Indexer indexer;
    private final String outerFolderPath = IndexerTest.class.getResource(SEPARATOR + "jinx").getPath();
    private final String innerFolderPath = outerFolderPath + SEPARATOR + "inner";

    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();

    @BeforeAll
    public static void beforeAll() {
        Awaitility.setDefaultPollInterval(Duration.ofSeconds(1));
        Awaitility.setDefaultPollDelay(Duration.ofMillis(100));
        Awaitility.setDefaultTimeout(Duration.ofMinutes(1));
    }

    @BeforeEach
    public void beforeEach() {
        indexer = new Indexer(List.of(".DS_Store"));
    }
    @AfterEach
    public void afterEach() {
        indexer.close();
    }

    @SneakyThrows
    @Test
    public void indexInnerAndOuterFolderFilesAlreadyExist() {
        indexer.index(List.of(outerFolderPath));

        await("Checking inner and outer folder occurrences").until(() -> indexer.queryToken("dependency").getOccurrences().keySet(),
                containsInAnyOrder(innerFolderPath + SEPARATOR + "bla.bla", outerFolderPath + SEPARATOR + "abl.bla"));
        await("Checking inner folder occurrences").until(() -> indexer.queryToken("logback").getOccurrences().keySet(),
                contains(innerFolderPath + SEPARATOR + "bla.bla"));
        await("Checking outer folder occurrences").until(() -> indexer.queryToken("classic1").getOccurrences().keySet(),
                contains(outerFolderPath + SEPARATOR + "abl.bla"));
    }


    @SneakyThrows
    @Test
    public void doubleDotFilePath() {
        String innerFolderDoubleDot = innerFolderPath + SEPARATOR + ".." + SEPARATOR + "inner";
        indexer.index(List.of(innerFolderPath));
        indexer.index(List.of(innerFolderDoubleDot));

        await("Checking double dot path is not added")
                .until(() -> indexer.queryToken("logback").getOccurrences().keySet(), hasSize(1));
        await("Checking double dot path is not added")
                .until(() -> indexer.queryToken("logback").getOccurrences().keySet(),
                contains(innerFolderPath + SEPARATOR + "bla.bla"));
    }

    @Test
    public void indexFilesAndFolderOperations() {
        indexer.index(List.of(outerFolderPath));

        //files and folders creation
        final String outerFilePath = outerFolderPath + SEPARATOR + "outer.txt";
        final String outerFileContent = "newOuterFile";
        final File outerFile = createFile(outerFilePath, outerFileContent);

        final String innerFilePath = innerFolderPath + SEPARATOR + "inner.txt";
        final String innerFileContent = "newInnerFile";
        final File innerFile = createFile(innerFilePath, innerFileContent);

        final String newFolderPath = outerFolderPath + SEPARATOR + "newInner";
        final String newFolderFilePath = newFolderPath + SEPARATOR + "newInner.txt";
        final String innerFolderFileContent = "newInnerFolderAndFile";
        createFolderAndFile(newFolderPath, newFolderFilePath, innerFolderFileContent);

        await("Checking the new file in the outer folder")
                .until(() -> indexer.queryToken(outerFileContent).getOccurrences().keySet(),
                contains(outerFilePath));
        await("Checking the new file in the inner folder")
                .until(() -> indexer.queryToken(innerFileContent).getOccurrences().keySet(),
                contains(innerFilePath));
        await("Checking the new folder and the new file inside it")
                .until(() -> indexer.queryToken(innerFolderFileContent).getOccurrences().keySet(),
                contains(newFolderFilePath));

        //files moving
        final File movedInnerFile = moveFile(innerFilePath, outerFolderPath);

        await("Checking the new place containing moved file")
                .until(() -> indexer.queryToken(innerFileContent).getOccurrences().keySet(),
                contains(movedInnerFile.getPath()));
        await("Checking the old place not containing moved file")
                .until(() -> indexer.queryToken(innerFileContent).getOccurrences().keySet(),
                not(contains(innerFile.getPath())));

        //folder renaming
        final String renamedFolderPath = outerFolderPath + SEPARATOR + "renamedInner";
        final String renamedFolderFilePath = renamedFolderPath + SEPARATOR + "newInner.txt";
        final File renamedFolder = rename(newFolderPath, renamedFolderPath);

        await("Checking the renamed folder and the file inside it")
                .until(() -> indexer.queryToken(innerFolderFileContent).getOccurrences().keySet(),
                        contains(renamedFolderFilePath));

        //files deletion
        deleteFiles(outerFile, movedInnerFile, new File(renamedFolderFilePath));
        deleteFiles(renamedFolder);

        await("Checking the removed file in the outer folder")
                .until(() -> indexer.queryToken(outerFileContent).getOccurrences().keySet(),
                empty());
        await("Checking the removed file in the inner folder")
                .until(() -> indexer.queryToken(innerFileContent).getOccurrences().keySet(),
                empty());
        await("Checking the removed file in the removed folder")
                .until(() -> indexer.queryToken(innerFolderFileContent).getOccurrences().keySet(),
                empty());
    }

    @Test
    @SneakyThrows
    public void indexFileContentChange() {
        indexer.index(List.of(outerFolderPath));

        final String outerFilePath = outerFolderPath + SEPARATOR + "abl.bla";
        final String outerOriginalContent = readContent(outerFilePath);
        final String outerAdditionToFile = "newOuterLineText";
        appendNewLineToFile(outerFilePath, outerAdditionToFile);

        final String innerFilePath = innerFolderPath + SEPARATOR + "bla.bla";
        final String innerOriginalContent = readContent(innerFilePath);
        final String innerAdditionToFile = "newInnerLineText";
        appendNewLineToFile(innerFilePath, innerAdditionToFile);

        await("Looking for the addition in the outer folder file")
                .until(() -> indexer.queryToken(outerAdditionToFile).getOccurrences().keySet(),
                contains(outerFilePath));
        await("Looking for the addition in the inner folder file")
                .until(() -> indexer.queryToken(innerAdditionToFile).getOccurrences().keySet(),
                contains(innerFilePath));

        rewriteFileContent(outerFilePath, outerOriginalContent);
        rewriteFileContent(innerFilePath, innerOriginalContent);

        await("Looking for the deleted line in the outer folder file")
                .until(() -> indexer.queryToken(outerAdditionToFile).getOccurrences().keySet(),
                empty());
        await("Looking for the deleted line in the inner folder file")
                .until(() -> indexer.queryToken(innerAdditionToFile).getOccurrences().keySet(),
                empty());
    }

}
