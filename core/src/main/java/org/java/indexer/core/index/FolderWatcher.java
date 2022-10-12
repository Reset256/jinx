package org.java.indexer.core.index;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.java.indexer.core.utils.FileUtils.isIgnoredFile;

@Slf4j
public class FolderWatcher implements Runnable {

    private final WatchService watchService;
    private final Index index;
    private final Path watchedFolder;
    private final List<String> ignoredNames;

    public FolderWatcher(WatchService watchService, Index index, Path watchedFolder, List<String> ignoredNames) {
        this.watchService = watchService;
        this.index = index;
        this.watchedFolder = watchedFolder;
        try {
            watchedFolder.register(watchService, ENTRY_CREATE,
                    ENTRY_DELETE, ENTRY_MODIFY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.ignoredNames = ignoredNames;
    }


    @Override
    @SuppressWarnings({"SleepWhileInLoop", "unchecked"})
    public void run() {
        while (true) {
            WatchKey key;
            try {
                key = watchService.take();
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                final WatchEvent.Kind<Path> kind = pathEvent.kind();
                final Path filename = pathEvent.context();
                if (isIgnoredFile(filename, ignoredNames)) {
                    log.info("Event is ignored for file {}", filename);
                    continue;
                }
                final Path contextPath = watchedFolder.resolve(filename);
                log.info("Event of type {} occured with file {}", kind, contextPath);
                if (ENTRY_CREATE.equals(kind)) {
                    index.addFile(contextPath);
                } else if (ENTRY_DELETE.equals(kind)) {
                    index.removeFile(contextPath);
                } else if (ENTRY_MODIFY.equals(kind)) {
                    index.removeFile(contextPath);
                    index.addFile(contextPath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                index.removeFolder(watchedFolder);
                log.info("Token is invalid, folder {} is not being watched anymore", watchedFolder);
                break;
            }
        }
    }
}
