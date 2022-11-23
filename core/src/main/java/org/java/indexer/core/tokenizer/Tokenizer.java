package org.java.indexer.core.tokenizer;

import java.nio.file.Path;
import java.util.Map;

/**
 * Represent a function that accepts file path and returns map of tokens as strings and its quantities.
 * <pre>
 *  Map.of("token1", "2", "token2", "6", "token3", "1");
 *  </pre>
 * with "token1" as string representation of token and "2" as number of its occurrences in the given file.
 */
public interface Tokenizer {

    /**
     * @param path of file to process
     * @return map of tokens as strings and its quantities
     */
    Map<String, Integer> tokenize(Path path);

}
