package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.Post;
import com.example.xinqiaobackend.entity.PostLike;
import com.example.xinqiaobackend.entity.PostLikeId;
import com.example.xinqiaobackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    boolean existsByPostAndUser(Post post, User user);
    long countByPost(Post post);
    int deleteByPostAndUser(Post post, User user);
    void deleteByPost(Post post);
}
