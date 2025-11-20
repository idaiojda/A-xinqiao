package com.example.xinqiaobackend.service;

import com.example.xinqiaobackend.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCommunityService implements CommunityService {
    private final List<String> groups = new ArrayList<>(Arrays.asList("社恐成长圈", "恋爱关系修复"));
    private final List<PostDto> allPosts = new ArrayList<>();
    private final Map<String, CacheEntry> cacheByCategory = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L;
    private final AtomicLong lastUpdated = new AtomicLong(System.currentTimeMillis());
    private final List<QuestionDto> questions = Arrays.asList(
            new QuestionDto("q1", "Q1. 如何面对考试焦虑？", "匿名用户：尝试建立规律作息，适度运动，分解任务，并进行深呼吸练习。"),
            new QuestionDto("q2", "Q2. 社交恐惧如何改善？", "匿名用户：从小场景练习开始，逐步暴露并记录积极体验。"),
            new QuestionDto("q3", "Q3. 失眠怎么办？", "匿名用户：避免睡前使用电子设备，建立固定的睡眠仪式。"),
            new QuestionDto("q4", "Q4. 恋爱关系中的安全感如何建立？", "匿名用户：明确需求边界，保持开放沟通与同理心。")
    );

    private final Set<String> liked = ConcurrentHashMap.newKeySet();
    private final Set<String> collected = ConcurrentHashMap.newKeySet();
    private final Map<String, List<CommentDto>> commentsMap = new ConcurrentHashMap<>();
    private final Set<String> members = ConcurrentHashMap.newKeySet();

    public InMemoryCommunityService() {
        // 不初始化示例帖子：仅返回实际创建的帖子数据
        commentsMap.put("q1", new ArrayList<>(Arrays.asList(
                new CommentDto("c1", "匿名用户", "谢谢分享，很有帮助！"),
                new CommentDto("c2", "心理咨询师", "建议尝试渐进式放松法，配合认知重构。")
        )));
        commentsMap.put("q2", new ArrayList<>(Arrays.asList(
                new CommentDto("c3", "匿名用户", "循序渐进，记录每次进步。")
        )));
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
