package org.java.indexer.core;

import org.java.indexer.core.index.FolderWatcherService;
import org.java.indexer.core.index.Index;
import org.java.indexer.core.index.QueryResult;
import org.java.indexer.core.utils.RegExUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Indexer {

    private final Index index;
    private final FolderWatcherService folderWatcherService;

    public Indexer(List<String> ignoredNames) {
        this.index = new Index(ignoredNames);
        this.folderWatcherService = new FolderWatcherService(ignoredNames, index);
    }

    public Indexer(List<String> ignoredNames, String regEx) {
        this.index = new Index(ignoredNames, RegExUtils.validateRegEx(regEx)
                .orElseThrow(() -> new RuntimeException("Regular expression is not valid and cannot be used as part of tokenization algorithm")));
        this.folderWatcherService = new FolderWatcherService(ignoredNames, index);
    }

    public void index(List<String> paths) {
        paths.forEach(path -> {
            final Path normalizedPath = Paths.get(path).normalize();
            index.add(normalizedPath);
            folderWatcherService.watch(normalizedPath);
        });
    }

    public QueryResult queryToken(String token) {
        return index.queryToken(token);
    }
}
