package org.java.indexer.core.index;

import lombok.extern.slf4j.Slf4j;
import org.java.indexer.core.tokenizer.RegexTokenizer;
import org.java.indexer.core.tokenizer.Tokenizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.java.indexer.core.utils.FileUtils.listFiles;
import static org.java.indexer.core.utils.FileUtils.readFile;

@Slf4j
public class Index {

    private final Tokenizer tokenizer;
    private final ConcurrentHashMap<Path, IndexedFile> indexedFiles;
    private final ReadWriteLock readWriteLock;
    private final List<String> ignoredNames;


    public Index(List<String> ignoredNames) {
        this.ignoredNames = ignoredNames;
        this.tokenizer = new RegexTokenizer();
        this.indexedFiles = new ConcurrentHashMap<>();
        this.readWriteLock = new ReentrantReadWriteLock();
    }
    public Index(List<String> ignoredNames, String regEx) {
        this.ignoredNames = ignoredNames;
        this.tokenizer = new RegexTokenizer(regEx);
        this.indexedFiles = new ConcurrentHashMap<>();
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    public void addFolder(Path folderPath) {
        if (Files.isRegularFile(folderPath)) {
            throw new RuntimeException("Only folders can be added to index");
        }
        listFiles(folderPath, ignoredNames).forEach(this::addFile);
        log.info("Folder {} added to index", folderPath);
    }

    void removeFolder(Path folderPath) {
        try {
            readWriteLock.writeLock().lock();
            indexedFiles.entrySet().removeIf(pathIndexedFileEntry -> pathIndexedFileEntry.getKey().startsWith(folderPath.toString()));
        } finally {
            readWriteLock.writeLock().unlock();
        }
        log.info("Folder {} removed from index", folderPath);
    }

    void addFile(Path filePath) {
        if (!indexedFiles.containsKey(filePath)) {
            try {
                readWriteLock.writeLock().lock();
                indexedFiles.compute(filePath,
                        (path1, indexedFile) -> new IndexedFile(UUID.randomUUID(), filePath, tokenizer.tokenize(readFile(filePath))));
                log.info("File {} added to index", filePath);
            } finally {
                readWriteLock.writeLock().unlock();
            }
        } else {
            log.info("File {}  is already in the index", filePath);
        }
    }

    void removeFile(Path filePath) {
        if (indexedFiles.containsKey(filePath)) {
            try {
                readWriteLock.writeLock().lock();
                indexedFiles.remove(filePath);
            } finally {
                readWriteLock.writeLock().unlock();
            }
        } else {
            throw new RuntimeException("File is not in the index");
        }
        log.info("File {} removed from index", filePath);
    }

    public QueryResult queryToken(String token) {
        log.info("Looking for token \"{}\" in the index", token);
        try {
            if (readWriteLock.readLock().tryLock(5, TimeUnit.SECONDS)) {
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
            readWriteLock.readLock().unlock();
        }
    }

}
