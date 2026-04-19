package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.dto.CommentDto;
import com.example.xinqiaobackend.model.*;
import com.example.xinqiaobackend.service.CommunityService;
import com.example.xinqiaobackend.service.InMemoryCommunityService;
import com.example.xinqiaobackend.service.JpaCommunityService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestController
@RequestMapping(path = "/community", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommunityController {
    private final CommunityService service;
    private final JpaCommunityService jpaService;

    @Autowired
    public CommunityController(JpaCommunityService jpaService) {
        this.jpaService = jpaService;
        this.service = jpaService;
    }

    @GetMapping("/groups")
    public List<String> groups() { return service.getGroups(); }

    @GetMapping("/questions")
    public List<QuestionDto> searchQuestions(@RequestParam(name = "query", required = false) String query) {
        return service.searchQuestions(query);
    }

    @GetMapping("/questions/{id}/status")
    public QuestionStatusDto status(@PathVariable("id") String id) { return service.getStatus(id); }

    @GetMapping("/questions/{id}/comments")
    public List<CommentDto> comments(@PathVariable("id") String id) { return service.getComments(id); }

    @PostMapping("/questions/{id}/comments")
    public CommentDto postComment(@PathVariable("id") String id, @RequestBody NewCommentRequest req) {
        String author = req.getAuthor() != null ? req.getAuthor() : "我";
        String text = req.getText() != null ? req.getText() : "";
        return service.addComment(id, author, text);
    }

    @PostMapping("/questions/{id}/toggle-like")
    public ToggleResultDto toggleLike(@PathVariable("id") String id) { return service.toggleLike(id); }

    @PostMapping("/questions/{id}/toggle-collect")
    public ToggleResultDto toggleCollect(@PathVariable("id") String id) { return service.toggleCollect(id); }

    // 新增：申请加入指定小组
    @PostMapping("/groups/{name}/apply")
    public GroupApplyResultDto apply(@PathVariable("name") String name) {
        return service.applyJoin(name);
    }

    // 新增：创建小组
    @PostMapping("/groups")
    public GroupCreateResultDto createGroup(@RequestBody CreateGroupRequest req) {
        String name = req.getName();
        String description = req.getDescription();
        String schedule = req.getSchedule();
        int capacity = req.getCapacity();
        return service.createGroup(name, description, schedule, capacity);
    }

    // 新增：创建匿名问题
    @PostMapping("/questions")
    public QuestionDto createQuestion(@RequestBody NewQuestionRequest req) {
        String title = req.getTitle();
        String content = req.getContent();
        return service.createQuestion(title, content);
    }

    // 新增：我的时间线
    @GetMapping("/timeline")
    public List<TimelineItemDto> timeline() { return service.getMyTimeline(); }

    // 新增：健康检查
    @GetMapping("/health")
    public HealthDto health() { return service.health(); }

    // 新增：主题交流区帖子流
    @GetMapping("/posts")
    public List<PostDto> posts(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return service.getPosts(category, page, size);
    }

    // 新增：创建帖子（所有客户端可见）
    @PostMapping("/posts")
    public PostDto createPost(@RequestBody com.example.xinqiaobackend.model.CreatePostRequest req) {
        String title = req.getTitle();
        String content = req.getContent();
        java.util.List<String> tags = req.getTags();
        java.util.List<String> images = req.getImages();
        boolean anonymous = req.isAnonymous();
        String authorName = req.getAuthorName();
        String authorAvatar = req.getAuthorAvatar();
        return service.createPost(title, content, tags, images, anonymous, authorName, authorAvatar);
    }

    @GetMapping("/posts/{postId}/comments")
    public java.util.List<CommentDto> postComments(@PathVariable("postId") Long postId) {
        return jpaService.getPostComments(postId);
    }

    @PostMapping("/posts/{postId}/comments")
    public CommentDto addPostComment(@PathVariable("postId") Long postId, @RequestBody NewCommentRequest req) {
        String author = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() : (req.getAuthor() != null ? req.getAuthor() : "我");
        String text = req.getText() != null ? req.getText() : "";
        return jpaService.addPostComment(postId, author, text, null);
    }

    @PostMapping("/posts/{postId}/like")
    public com.example.xinqiaobackend.model.FollowResultDto like(@PathVariable("postId") Long postId, @RequestParam(name = "on", defaultValue = "true") boolean on) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean ok = jpaService.setPostLike(postId, on, username);
        return new com.example.xinqiaobackend.model.FollowResultDto(ok);
    }

    @DeleteMapping("/posts/{postId}")
    public com.example.xinqiaobackend.model.FollowResultDto deletePost(@PathVariable("postId") Long postId) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean ok = jpaService.deletePost(postId, username);
        return new com.example.xinqiaobackend.model.FollowResultDto(ok);
    }

    @PutMapping("/posts/{postId}")
    public PostDto updatePost(@PathVariable("postId") Long postId, @RequestBody com.example.xinqiaobackend.model.UpdatePostRequest req) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return jpaService.updatePost(postId, req.getTitle(), req.getContent(), req.getTags(), username);
    }
    
    @PostMapping("/posts/{postId}/review")
    public com.example.xinqiaobackend.model.FollowResultDto reviewPost(
            @PathVariable("postId") Long postId, 
            @RequestParam(name = "status") String status) {
        boolean ok = jpaService.reviewPost(postId, status);
        return new com.example.xinqiaobackend.model.FollowResultDto(ok);
    }
    
    @GetMapping("/posts/pending")
    public List<PostDto> getPendingPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return jpaService.getPendingPosts(page, size);
    }
    
    @GetMapping("/posts/all")
    public List<PostDto> getAllPostsForAdmin(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return jpaService.getAllPostsForAdmin(status, page, size);
    }
}
