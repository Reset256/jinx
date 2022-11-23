package org.java.indexer.core.index;

import lombok.SneakyThrows;
import org.java.indexer.core.Indexer;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IndexerWithCustomRegExTest {

    private static final String separator = FileSystems.getDefault().getSeparator();
    private static final String FOLDER_PATH = IndexerWithCustomRegExTest.class.getResource(separator + "jinx").getPath();

    @SneakyThrows
    @Test
    void indexWithValidRegEx() {
        //given
        String regEx = "\\s+";

        //when
        final Indexer indexer = new Indexer(List.of(".DS_Store"), regEx);
        indexer.index(List.of(FOLDER_PATH));
        Thread.sleep(1000);

        final QueryResult result = indexer.queryToken("custom");

        //then
        assertThat(result.getOccurrences().keySet(),
                containsInAnyOrder("/Users/victor/IdeaProjects/jinx/core/target/test-classes/jinx/abl.bla"));
    }

    @Test
    void indexWithInvalidRegEx() {
        //given
        String regEx = "***";

        //when
        assertThrows(RuntimeException.class, () -> new Indexer(List.of(".DS_Store"), regEx),
                "Regular expression is not valid and cannot be used as part of tokenization algorithm");
    }

}
