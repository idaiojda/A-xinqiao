package com.example.xinqiaobackend.service;

import com.example.xinqiaobackend.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCommunityService implements CommunityService {
    private final List<String> groups = new ArrayList<>(Arrays.asList("考研互助小组", "社恐成长圈", "恋爱关系修复"));
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
        List<PostDto> all = new ArrayList<>();
        all.add(new PostDto(
                "p1",
                "小桥",
                true,
                "刚刚",
                "夜深时的情绪波动怎么办？",
                "最近晚上总是心跳加快、脑子停不下来。尝试了呼吸练习有一点帮助，但还是会被突如其来的焦虑击中。大家有什么实用的办法吗？",
                Arrays.asList("夜间情绪", "焦虑"),
                3,
                null
        ));
        all.add(new PostDto(
                "p2",
                "明月",
                true,
                "1 小时前",
                "和室友相处的边界感",
                "室友总是会进入我的私人空间，虽然不是恶意，但我会紧张。想学习如何更自然地表达界限又不伤害关系。",
                Arrays.asList("社交与关系", "边界"),
                0,
                28
        ));
        all.add(new PostDto(
                "p3",
                "安然",
                true,
                "2 小时前",
                "今天的呼吸练习让我慢下来",
                "跟着 4-7-8 的节奏做了 5 轮，心态竟然平稳了不少。记录一下这份改变。",
                Arrays.asList("呼吸练习", "自我关怀"),
                1,
                null
        ));
        all.add(new PostDto(
                "p4",
                "灯下客",
                true,
                "昨天",
                "晚间散步的声音",
                "录了一段路上的环境音和自己的心情，舒服。",
                Arrays.asList("夜间情绪"),
                0,
                16
        ));

        // 按类别过滤（若传入）
        List<PostDto> filtered;
        if (category == null || category.trim().isEmpty()) {
            filtered = all;
        } else {
            String cat = category.trim();
            filtered = new ArrayList<>();
            for (PostDto p : all) {
                if (p.getTags() != null && p.getTags().stream().anyMatch(t -> t.contains(cat))) {
                    filtered.add(p);
                }
            }
            if (filtered.isEmpty()) filtered = all; // 若无匹配，回退全部
        }

        // 简单分页
        int from = Math.max(0, page * Math.max(1, size));
        int to = Math.min(filtered.size(), from + Math.max(1, size));
        if (from >= filtered.size()) return Collections.emptyList();
        return filtered.subList(from, to);
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
