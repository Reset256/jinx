package org.java.indexer.core.utils;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegExUtils {

    public static boolean isRegExValid(String regEx) {
        try {
            Pattern.compile(regEx);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
}
