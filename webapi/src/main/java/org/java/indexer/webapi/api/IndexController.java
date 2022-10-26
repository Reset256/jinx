package org.java.indexer.webapi.api;

import org.java.indexer.core.Indexer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/index")
public class IndexController {

    private Indexer indexer;

    @PostMapping
    @SuppressWarnings({"unchecked"})
    public ResponseEntity<?> index(@RequestBody Map<String, Object> body) {
        final String path = (String) body.get("path");
        final List<String> ignoredNames = (List<String>) body.get("ignoredNames");

        indexer = Optional.ofNullable(body.get("regEx"))
                .map(o -> (String) o)
                .map(s -> new Indexer(ignoredNames, s))
                .orElseGet(() -> new Indexer(ignoredNames));

        indexer.indexFolder(path);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> queryToken(@PathVariable String token) {
        return ResponseEntity.ok(indexer.queryToken(token));
    }
}
