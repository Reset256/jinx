package org.java.indexer.core;

import lombok.extern.slf4j.Slf4j;
import org.java.indexer.core.index.FolderWatcherService;
import org.java.indexer.core.index.Index;
import org.java.indexer.core.index.QueryResult;
import org.java.indexer.core.tokenizer.RegexTokenizer;
import org.java.indexer.core.tokenizer.Tokenizer;
import org.java.indexer.core.utils.RegExUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

/**
 * Instance of index. Indexer object should be reachable as long as index is needed.
 * Before letting indexer be collected by GC {@link Indexer#close} should be invoked.
 *
 * <p> <b>Usage Examples:</b>
 *
 * <p>Index should be created via one of available constructors.
 * <pre>
 * Indexer indexer = new Indexer();
 * </pre>
 * <p>All the files in the listed folders and single files will be indexed only if filename is not contained in the
 * ignoredNames collection.
 * <pre>
 * indexer.index(List.of("/path/to/folder", "path/to/file.file"));
 * </pre>
 * <p>Query result are aggregated in {@link QueryResult} objects.
 * <pre>
 * QueryResult queryResult = indexer.queryToken("token1");
 * </pre>
 * <p> If the index is no longer needed {@link Indexer#close} should be called upon Indexer object.
 * It is used to stop file watcher and to prevent memory leakage.
 * <pre>
 * indexer.close();
 * indexer = null;
 * </pre>
 */

@Slf4j
public class Indexer {

    private final Index index;
    private final FolderWatcherService folderWatcherService;

    /**
     * Empty index initialization. Index will use RegexTokenizer with default regular expression
     * that will tokenize file word by word.
     *
     * @see RegexTokenizer
     */
    public Indexer() {
        this.index = new Index(null);
        this.folderWatcherService = new FolderWatcherService(emptyList(), index);
    }

    /**
     * Empty index initialization. Index will use RegexTokenizer with default regular expression
     * that will tokenize file word by word.
     *
     * @param ignoredNames collection of ignored filenames. Files with the names contained in that collection will not be indexed. Might be null or empty.
     * @see RegexTokenizer
     */
    public Indexer(Collection<String> ignoredNames) {
        this.index = new Index(ignoredNames);
        this.folderWatcherService = new FolderWatcherService(ignoredNames, index);
    }

    /**
     * Empty index initialization. Index will use RegexTokenizer with provided regular expression as tokenizer.
     *
     * @param ignoredNames collection of ignored filenames. Files with the names contained in that collection will not be indexed. Might be null or empty.
     * @param regEx        String representation of regular expression
     * @throws RuntimeException in case of invalid regular expression
     * @see RegexTokenizer
     */

    public Indexer(Collection<String> ignoredNames, String regEx) {
        this.index = new Index(ignoredNames, RegExUtils.validateRegEx(regEx).orElseThrow(() -> new RuntimeException("Regular expression is not valid and cannot be used as part of tokenization algorithm")));
        this.folderWatcherService = new FolderWatcherService(ignoredNames, index);
    }

    /**
     * Empty index initialization. Index will use provided tokenizer object for every file tokenization.
     *
     * @param ignoredNames collection of ignored filenames. Files with the names contained in that collection will not be indexed. Might be null or empty.
     * @param tokenizer    implementation of Tokenizer interface
     * @throws RuntimeException in case of null tokenizer
     * @see Tokenizer
     */

    public Indexer(Collection<String> ignoredNames, Tokenizer tokenizer) {
        this.index = new Index(ignoredNames, Optional.ofNullable(tokenizer).orElseThrow(() -> new RuntimeException("Tokenizer cannot be null")));
        this.folderWatcherService = new FolderWatcherService(ignoredNames, index);
    }

    /**
     * Indexes provided files and folders. If parsing path throws an exception,
     * indexing will not be performed and invalid path will be ignored.
     *
     * @param paths list of String representations of paths to be indexed
     * @see InvalidPathException
     */

    public void index(List<String> paths) {
        paths.forEach(path -> {
            Path normalizedPath;
            try {
                normalizedPath = Paths.get(path).normalize();
            } catch (InvalidPathException e) {
                log.error("Path {} is not valid", path, e);
                return;
            }
            index.add(normalizedPath);
            folderWatcherService.watch(normalizedPath);
        });
    }

    /**
     * Returns all token occurrences of this index.
     *
     * @param token element to be found in the indexed files
     * @return {@link QueryResult} representation of token occurrences in the index
     * @throws IllegalArgumentException if the token in null or empty
     */

    public QueryResult queryToken(String token) {
        if (token != null && !token.isEmpty()) {
            return index.queryToken(token);
        } else {
            throw new IllegalArgumentException("Token should not be null or empty");
        }
    }

    /**
     * This method is to be called in case index in no longer needed.
     * It stops index from watching for changes in the indexed files and folders and clears current index.
     * Not using this method before GC collects {@link Indexer} might cause memory leakage.
     */

    public void close() {
        folderWatcherService.stop();
        index.clear();
    }
}
