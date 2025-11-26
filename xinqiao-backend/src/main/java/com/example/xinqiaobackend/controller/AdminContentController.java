package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.Article;
import com.example.xinqiaobackend.entity.Post;
import com.example.xinqiaobackend.repository.ArticleRepository;
import com.example.xinqiaobackend.repository.PostRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/content")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {
    private final ArticleRepository articleRepo;
    private final PostRepository postRepo;

    public AdminContentController(ArticleRepository articleRepo, PostRepository postRepo) {
        this.articleRepo = articleRepo;
        this.postRepo = postRepo;
    }

    @GetMapping("/articles")
    public List<Article> listArticles(@RequestParam(required = false) String status) {
        return status == null ? articleRepo.findAll() : articleRepo.findByReviewStatus(status);
    }

    @GetMapping("/articles/{id}")
    public com.example.xinqiaobackend.api.ApiResponse<Article> articleDetail(@PathVariable Long id) {
        Article a = articleRepo.findById(id).orElse(null);
        if (a == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        return com.example.xinqiaobackend.api.ApiResponse.success(a);
    }

    @PostMapping("/articles/{id}/approve")
    public com.example.xinqiaobackend.api.ApiResponse<Object> approveArticle(@PathVariable Long id) {
        Article a = articleRepo.findById(id).orElse(null);
        if (a == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        a.setReviewStatus("APPROVED");
        articleRepo.save(a);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已通过");
    }

    @PostMapping("/articles/{id}/reject")
    public com.example.xinqiaobackend.api.ApiResponse<Object> rejectArticle(@PathVariable Long id) {
        Article a = articleRepo.findById(id).orElse(null);
        if (a == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        a.setReviewStatus("REJECTED");
        articleRepo.save(a);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已驳回");
    }

    @GetMapping("/posts")
    public List<Post> listPosts(@RequestParam(required = false) String status) {
        return status == null ? postRepo.findAll() : postRepo.findByReviewStatus(status);
    }

    @GetMapping("/posts/{id}")
    public com.example.xinqiaobackend.api.ApiResponse<Post> postDetail(@PathVariable Long id) {
        Post p = postRepo.findById(id).orElse(null);
        if (p == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        return com.example.xinqiaobackend.api.ApiResponse.success(p);
    }

    @PostMapping("/posts/{id}/approve")
    public com.example.xinqiaobackend.api.ApiResponse<Object> approvePost(@PathVariable Long id) {
        Post p = postRepo.findById(id).orElse(null);
        if (p == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        p.setReviewStatus("APPROVED");
        postRepo.save(p);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已通过");
    }

    @PostMapping("/posts/{id}/reject")
    public com.example.xinqiaobackend.api.ApiResponse<Object> rejectPost(@PathVariable Long id) {
        Post p = postRepo.findById(id).orElse(null);
        if (p == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        p.setReviewStatus("REJECTED");
        postRepo.save(p);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已驳回");
    }
}