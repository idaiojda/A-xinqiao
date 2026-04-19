package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.Article;
import com.example.xinqiaobackend.service.ArticleService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping(path = "/api/articles", produces = MediaType.APPLICATION_JSON_VALUE)
public class ArticleController {
    private final ArticleService service;
    public ArticleController(ArticleService service) { this.service = service; }

    @GetMapping
    public List<Article> listDefault(@RequestParam(required = false) String category,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return service.list(category, page, size);
    }

    @GetMapping("/list")
    public List<Article> list(@RequestParam(required = false) String category,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {
        return service.list(category, page, size);
    }

    @GetMapping("/{id}")
    public Article get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    public Article create(@RequestParam String title, @RequestParam String content, @RequestParam(required = false) String category) {
        return service.create(title, content, category);
    }

    @PostMapping("/{id}")
    public Article update(@PathVariable Long id, @RequestParam(required = false) String title,
                          @RequestParam(required = false) String content,
                          @RequestParam(required = false) String category) {
        return service.update(id, title, content, category);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        service.delete(id);
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        return res;
    }
}

