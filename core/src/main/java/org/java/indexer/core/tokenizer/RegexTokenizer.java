package org.java.indexer.core.tokenizer;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.java.indexer.core.utils.FileUtils.parseAndConsume;

@Slf4j
public class RegexTokenizer implements Tokenizer {

    private static final String DEFAULT_REGEX = "[^A-Za-z0-9_А-яЁё]";

    private static final List<Charset> SUPPORTED_ENCODINGS = List.of(UTF_8, UTF_16);
    private static final Function<Map<String, Integer>, Consumer<String>> RESULT_AGGREGATOR_SUPPLIER =
            map -> token -> map.compute(token, (key, count) -> Optional.ofNullable(count).orElse(0) + 1);

    private final Pattern regEx;


    public RegexTokenizer(Pattern regEx) {
        this.regEx = regEx;
    }

    public RegexTokenizer() {
        this.regEx = Pattern.compile(DEFAULT_REGEX);
    }

    @Override
    public Map<String, Integer> tokenize(Path path) {
        Map<String, Integer> result = new HashMap<>();
        final Consumer<String> resultAggregator = RESULT_AGGREGATOR_SUPPLIER.apply(result);

        for (Charset encoding : SUPPORTED_ENCODINGS) {
            try {
                parseAndConsume(path, resultAggregator, encoding, regEx);
                log.info("File {} parsed with {} encoding", path, encoding.name());
                break;
            } catch (IOException e) {
                log.error("File {} cannot be parsed with {} encoding", path, encoding.name());
            }
        }

        return result;
    }


}
