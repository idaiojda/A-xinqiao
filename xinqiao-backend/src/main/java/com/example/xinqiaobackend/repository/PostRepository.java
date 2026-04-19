package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Post> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);
    List<Post> findByReviewStatus(String reviewStatus);
    
    // 只查询已审核通过的帖子
    Page<Post> findByReviewStatusOrderByCreatedAtDesc(String reviewStatus, Pageable pageable);
    Page<Post> findByCategoryAndReviewStatusOrderByCreatedAtDesc(String category, String reviewStatus, Pageable pageable);
    
    // 查询指定用户的指定状态帖子
    List<Post> findByAuthorNameAndReviewStatusOrderByCreatedAtDesc(String authorName, String reviewStatus);
}
