package org.java.indexer.core.index;

import org.awaitility.Awaitility;
import org.java.indexer.core.Indexer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IndexerWithCustomRegExTest {

    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();
    private final String folderPath = IndexerWithCustomRegExTest.class.getResource(SEPARATOR + "jinx").getPath();

    @BeforeAll
    public static void beforeAll() {
        Awaitility.setDefaultPollInterval(Duration.ofSeconds(1));
        Awaitility.setDefaultPollDelay(Duration.ofMillis(100));
        Awaitility.setDefaultTimeout(Duration.ofMinutes(1));
    }

    @Test
    void indexWithValidRegEx() {
        String regEx = "\\s+";
        try (final Indexer indexer = new Indexer(List.of(".DS_Store"), regEx)) {
            indexer.index(List.of(folderPath));

            await().until(() -> indexer.queryToken("custom").getOccurrences().keySet(),
                    contains(folderPath + SEPARATOR + "abl.bla"));
        }
    }

    @Test
    void indexWithInvalidRegEx() {
        String regEx = "***";

        assertThrows(RuntimeException.class, () -> new Indexer(List.of(".DS_Store"), regEx),
                "Regular expression is not valid and cannot be used as part of tokenization algorithm");
    }

}
