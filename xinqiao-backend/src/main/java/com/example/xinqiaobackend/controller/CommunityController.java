package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.model.*;
import com.example.xinqiaobackend.service.CommunityService;
import com.example.xinqiaobackend.service.InMemoryCommunityService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/community", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommunityController {
    private final CommunityService service = new InMemoryCommunityService();

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
}
