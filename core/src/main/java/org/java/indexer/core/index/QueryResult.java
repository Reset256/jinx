package org.java.indexer.core.index;

import lombok.Value;

import java.util.Map;

@Value
public class QueryResult {

    String queryString;
    Integer totalOccurrencesCount;
    Map<String, Integer> occurrences;

    public QueryResult(String queryString, Map<String, Integer> occurrences) {
        this.queryString = queryString;
        this.occurrences = occurrences;
        totalOccurrencesCount = occurrences.values().stream().reduce(0, Integer::sum);
    }
}
