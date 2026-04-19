package com.example.xinqiaobackend.service;

import com.example.xinqiaobackend.dto.CommentDto;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    /**
     * 格式化相对时间
     * @param createdAt 创建时间
     * @return 格式化后的时间字符串
     */
    private String formatRelativeTime(Instant createdAt) {
        if (createdAt == null) {
            return "刚刚";
        }
        
        Instant now = Instant.now();
        long diffSeconds = now.getEpochSecond() - createdAt.getEpochSecond();
        
        // 小于1分钟
        if (diffSeconds < 60) {
            return "刚刚";
        }
        
        // 小于1小时
        long diffMinutes = diffSeconds / 60;
        if (diffMinutes < 60) {
            return diffMinutes + "分钟前";
        }
        
        // 小于24小时
        long diffHours = diffMinutes / 60;
        if (diffHours < 24) {
            return diffHours + "小时前";
        }
        
        // 小于7天
        long diffDays = diffHours / 24;
        if (diffDays < 7) {
            return diffDays + "天前";
        }
        
        // 7天及以上，显示具体日期时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());
        return formatter.format(createdAt);
    }

    // 移除缓存，因为点赞状态是用户相关的，不适合缓存
    // @Cacheable(value = "posts", key = "((#category == null || #category.trim().isEmpty()) ? '__ALL__' : #category) + '-' + #page + '-' + #size")
    public List<PostDto> getPosts(String category, int page, int size) {
        System.out.println("DEBUG: getPosts called - category: " + category + ", page: " + page + ", size: " + size);
        
        // 获取当前登录用户
        String currentUsername = null;
        com.example.xinqiaobackend.entity.User currentUser = null;
        try {
            currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            System.out.println("DEBUG: Current username from SecurityContext: " + currentUsername);
            if (currentUsername != null && !currentUsername.equals("anonymousUser")) {
                currentUser = userRepository.findByUsername(currentUsername).orElse(null);
                System.out.println("DEBUG: Current user found: " + (currentUser != null ? currentUser.getId() : "null"));
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting current user: " + e.getMessage());
            // 未登录或获取用户失败，继续处理
        }
        
        List<Post> posts = new ArrayList<>();
        
        // 只在第一页时添加用户自己的待审核和被拒绝的帖子
        if (page == 0 && currentUser != null && currentUsername != null) {
            System.out.println("DEBUG: Adding user's PENDING and REJECTED posts for user: " + currentUsername);
            
            // 获取用户昵称
            String userNickname = currentUser.getNickname();
            System.out.println("DEBUG: User nickname: " + userNickname);
            
            // 添加用户的待审核帖子（使用用户名或昵称查询）
            List<Post> userPendingPosts = new ArrayList<>();
            userPendingPosts.addAll(postRepository.findByAuthorNameAndReviewStatusOrderByCreatedAtDesc(currentUsername, "PENDING"));
            if (userNickname != null && !userNickname.equals(currentUsername)) {
                userPendingPosts.addAll(postRepository.findByAuthorNameAndReviewStatusOrderByCreatedAtDesc(userNickname, "PENDING"));
            }
            
            // 去重PENDING帖子
            java.util.Set<Long> pendingIds = new java.util.HashSet<>();
            for (Post p : userPendingPosts) {
                if (!pendingIds.contains(p.getId())) {
                    posts.add(p);
                    pendingIds.add(p.getId());
                }
            }
            System.out.println("DEBUG: Added " + posts.size() + " unique PENDING posts");
            
            // 添加用户的被拒绝帖子（使用用户名或昵称查询）
            List<Post> userRejectedPosts = new ArrayList<>();
            userRejectedPosts.addAll(postRepository.findByAuthorNameAndReviewStatusOrderByCreatedAtDesc(currentUsername, "REJECTED"));
            if (userNickname != null && !userNickname.equals(currentUsername)) {
                userRejectedPosts.addAll(postRepository.findByAuthorNameAndReviewStatusOrderByCreatedAtDesc(userNickname, "REJECTED"));
            }
            
            // 去重REJECTED帖子
            java.util.Set<Long> rejectedIds = new java.util.HashSet<>();
            for (Post p : userRejectedPosts) {
                if (!rejectedIds.contains(p.getId()) && !pendingIds.contains(p.getId())) {
                    posts.add(p);
                    rejectedIds.add(p.getId());
                }
            }
            System.out.println("DEBUG: Added " + rejectedIds.size() + " unique REJECTED posts");
        }
        
        // 获取已审核通过的帖子
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        List<Post> approvedPosts;
        if (category != null && category.trim().length() > 0) {
            approvedPosts = postRepository.findByCategoryAndReviewStatusOrderByCreatedAtDesc(category.trim(), "APPROVED", pageable).getContent();
        } else {
            approvedPosts = postRepository.findByReviewStatusOrderByCreatedAtDesc("APPROVED", pageable).getContent();
        }
        
        // 合并APPROVED帖子，去重
        java.util.Set<Long> existingIds = new java.util.HashSet<>();
        for (Post p : posts) {
            existingIds.add(p.getId());
        }
        for (Post p : approvedPosts) {
            if (!existingIds.contains(p.getId())) {
                posts.add(p);
                existingIds.add(p.getId());
            }
        }
        
        System.out.println("DEBUG: Total posts after merging: " + posts.size());
        
        List<PostDto> dtos = new ArrayList<>();
        for (Post p : posts) {
            // 动态计算点赞数和评论数
            int likeCount = (int) postLikeRepository.countByPost(p);
            int commentCount = (int) postCommentRepository.countByPost(p);
            
            // 检查当前用户是否已点赞
            boolean liked = false;
            if (currentUser != null) {
                liked = postLikeRepository.existsByPostAndUser(p, currentUser);
                System.out.println("DEBUG: Post " + p.getId() + " - User " + currentUser.getId() + " liked: " + liked + ", likeCount: " + likeCount);
            } else {
                System.out.println("DEBUG: Post " + p.getId() + " - No current user, likeCount: " + likeCount);
            }
            
            PostDto dto = new PostDto(
                    String.valueOf(p.getId()),
                    p.isAnonymous() ? "匿名用户" : (p.getAuthorName() != null ? p.getAuthorName() : "我"),
                    p.getAuthorName(),
                    p.getAuthorAvatar(),
                    p.isAnonymous(),
                    formatRelativeTime(p.getCreatedAt()),
                    p.getTitle(),
                    p.getContent(),
                    p.getTags(),
                    p.getImages(),
                    null,
                    p.getCreatedAt().toEpochMilli(),
                    likeCount,
                    commentCount,
                    liked,
                    p.getReviewStatus()
            );
            dtos.add(dto);
        }
        
        // 调试：打印返回的帖子ID列表
        System.out.println("DEBUG: getPosts returning " + dtos.size() + " posts with IDs: " + 
            dtos.stream().map(PostDto::getId).collect(java.util.stream.Collectors.joining(", ")));
        
        return dtos;
    }

    @CacheEvict(value = "posts", allEntries = true)
    public PostDto createPost(String title, String content, List<String> tags, List<String> images, boolean anonymous, String authorName, String authorAvatar) {
        System.out.println("DEBUG: createPost called - title: " + title + ", images: " + images);
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
        p.setReviewStatus("PENDING"); // 新帖子默认待审核
        Post saved = postRepository.save(p);
        System.out.println("DEBUG: Post saved - id: " + saved.getId() + ", images: " + saved.getImages());
        return new PostDto(
                String.valueOf(saved.getId()),
                saved.isAnonymous() ? "匿名用户" : (saved.getAuthorName() != null ? saved.getAuthorName() : "我"),
                saved.getAuthorName(),
                saved.getAuthorAvatar(),
                saved.isAnonymous(),
                formatRelativeTime(saved.getCreatedAt()),
                saved.getTitle(),
                saved.getContent(),
                saved.getTags(),
                saved.getImages(),
                null,
                saved.getCreatedAt().toEpochMilli(),
                0,
                0,
                false,
                saved.getReviewStatus()
        );
    }

    public List<CommentDto> getPostComments(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("post not found"));
        List<PostComment> list = postCommentRepository.findByPostOrderByCreatedAtAsc(post);
        List<CommentDto> dtos = new ArrayList<>();
        for (PostComment c : list) {
            String displayName = c.getAuthorName() != null ? c.getAuthorName() : "我";
            
            // 尝试获取用户昵称
            if (c.getAuthorName() != null && !c.getAuthorName().isEmpty()) {
                try {
                    com.example.xinqiaobackend.entity.User user = userRepository.findByUsername(c.getAuthorName()).orElse(null);
                    if (user != null && user.getNickname() != null && !user.getNickname().isEmpty()) {
                        displayName = user.getNickname();
                    }
                } catch (Exception e) {
                    // 如果查询失败，使用原始的 authorName
                    System.out.println("DEBUG: Failed to get nickname for user: " + c.getAuthorName() + ", error: " + e.getMessage());
                }
            }
            
            dtos.add(new CommentDto(String.valueOf(c.getId()), displayName, c.getContent()));
        }
        return dtos;
    }

    public CommentDto addPostComment(Long postId, String author, String text, Long parentId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("post not found"));
        PostComment c = new PostComment();
        c.setPost(post);
        c.setAuthorName(author);
        c.setContent(text != null ? text : "");
        if (parentId != null) {
            postCommentRepository.findById(parentId).ifPresent(c::setParent);
        }
        PostComment saved = postCommentRepository.save(c);
        
        // 获取用户昵称
        String displayName = saved.getAuthorName() != null ? saved.getAuthorName() : "我";
        if (saved.getAuthorName() != null && !saved.getAuthorName().isEmpty()) {
            try {
                com.example.xinqiaobackend.entity.User user = userRepository.findByUsername(saved.getAuthorName()).orElse(null);
                if (user != null && user.getNickname() != null && !user.getNickname().isEmpty()) {
                    displayName = user.getNickname();
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Failed to get nickname for user: " + saved.getAuthorName() + ", error: " + e.getMessage());
            }
        }
        
        return new CommentDto(String.valueOf(saved.getId()), displayName, saved.getContent());
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

    @CacheEvict(value = "posts", allEntries = true)
    public boolean deletePost(Long postId, String username) {
        try {
            Post post = postRepository.findById(postId).orElse(null);
            if (post == null) {
                return false;
            }
            
            // 验证权限：只有作者可以删除
            com.example.xinqiaobackend.entity.User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return false;
            }
            
            // 检查是否是作者（比较用户名或昵称）
            boolean isAuthor = false;
            if (post.getAuthorName() != null) {
                isAuthor = post.getAuthorName().equalsIgnoreCase(username) || 
                          post.getAuthorName().equalsIgnoreCase(user.getNickname());
            }
            
            if (!isAuthor) {
                System.out.println("DEBUG: User " + username + " is not the author of post " + postId);
                return false;
            }
            
            // 删除相关的点赞和评论
            postLikeRepository.deleteByPost(post);
            postCommentRepository.deleteByPost(post);
            
            // 删除帖子
            postRepository.delete(post);
            return true;
        } catch (Exception e) {
            System.out.println("DEBUG: Error deleting post: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @CacheEvict(value = "posts", allEntries = true)
    public PostDto updatePost(Long postId, String title, String content, List<String> tags, String username) {
        try {
            Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("post not found"));
            
            // 验证权限：只有作者可以编辑
            com.example.xinqiaobackend.entity.User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("user not found"));
            
            // 检查是否是作者
            boolean isAuthor = false;
            if (post.getAuthorName() != null) {
                isAuthor = post.getAuthorName().equalsIgnoreCase(username) || 
                          post.getAuthorName().equalsIgnoreCase(user.getNickname());
            }
            
            if (!isAuthor) {
                throw new IllegalArgumentException("Only author can update post");
            }
            
            // 更新帖子
            post.setTitle(title != null && !title.trim().isEmpty() ? title : "未命名");
            post.setContent(content != null ? content : "");
            post.setTags(tags != null ? tags : new ArrayList<>());
            if (tags != null && !tags.isEmpty()) {
                String primary = tags.get(0);
                if (primary != null && primary.trim().length() > 0) {
                    post.setCategory(primary.trim());
                }
            }
            
            Post saved = postRepository.save(post);
            
            // 计算点赞数和评论数
            int likeCount = (int) postLikeRepository.countByPost(saved);
            int commentCount = (int) postCommentRepository.countByPost(saved);
            boolean liked = postLikeRepository.existsByPostAndUser(saved, user);
            
            return new PostDto(
                    String.valueOf(saved.getId()),
                    saved.isAnonymous() ? "匿名用户" : (saved.getAuthorName() != null ? saved.getAuthorName() : "我"),
                    saved.getAuthorName(),
                    saved.getAuthorAvatar(),
                    saved.isAnonymous(),
                    formatRelativeTime(saved.getCreatedAt()),
                    saved.getTitle(),
                    saved.getContent(),
                    saved.getTags(),
                    saved.getImages(),
                    null,
                    saved.getCreatedAt().toEpochMilli(),
                    likeCount,
                    commentCount,
                    liked
            );
        } catch (Exception e) {
            System.out.println("DEBUG: Error updating post: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean reviewPost(Long postId, String status) {
        try {
            Post post = postRepository.findById(postId).orElse(null);
            if (post == null) {
                return false;
            }
            
            // 验证状态值
            if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
                return false;
            }
            
            post.setReviewStatus(status);
            postRepository.save(post);
            return true;
        } catch (Exception e) {
            System.out.println("DEBUG: Error reviewing post: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public List<PostDto> getPendingPosts(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        List<Post> posts = postRepository.findByReviewStatusOrderByCreatedAtDesc("PENDING", pageable).getContent();
        
        List<PostDto> dtos = new ArrayList<>();
        for (Post p : posts) {
            int likeCount = (int) postLikeRepository.countByPost(p);
            int commentCount = (int) postCommentRepository.countByPost(p);
            
            PostDto dto = new PostDto(
                    String.valueOf(p.getId()),
                    p.isAnonymous() ? "匿名用户" : (p.getAuthorName() != null ? p.getAuthorName() : "我"),
                    p.getAuthorName(),
                    p.getAuthorAvatar(),
                    p.isAnonymous(),
                    formatRelativeTime(p.getCreatedAt()),
                    p.getTitle(),
                    p.getContent(),
                    p.getTags(),
                    p.getImages(),
                    null,
                    p.getCreatedAt().toEpochMilli(),
                    likeCount,
                    commentCount,
                    false,
                    p.getReviewStatus()
            );
            dtos.add(dto);
        }
        return dtos;
    }
    
    public List<PostDto> getAllPostsForAdmin(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        List<Post> posts;
        
        // 如果指定了状态，只返回该状态的帖子；否则返回所有帖子
        if (status != null && !status.trim().isEmpty()) {
            posts = postRepository.findByReviewStatusOrderByCreatedAtDesc(status.trim(), pageable).getContent();
        } else {
            posts = postRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
        }
        
        List<PostDto> dtos = new ArrayList<>();
        for (Post p : posts) {
            int likeCount = (int) postLikeRepository.countByPost(p);
            int commentCount = (int) postCommentRepository.countByPost(p);
            
            PostDto dto = new PostDto(
                    String.valueOf(p.getId()),
                    p.isAnonymous() ? "匿名用户" : (p.getAuthorName() != null ? p.getAuthorName() : "我"),
                    p.getAuthorName(),
                    p.getAuthorAvatar(),
                    p.isAnonymous(),
                    formatRelativeTime(p.getCreatedAt()),
                    p.getTitle(),
                    p.getContent(),
                    p.getTags(),
                    p.getImages(),
                    null,
                    p.getCreatedAt().toEpochMilli(),
                    likeCount,
                    commentCount,
                    false,
                    p.getReviewStatus()
            );
            dtos.add(dto);
        }
        return dtos;
    }
}
