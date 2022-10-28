package org.java.indexer.core.tokenizer;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegexTokenizerTest {

    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();
    private final RegexTokenizer tokenizer = new RegexTokenizer();

    @Test
    @SneakyThrows
    public void defaultRegexTokenizer() {
        //given
        final String s = "test test0 1test _test test0 1test </test>";
        final Path latinWordsFile = Paths.get(RegexTokenizerTest.class.getResource(SEPARATOR + "latinWords").toURI());

        //when
        final Map<String, Integer> result = tokenizer.tokenize(latinWordsFile);

        //then
        assertEquals(Map.of("test", 2, "test0", 2, "1test", 2, "_test", 1), result);
    }

    @Test
    @SneakyThrows
    public void defaultRegexTokenizer_russianLanguage() {
        //given
        final Path russianWordsFile = Paths.get(RegexTokenizerTest.class.getResource(SEPARATOR + "russianWords").toURI());

        //when
        final Map<String, Integer> result = tokenizer.tokenize(russianWordsFile);

        //then
        assertEquals(Map.of("тест", 2, "тест0", 2, "1тест", 2, "_тест", 1, "тёст", 1, "тЁст", 1), result);
    }

    @Test
    @SneakyThrows
    public void defaultRegexTokenizerWithDigits() {
        //given
        final Path digitsFile = Paths.get(RegexTokenizerTest.class.getResource(SEPARATOR + "digits").toURI());

        //when
        final Map<String, Integer> result = tokenizer.tokenize(digitsFile);

        //then
        assertEquals(Map.of("1", 1, "2", 1, "3", 1, "4", 1), result);
    }

    @Test
    @SneakyThrows
    public void defaultRegexTokenizerWithUtf16File() {
        //given
        final Path utf16File = Paths.get(RegexTokenizerTest.class.getResource(SEPARATOR + "utf16").toURI());

        //when
        final Map<String, Integer> result = tokenizer.tokenize(utf16File);

        //then
        assertEquals(Map.of("TEXTINCLUDE", 1, "BEGIN", 1, "INVOKED", 1), result);
    }
}