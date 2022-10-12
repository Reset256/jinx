package org.java.indexer.core.index;

import org.java.indexer.core.utils.FileUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderWatcherService {

    private final ExecutorService executorService;
    private final List<String> ignoredNames;
    private final Set<Path> watchedFolder;

    public FolderWatcherService(List<String> ignoredNames) {
        executorService = Executors.newCachedThreadPool(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        this.ignoredNames = ignoredNames;
        this.watchedFolder = new HashSet<>();
    }

    public void watchFoldersWithIndex(Path folderPath, Index index) {
        final List<Path> newFolders = FileUtils.listFolders(folderPath);
        newFolders.stream()
                .filter(o -> !watchedFolder.contains(o))
                .peek(watchedFolder::add)
                .map(folder -> {
                    try {
                        return new FolderWatcher(FileSystems.getDefault().newWatchService(), index, folder, ignoredNames);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(executorService::submit);
    }
}
