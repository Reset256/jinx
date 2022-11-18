package org.java.indexer.core.index;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

public class FolderWatcherService {

    private final FolderWatcher folderWatcher;

    public FolderWatcherService(List<String> ignoredNames, Index index) {
        try {
            folderWatcher = new FolderWatcher(FileSystems.getDefault().newWatchService(), index, ignoredNames);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Thread thread = new Thread(folderWatcher);
        thread.setDaemon(true);
        thread.start();
    }

    public void watch(Path path) {
        folderWatcher.watch(path);
    }

    public void stop() {
        folderWatcher.stop();
    }
}
