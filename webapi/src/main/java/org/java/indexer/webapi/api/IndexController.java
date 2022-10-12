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

@RestController
@RequestMapping("/index")
public class IndexController {

    private final Indexer indexer;

    public IndexController() {
        this.indexer = new Indexer(List.of(".DS_Store"));
    }

    @PostMapping
    public ResponseEntity<?> index(@RequestBody Map<String, String> body) {
        indexer.indexFolder(body.get("path"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> queryToken(@PathVariable String token) {
        return ResponseEntity.ok(indexer.queryToken(token));
    }
}
