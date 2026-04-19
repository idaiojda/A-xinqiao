package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Page<Article> findByCategoryOrderByPublishedAtDesc(String category, Pageable pageable);
    Page<Article> findAllByOrderByPublishedAtDesc(Pageable pageable);
    List<Article> findByReviewStatus(String reviewStatus);
    List<Article> findByAuthorIdOrderByPublishedAtDesc(String authorId);
    Page<Article> findByReviewStatusOrderByPublishedAtDesc(String reviewStatus, Pageable pageable);
    Page<Article> findByCategoryAndReviewStatusOrderByPublishedAtDesc(String category, String reviewStatus, Pageable pageable);
}

