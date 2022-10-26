package org.java.indexer.core;

import org.java.indexer.core.index.FolderWatcherService;
import org.java.indexer.core.index.Index;
import org.java.indexer.core.index.IndexedFile;
import org.java.indexer.core.utils.RegExUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Indexer {

    private final Index index;
    private final FolderWatcherService folderWatcherService;

    public Indexer(List<String> ignoredNames) {
        this.index = new Index(ignoredNames);
        this.folderWatcherService = new FolderWatcherService(ignoredNames);
    }

    public Indexer(List<String> ignoredNames, String regEx) {
        if (!RegExUtils.isRegExValid(regEx)) {
            throw new RuntimeException("Regular expression is not valid and cannot be used as part of tokenization algorithm");
        }
        this.index = new Index(ignoredNames, regEx);
        this.folderWatcherService = new FolderWatcherService(ignoredNames);
    }

    public void indexFolder(String path) {
        final Path folderPath = Paths.get(path).normalize();
        index.addFolder(folderPath);
        folderWatcherService.watchFoldersWithIndex(folderPath, index);
    }

    public List<IndexedFile> queryToken(String token) {
        return index.queryToken(token);
    }
}
