package org.java.indexer.core.index;

import lombok.extern.java.Log;
import org.java.indexer.core.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.java.indexer.core.utils.FileUtils.isIgnoredFile;

@Log
public class FolderWatcher implements Runnable {

    private final WatchService watchService;
    private final Index index;
    private final Set<String> ignoredNames;
    private final Map<Path, Set<Path>> watchedPaths;

    public FolderWatcher(WatchService watchService, Index index, List<String> ignoredNames) {
        this.watchService = watchService;
        this.index = index;
        this.ignoredNames = new HashSet<>(ignoredNames);
        this.watchedPaths = new HashMap<>();
    }

    public void watch(Path path) {
        if (Files.isRegularFile(path)) {
            watchFile(path);
        } else {
            watchFolderRecursively(path);
        }
    }

    private void watchFolderRecursively(Path path) {
        FileUtils.listFolders(path).forEach(this::watchFolder);
    }


    private void watchFolder(Path path) {
        if (!watchedPaths.containsKey(path) || !watchedPaths.get(path).isEmpty()) {
            watchedPaths.put(path, Collections.emptySet());
            watchWithEvents(path, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            log.info(String.format("Folder %s added to watch service", path));
        } else {
            log.info(String.format("Folder %s is already in the watch service", path));
        }
    }

    private void removeFolder(Path path) {
        watchedPaths.remove(path);
    }

    private void watchFile(Path path) {
        final Path folderPath = path.getParent();
        if (watchedPaths.containsKey(folderPath)) {
            final Set<Path> filesPaths = watchedPaths.get(folderPath);
            if (filesPaths.isEmpty() || filesPaths.contains(path)) {
                log.info(String.format("File %s is already in the watch service", path));
            } else {
                filesPaths.add(path);
                log.info(String.format("File %s added to watch service", path));
            }
        } else {
            final HashSet<Path> filesPaths = new HashSet<>();
            filesPaths.add(path);
            watchedPaths.put(folderPath, filesPaths);
            watchWithEvents(folderPath, ENTRY_DELETE, ENTRY_MODIFY);
            log.info(String.format("File %s added to watch service", path));
        }
    }

    private void watchWithEvents(Path path, WatchEvent.Kind<Path>... events) {
        try {
            path.register(watchService, events);
        } catch (IOException e) {
            log.severe(String.format("Folder watcher cannot be registered", e));
        }
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
                    log.info(String.format("Event is ignored for file %s", filename));
                    continue;
                }
                final Path contextPath = ((Path) key.watchable()).resolve(filename);
                if (Files.isDirectory(contextPath)) {
                    log.info(String.format("Event of type %s occurred with folder %s", kind, contextPath));
                    processFolderEventWithIndex(kind, contextPath);
                } else {
                    log.info(String.format("Event of type %s occurred with file %s", kind, contextPath));
                    processFileEventWithIndex(kind, contextPath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                Path watchedFolder = (Path) key.watchable();
                index.removeFolder(watchedFolder);
                log.info(String.format("Token is invalid, folder %s is not being watched anymore", watchedFolder));
            }
        }
    }

    private void processFileEventWithIndex(WatchEvent.Kind<Path> kind, Path contextPath) {
        final Path folderName = contextPath.getParent();
        if (watchedPaths.containsKey(folderName)) {
            final Set<Path> pathSet = watchedPaths.get(folderName);
            if (ENTRY_CREATE.equals(kind)) {
                if (pathSet.isEmpty()) {
                    index.addFile(contextPath);
                }
            } else if (ENTRY_MODIFY.equals(kind)) {
                if (pathSet.isEmpty() || pathSet.contains(contextPath)) {
                    index.addFile(contextPath);
                }
            } else {
                if (ENTRY_DELETE.equals(kind)) {
                    if (pathSet.isEmpty()) {
                        index.removeFile(contextPath);
                    } else if (pathSet.contains(contextPath)) {
                        index.removeFile(contextPath);
                        if (pathSet.remove(contextPath) && pathSet.isEmpty()) {
                            watchedPaths.remove(folderName);
                        }
                    }
                }
            }
        }
    }

    private void processFolderEventWithIndex(WatchEvent.Kind<Path> kind, Path contextPath) {
        if (ENTRY_CREATE.equals(kind)) {
            this.watch(contextPath);
            index.add(contextPath);
        } else if (ENTRY_DELETE.equals(kind)) {
            this.removeFolder(contextPath);
            index.removeFolder(contextPath);
        }
    }
}
