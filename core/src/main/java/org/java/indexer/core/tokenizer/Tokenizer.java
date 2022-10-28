package org.java.indexer.core.tokenizer;

import java.nio.file.Path;
import java.util.Map;

public interface Tokenizer {

    Map<String, Integer> tokenize(Path path);

}
