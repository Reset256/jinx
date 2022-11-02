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
    private final List<String> ignoredNames;
    private final Lock writeLock;
    private final Lock readLock;


    public Index(List<String> ignoredNames) {
        this.ignoredNames = ignoredNames;
        this.tokenizer = new RegexTokenizer();
        this.indexedFiles = new ConcurrentHashMap<>();
        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        readLock = readWriteLock.readLock();
    }

    public Index(List<String> ignoredNames, Pattern regEx) {
        this.ignoredNames = ignoredNames;
        this.tokenizer = new RegexTokenizer(regEx);
        this.indexedFiles = new ConcurrentHashMap<>();
        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        readLock = readWriteLock.readLock();
    }

    public void addFolder(Path folderPath) {
        if (Files.isRegularFile(folderPath)) {
            throw new RuntimeException("Only folders can be added to index");
        }
        listFiles(folderPath, ignoredNames).forEach(this::addFile);
        log.info("Folder {} added to index", folderPath);
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
        if (!indexedFiles.containsKey(filePath)) {
            writeLock.lock();
            try {
                indexedFiles.compute(filePath,
                        (path1, indexedFile) -> new IndexedFile(UUID.randomUUID(), filePath, tokenizer.tokenize(filePath)));
                log.info("File {} added to index", filePath);
            } finally {
                writeLock.unlock();
            }
        } else {
            log.info("File {}  is already in the index", filePath);
        }
    }

    void removeFile(Path filePath) {
        if (indexedFiles.containsKey(filePath)) {
            writeLock.lock();
            try {
                indexedFiles.remove(filePath);
            } finally {
                writeLock.unlock();
            }
        } else {
            throw new RuntimeException("File is not in the index");
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

}
