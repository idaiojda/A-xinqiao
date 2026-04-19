package com.example.xinqiaobackend.service;

import com.example.xinqiaobackend.dto.CommentDto;
import com.example.xinqiaobackend.model.*;
import java.util.*;

public interface CommunityService {
    List<String> getGroups();
    List<QuestionDto> searchQuestions(String query);
    QuestionStatusDto getStatus(String id);
    List<CommentDto> getComments(String id);
    CommentDto addComment(String id, String author, String text);
    ToggleResultDto toggleLike(String id);
    ToggleResultDto toggleCollect(String id);
    // 新增接口：申请加入、创建问题、时间线、健康检查
    GroupApplyResultDto applyJoin(String groupName);
    GroupCreateResultDto createGroup(String name, String description, String schedule, int capacity);
    QuestionDto createQuestion(String title, String content);
    List<TimelineItemDto> getMyTimeline();
    HealthDto health();
    // 新增接口：主题交流区帖子流
    List<PostDto> getPosts(String category, int page, int size);
    PostDto createPost(String title, String content, List<String> tags, List<String> images, boolean anonymous, String authorName, String authorAvatar);
}
