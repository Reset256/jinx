package org.java.indexer.core.tokenizer;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RegexTokenizer implements Tokenizer {

    private static final String DEFAULT_REGEX = "\\W+";

    private final String regEx;


    public RegexTokenizer(String regEx) {
        this.regEx = regEx;
    }

    public RegexTokenizer() {
        this.regEx = DEFAULT_REGEX;
    }

    @Override
    public Map<String, Integer> tokenize(String s) {
        return Arrays.stream(s.split(regEx))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toMap(Function.identity(), s1 -> 1, Integer::sum));
    }
}
