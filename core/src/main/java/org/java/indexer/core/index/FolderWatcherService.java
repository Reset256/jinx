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
    private final Set<Path> watchedFolder;
    private final FolderWatcher folderWatcher;

    public FolderWatcherService(List<String> ignoredNames, Index index) {
        executorService = Executors.newFixedThreadPool(1, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        this.watchedFolder = new HashSet<>();
        try {
            folderWatcher = new FolderWatcher(FileSystems.getDefault().newWatchService(), index, ignoredNames);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        executorService.submit(folderWatcher);
    }

    public void watchFolders(Path folderPath) {
        final List<Path> newFolders = FileUtils.listFolders(folderPath);
        newFolders.stream()
                .filter(o -> !watchedFolder.contains(o))
                .peek(watchedFolder::add)
                .forEach(folderWatcher::watchFolder);
    }
}
