package com.example.xinqiaobackend.service;

import com.example.xinqiaobackend.dto.CommentDto;
import com.example.xinqiaobackend.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCommunityService implements CommunityService {
    private final List<String> groups = new ArrayList<>(); // 清空静态数据
    private final List<PostDto> allPosts = new ArrayList<>();
    private final Map<String, CacheEntry> cacheByCategory = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L;
    private final AtomicLong lastUpdated = new AtomicLong(System.currentTimeMillis());
    private final List<QuestionDto> questions = new ArrayList<>(); // 清空静态数据

    private final Set<String> liked = ConcurrentHashMap.newKeySet();
    private final Set<String> collected = ConcurrentHashMap.newKeySet();
    private final Map<String, List<CommentDto>> commentsMap = new ConcurrentHashMap<>();
    private final Set<String> members = ConcurrentHashMap.newKeySet();

    public InMemoryCommunityService() {
        // 不初始化任何静态数据，所有数据来自数据库
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    @Override
    public List<QuestionDto> searchQuestions(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return questions;
        List<QuestionDto> res = new ArrayList<>();
        for (QuestionDto dto : questions) {
            if (dto.getTitle().contains(q) || dto.getContent().contains(q)) res.add(dto);
        }
        return res;
    }

    @Override
    public QuestionStatusDto getStatus(String id) {
        return new QuestionStatusDto(liked.contains(id), collected.contains(id));
    }

    @Override
    public List<CommentDto> getComments(String id) {
        return new ArrayList<>(commentsMap.getOrDefault(id, Collections.emptyList()));
    }

    @Override
    public CommentDto addComment(String id, String author, String text) {
        CommentDto c = new CommentDto("c" + System.currentTimeMillis(), author, text);
        commentsMap.computeIfAbsent(id, k -> new ArrayList<>()).add(c);
        return c;
    }

    @Override
    public ToggleResultDto toggleLike(String id) {
        if (!liked.add(id)) liked.remove(id);
        return new ToggleResultDto(liked.contains(id), collected.contains(id));
    }

    @Override
    public ToggleResultDto toggleCollect(String id) {
        if (!collected.add(id)) collected.remove(id);
        return new ToggleResultDto(liked.contains(id), collected.contains(id));
    }

    // 新增：申请加入
    @Override
    public GroupApplyResultDto applyJoin(String groupName) {
        boolean added = members.add(groupName);
        String msg = added ? "已申请加入：" + groupName : "已在申请中或已加入：" + groupName;
        return new GroupApplyResultDto(added, msg);
    }

    // 新增：创建问题
    @Override
    public QuestionDto createQuestion(String title, String content) {
        String id = "q" + System.currentTimeMillis();
        QuestionDto q = new QuestionDto(id, title != null ? title : "匿名提问", content != null ? content : "");
        // 简单地把新问题加入首位，模拟新增
        List<QuestionDto> newList = new ArrayList<>(questions);
        newList.add(0, q);
        // 由于 questions 是固定 List，这里不替换原引用，仅返回创建结果
        return q;
    }

    // 新增：我的时间线
    @Override
    public List<TimelineItemDto> getMyTimeline() {
        List<TimelineItemDto> items = new ArrayList<>();
        long now = System.currentTimeMillis();
        items.add(new TimelineItemDto("checkin", "今日完成冥想 10 分钟", now - 3_600_000L));
        items.add(new TimelineItemDto("share", "与组员分享了缓解失眠的音乐", now - 7_200_000L));
        items.add(new TimelineItemDto("badge", "获得‘坚持打卡’徽章", now - 86_400_000L));
        return items;
    }

    // 新增：健康检查
    @Override
    public HealthDto health() {
        return new HealthDto(true, "OK");
    }

    // 新增：主题交流区帖子流
    @Override
    public List<PostDto> getPosts(String category, int page, int size) {
        String key = (category == null || category.trim().isEmpty()) ? "__ALL__" : category.trim();
        CacheEntry entry = cacheByCategory.get(key);
        long now = System.currentTimeMillis();
        boolean needRebuild = entry == null || (now - entry.builtAt) >= CACHE_TTL_MILLIS || entry.builtAt < lastUpdated.get();
        if (needRebuild) {
            List<PostDto> filtered;
            if ("__ALL__".equals(key)) {
                filtered = new ArrayList<>(allPosts);
            } else {
                filtered = new ArrayList<>();
                for (PostDto p : allPosts) {
                    if (p.getTags() != null && p.getTags().stream().anyMatch(t -> t.contains(key))) {
                        filtered.add(p);
                    }
                }
                if (filtered.isEmpty()) filtered = new ArrayList<>(allPosts);
            }
            filtered.sort(Comparator.comparingLong(PostDto::getCreatedAtMillis).reversed());
            CacheEntry newEntry = new CacheEntry(Collections.unmodifiableList(filtered), now);
            cacheByCategory.put(key, newEntry);
            entry = newEntry;
        }
        List<PostDto> source = entry.snapshot;
        int from = Math.max(0, page * Math.max(1, size));
        int to = Math.min(source.size(), from + Math.max(1, size));
        if (from >= source.size()) return Collections.emptyList();
        return source.subList(from, to);
    }

    @Override
    public PostDto createPost(String title, String content, List<String> tags, List<String> images, boolean anonymous, String authorName, String authorAvatar) {
        String id = "p" + System.currentTimeMillis();
        String author = anonymous ? "匿名用户" : (authorName != null && !authorName.trim().isEmpty() ? authorName.trim() : "我");
        PostDto p = new PostDto(id, author, authorName != null ? authorName : author, authorAvatar, anonymous, "刚刚", title != null && !title.trim().isEmpty() ? title : "未命名", content != null ? content : "", tags != null ? tags : Collections.emptyList(), images != null ? images : Collections.emptyList(), null, System.currentTimeMillis());
        allPosts.add(0, p);
        lastUpdated.set(System.currentTimeMillis());
        cacheByCategory.clear();
        return p;
    }

    private static final class CacheEntry {
        final List<PostDto> snapshot;
        final long builtAt;
        CacheEntry(List<PostDto> snapshot, long builtAt) { this.snapshot = snapshot; this.builtAt = builtAt; }
    }

    // 新增：创建小组
    @Override
    public GroupCreateResultDto createGroup(String name, String description, String schedule, int capacity) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            return new GroupCreateResultDto(false, "小组名称不能为空");
        }
        if (groups.contains(n)) {
            return new GroupCreateResultDto(false, "小组已存在：" + n);
        }
        groups.add(n);
        return new GroupCreateResultDto(true, "已创建：" + n);
    }
}
