package com.example.xinqiaobackend.service;

import com.example.xinqiaobackend.entity.Post;
import com.example.xinqiaobackend.entity.PostComment;
import com.example.xinqiaobackend.model.*;
import com.example.xinqiaobackend.repository.PostCommentRepository;
import com.example.xinqiaobackend.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.ArrayList;
import java.util.List;

@Service
public class JpaCommunityService implements CommunityService {
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final com.example.xinqiaobackend.repository.PostLikeRepository postLikeRepository;
    private final com.example.xinqiaobackend.repository.UserRepository userRepository;
    private final InMemoryCommunityService fallback;

    public JpaCommunityService(PostRepository postRepository, PostCommentRepository postCommentRepository,
                               com.example.xinqiaobackend.repository.PostLikeRepository postLikeRepository,
                               com.example.xinqiaobackend.repository.UserRepository userRepository) {
        this.postRepository = postRepository;
        this.postCommentRepository = postCommentRepository;
        this.postLikeRepository = postLikeRepository;
        this.userRepository = userRepository;
        this.fallback = new InMemoryCommunityService();
    }

    public List<String> getGroups() { return fallback.getGroups(); }
    public List<QuestionDto> searchQuestions(String query) { return fallback.searchQuestions(query); }
    public QuestionStatusDto getStatus(String id) { return fallback.getStatus(id); }
    public List<CommentDto> getComments(String id) { return fallback.getComments(id); }
    public CommentDto addComment(String id, String author, String text) { return fallback.addComment(id, author, text); }
    public ToggleResultDto toggleLike(String id) { return fallback.toggleLike(id); }
    public ToggleResultDto toggleCollect(String id) { return fallback.toggleCollect(id); }
    public GroupApplyResultDto applyJoin(String groupName) { return fallback.applyJoin(groupName); }
    public GroupCreateResultDto createGroup(String name, String description, String schedule, int capacity) { return fallback.createGroup(name, description, schedule, capacity); }
    public QuestionDto createQuestion(String title, String content) { return fallback.createQuestion(title, content); }
    public List<TimelineItemDto> getMyTimeline() { return fallback.getMyTimeline(); }
    public HealthDto health() { return fallback.health(); }

    @Cacheable(value = "posts", key = "((#category == null || #category.trim().isEmpty()) ? '__ALL__' : #category) + '-' + #page + '-' + #size")
    public List<PostDto> getPosts(String category, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        List<Post> posts;
        if (category != null && category.trim().length() > 0) {
            posts = postRepository.findByCategoryOrderByCreatedAtDesc(category.trim(), pageable).getContent();
        } else {
            posts = postRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
        }
        List<PostDto> dtos = new ArrayList<>();
        for (Post p : posts) {
            PostDto dto = new PostDto(
                    String.valueOf(p.getId()),
                    p.isAnonymous() ? "匿名用户" : (p.getAuthorName() != null ? p.getAuthorName() : "我"),
                    p.getAuthorName(),
                    p.getAuthorAvatar(),
                    p.isAnonymous(),
                    "刚刚",
                    p.getTitle(),
                    p.getContent(),
                    p.getTags(),
                    p.getImages(),
                    null,
                    p.getCreatedAt().toEpochMilli()
            );
            dtos.add(dto);
        }
        return dtos;
    }

    @CacheEvict(value = "posts", allEntries = true)
    public PostDto createPost(String title, String content, List<String> tags, List<String> images, boolean anonymous, String authorName, String authorAvatar) {
        Post p = new Post();
        p.setTitle(title != null && !title.trim().isEmpty() ? title : "未命名");
        p.setContent(content != null ? content : "");
        p.setTags(tags != null ? tags : new ArrayList<>());
        if (tags != null && !tags.isEmpty()) {
            String primary = tags.get(0);
            if (primary != null && primary.trim().length() > 0) {
                p.setCategory(primary.trim());
            }
        }
        p.setImages(images != null ? images : new ArrayList<>());
        p.setAnonymous(anonymous);
        p.setAuthorName(authorName);
        p.setAuthorAvatar(authorAvatar);
        Post saved = postRepository.save(p);
        return new PostDto(
                String.valueOf(saved.getId()),
                saved.isAnonymous() ? "匿名用户" : (saved.getAuthorName() != null ? saved.getAuthorName() : "我"),
                saved.getAuthorName(),
                saved.getAuthorAvatar(),
                saved.isAnonymous(),
                "刚刚",
                saved.getTitle(),
                saved.getContent(),
                saved.getTags(),
                saved.getImages(),
                null,
                saved.getCreatedAt().toEpochMilli()
        );
    }

    public List<com.example.xinqiaobackend.model.CommentDto> getPostComments(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("post not found"));
        List<PostComment> list = postCommentRepository.findByPostOrderByCreatedAtAsc(post);
        List<com.example.xinqiaobackend.model.CommentDto> dtos = new ArrayList<>();
        for (PostComment c : list) {
            dtos.add(new com.example.xinqiaobackend.model.CommentDto(String.valueOf(c.getId()), c.getAuthorName() != null ? c.getAuthorName() : "我", c.getContent()));
        }
        return dtos;
    }

    public com.example.xinqiaobackend.model.CommentDto addPostComment(Long postId, String author, String text, Long parentId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("post not found"));
        PostComment c = new PostComment();
        c.setPost(post);
        c.setAuthorName(author);
        c.setContent(text != null ? text : "");
        if (parentId != null) {
            postCommentRepository.findById(parentId).ifPresent(c::setParent);
        }
        PostComment saved = postCommentRepository.save(c);
        return new com.example.xinqiaobackend.model.CommentDto(String.valueOf(saved.getId()), saved.getAuthorName() != null ? saved.getAuthorName() : "我", saved.getContent());
    }

    @org.springframework.cache.annotation.CacheEvict(value = "posts", allEntries = true)
    public boolean setPostLike(Long postId, boolean on, String username) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("post not found"));
        com.example.xinqiaobackend.entity.User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("user not found"));
        boolean exists = postLikeRepository.existsByPostAndUser(post, user);
        if (on) {
            if (!exists) {
                com.example.xinqiaobackend.entity.PostLike pl = new com.example.xinqiaobackend.entity.PostLike();
                pl.setPost(post);
                pl.setUser(user);
                postLikeRepository.save(pl);
            }
            return true;
        } else {
            if (exists) postLikeRepository.deleteByPostAndUser(post, user);
            return true;
        }
    }

    
}
