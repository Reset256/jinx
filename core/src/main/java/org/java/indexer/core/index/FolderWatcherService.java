package org.java.indexer.core.index;

import org.java.indexer.core.utils.FileUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderWatcherService {

    private final Set<Path> watchedFolder;
    private final FolderWatcher folderWatcher;

    public FolderWatcherService(List<String> ignoredNames, Index index) {
        this.watchedFolder = new HashSet<>();
        try {
            folderWatcher = new FolderWatcher(FileSystems.getDefault().newWatchService(), index, ignoredNames);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Thread thread = new Thread(folderWatcher);
        thread.setDaemon(true);
        thread.start();
    }

    public void watchFolders(Path folderPath) {
        FileUtils.listFolders(folderPath)
                .stream()
                .filter(o -> !watchedFolder.contains(o))
                .peek(watchedFolder::add)
                .forEach(folderWatcher::watchFolder);
    }
}
