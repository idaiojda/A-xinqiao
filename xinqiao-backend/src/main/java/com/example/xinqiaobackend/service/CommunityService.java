package com.example.xinqiaobackend.service;

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
    QuestionDto createQuestion(String title, String content);
    List<TimelineItemDto> getMyTimeline();
    HealthDto health();
}
