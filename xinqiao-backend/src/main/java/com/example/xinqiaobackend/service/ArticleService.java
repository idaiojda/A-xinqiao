package com.example.xinqiaobackend.service;

import com.example.xinqiaobackend.entity.Article;
import com.example.xinqiaobackend.repository.ArticleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {
    private final ArticleRepository repo;
    public ArticleService(ArticleRepository repo) { this.repo = repo; }

    @Cacheable(value = "articles", key = "((#category == null || #category.trim().isEmpty()) ? '__ALL__' : #category) + '-' + #page + '-' + #size")
    public List<Article> list(String category, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        // 只返回已审核通过的文章给普通用户
        if (category != null && category.trim().length() > 0) {
            return repo.findByCategoryAndReviewStatusOrderByPublishedAtDesc(category.trim(), "APPROVED", pageable).getContent();
        }
        return repo.findByReviewStatusOrderByPublishedAtDesc("APPROVED", pageable).getContent();
    }

    public Article get(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("article not found")); }

    @CacheEvict(value = "articles", allEntries = true)
    public Article create(String title, String content, String category) {
        Article a = new Article();
        a.setTitle(title);
        a.setContent(content);
        a.setCategory(category);
        return repo.save(a);
    }

    @CacheEvict(value = "articles", allEntries = true)
    public Article update(Long id, String title, String content, String category) {
        Article a = get(id);
        if (title != null) a.setTitle(title);
        if (content != null) a.setContent(content);
        if (category != null) a.setCategory(category);
        return repo.save(a);
    }

    @CacheEvict(value = "articles", allEntries = true)
    public void delete(Long id) { repo.deleteById(id); }
}

