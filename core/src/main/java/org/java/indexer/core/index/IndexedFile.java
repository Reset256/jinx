package org.java.indexer.core.index;

import lombok.Getter;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class IndexedFile {
    @Getter private final UUID id;
    @Getter private final Path path;
    private final Map<String, Integer> countedTokens;

    public IndexedFile(UUID id, Path path, Map<String, Integer> countedTokens) {
        this.id = id;
        this.path = path;
        this.countedTokens = countedTokens;
    }

    public int query(String token) {
        return countedTokens.getOrDefault(token, 0);
    }
}
