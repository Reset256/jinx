package org.java.indexer.core.index;

import lombok.extern.slf4j.Slf4j;
import org.java.indexer.core.tokenizer.RegexTokenizer;
import org.java.indexer.core.tokenizer.Tokenizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.java.indexer.core.utils.FileUtils.listFiles;

@Slf4j
public class Index {

    private final Tokenizer tokenizer;
    private final ConcurrentHashMap<Path, IndexedFile> indexedFiles;
    private final Set<String> ignoredNames;
    private final Lock writeLock;
    private final Lock readLock;
    private final ExecutorService indexExecutorService;

    public Index(Collection<String> ignoredNames) {
        if (ignoredNames == null || ignoredNames.isEmpty()) {
            this.ignoredNames = Collections.emptySet();
        } else {
            this.ignoredNames = new HashSet<>(ignoredNames);
        }
        this.tokenizer = new RegexTokenizer();
        this.indexedFiles = new ConcurrentHashMap<>();
        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        readLock = readWriteLock.readLock();
        indexExecutorService = Executors.newWorkStealingPool();
    }

    public Index(Collection<String> ignoredNames, Pattern regEx) {
        if (ignoredNames == null || ignoredNames.isEmpty()) {
            this.ignoredNames = Collections.emptySet();
        } else {
            this.ignoredNames = new HashSet<>(ignoredNames);
        }
        this.tokenizer = new RegexTokenizer(regEx);
        this.indexedFiles = new ConcurrentHashMap<>();
        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        readLock = readWriteLock.readLock();
        indexExecutorService = Executors.newWorkStealingPool();
    }

    public Index(Collection<String> ignoredNames, Tokenizer tokenizer) {
        if (ignoredNames == null || ignoredNames.isEmpty()) {
            this.ignoredNames = Collections.emptySet();
        } else {
            this.ignoredNames = new HashSet<>(ignoredNames);
        }
        this.tokenizer = tokenizer;
        this.indexedFiles = new ConcurrentHashMap<>();
        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        readLock = readWriteLock.readLock();
        indexExecutorService = Executors.newWorkStealingPool();
    }

    public void add(Path path) {
        if (Files.isRegularFile(path)) {
            indexExecutorService.submit(() -> addFile(path));
            log.info("File {} added to index", path);
        } else {
            listFiles(path, ignoredNames).forEach(filePath -> indexExecutorService.submit(() -> addFile(filePath)));
            log.info("Folder {} added to index", path);
        }
    }

    void removeFolder(Path folderPath) {
        writeLock.lock();
        try {
            indexedFiles.entrySet().removeIf(pathIndexedFileEntry -> pathIndexedFileEntry.getKey().startsWith(folderPath.toString()));
        } finally {
            writeLock.unlock();
        }
        log.info("Folder {} removed from index", folderPath);
    }

    void addFile(Path filePath) {
        final Map<String, Integer> tokenizedFile;
        try {
            tokenizedFile = tokenizer.tokenize(filePath);
        } catch (Exception e) {
            log.error("Something went wrong during tokenization, file {} was not indexed", filePath, e);
            return;
        }
        writeLock.lock();
        try {
            indexedFiles.compute(filePath,
                    (path1, indexedFile) -> new IndexedFile(UUID.randomUUID(), filePath, tokenizedFile));
        } finally {
            writeLock.unlock();
        }
        log.info("File {} is in the index", filePath);
    }

    void removeFile(Path filePath) {
        writeLock.lock();
        try {
            indexedFiles.remove(filePath);
        } finally {
            writeLock.unlock();
        }
        log.info("File {} removed from index", filePath);
    }

    public QueryResult queryToken(String token) {
        log.info("Looking for token \"{}\" in the index", token);
        try {
            if (readLock.tryLock(5, TimeUnit.SECONDS)) {
                final Map<String, Integer> occurrenceMap = indexedFiles.values().stream()
                        .map(indexedFile -> new AbstractMap.SimpleEntry<>(indexedFile.getPath(), indexedFile.query(token)))
                        .filter(entry -> entry.getValue() != 0)
                        .collect(Collectors.toMap(entry -> entry.getKey().toString(), AbstractMap.SimpleEntry::getValue));
                return new QueryResult(token, occurrenceMap);
            } else {
                throw new RuntimeException("Index is being updated, try later");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    public void clear() {
        writeLock.lock();
        try {
            indexedFiles.clear();
        } finally {
            writeLock.unlock();
        }
    }

}
