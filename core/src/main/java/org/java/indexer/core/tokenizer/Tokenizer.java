package org.java.indexer.core.tokenizer;

import java.util.Map;

public interface Tokenizer {

    Map<String, Integer> tokenize (String s);

}
