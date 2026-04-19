package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.Post;
import com.example.xinqiaobackend.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findByPostOrderByCreatedAtAsc(Post post);
    long countByPost(Post post);
    void deleteByPost(Post post);
}

