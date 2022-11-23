package org.java.indexer.core.index;

import lombok.extern.slf4j.Slf4j;
import org.java.indexer.core.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.java.indexer.core.utils.FileUtils.isIgnoredFile;

@Slf4j
public class FolderWatcher implements Runnable {

    private final WatchService watchService;
    private final Index index;
    private final Set<String> ignoredNames;
    private final Map<Path, Set<Path>> watchedPaths;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public FolderWatcher(WatchService watchService, Index index, Collection<String> ignoredNames) {
        this.watchService = watchService;
        this.index = index;
        if (ignoredNames == null || ignoredNames.isEmpty()) {
            this.ignoredNames = Collections.emptySet();
        } else {
            this.ignoredNames = new HashSet<>(ignoredNames);
        }
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
            log.info("Folder {} added to watch service", path);
        } else {
            log.info("Folder {} is already in the watch service", path);
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
                log.info("File {} is already in the watch service", path);
            } else {
                filesPaths.add(path);
                log.info("File {} added to watch service", path);
            }
        } else {
            final HashSet<Path> filesPaths = new HashSet<>();
            filesPaths.add(path);
            watchedPaths.put(folderPath, filesPaths);
            watchWithEvents(folderPath, ENTRY_DELETE, ENTRY_MODIFY);
            log.info("File {} added to watch service", path);
        }
    }

    private void watchWithEvents(Path path, WatchEvent.Kind<Path>... events) {
        try {
            path.register(watchService, events);
        } catch (IOException e) {
            log.error("Folder watcher cannot be registered", e);
        }
    }


    @Override
    @SuppressWarnings({"SleepWhileInLoop", "unchecked"})
    public void run() {
        running.set(true);
        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.take();
                Thread.sleep(50);
            } catch (InterruptedException e) {
                log.info("FolderWatcher is stopped");
                running.set(false);
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                final WatchEvent.Kind<Path> kind = pathEvent.kind();
                final Path filename = pathEvent.context();
                final Path contextPath = ((Path) key.watchable()).resolve(filename);
                if (Files.isDirectory(contextPath) || watchedPaths.containsKey(contextPath)) {
                    log.info("Event of type {} occurred with folder {}", kind, contextPath);
                    processFolderEventWithIndex(kind, contextPath);
                } else {
                    if (isIgnoredFile(filename, ignoredNames)) {
                        log.info("Event is ignored for file {}", filename);
                        continue;
                    }
                    log.info("Event of type {} occurred with file {}", kind, contextPath);
                    processFileEventWithIndex(kind, contextPath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                Path watchedFolder = (Path) key.watchable();
                this.removeFolder(watchedFolder);
                index.removeFolder(watchedFolder);
                log.info("Token is invalid, folder {} is not being watched anymore", watchedFolder);
            }
        }
    }

    public void stop() {
        running.set(false);
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
