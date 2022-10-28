package org.java.indexer.core.utils;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegExUtils {

    public static Optional<Pattern> validateRegEx(String regEx) {
        try {
            return Optional.of(Pattern.compile(regEx));
        } catch (PatternSyntaxException e) {
            return Optional.empty();
        }
    }
}
