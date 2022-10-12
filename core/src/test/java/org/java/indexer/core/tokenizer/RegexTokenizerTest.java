package org.java.indexer.core.tokenizer;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegexTokenizerTest {

    private final RegexTokenizer tokenizer = new RegexTokenizer();

    @Test
    public void defaultRegexTokenizer() {
        //given
        final String s = "test test0 1test _test test0 1test";

        //when
        final Map<String, Integer> result = tokenizer.tokenize(s);

        //then
        assertEquals(result, Map.of("test", 1, "test0", 2, "1test", 2,"_test", 1));
    }

}