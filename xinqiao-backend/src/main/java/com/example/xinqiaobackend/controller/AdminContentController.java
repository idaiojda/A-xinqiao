package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.Article;
import com.example.xinqiaobackend.entity.Course;
import com.example.xinqiaobackend.entity.Post;
import com.example.xinqiaobackend.repository.ArticleRepository;
import com.example.xinqiaobackend.repository.CourseRepository;
import com.example.xinqiaobackend.repository.PostRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/content")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {
    private final ArticleRepository articleRepo;
    private final CourseRepository courseRepo;
    private final com.example.xinqiaobackend.repository.CourseLessonRepository lessonRepo;
    private final PostRepository postRepo;

    public AdminContentController(ArticleRepository articleRepo, CourseRepository courseRepo, 
                                 com.example.xinqiaobackend.repository.CourseLessonRepository lessonRepo,
                                 PostRepository postRepo) {
        this.articleRepo = articleRepo;
        this.courseRepo = courseRepo;
        this.lessonRepo = lessonRepo;
        this.postRepo = postRepo;
    }

    @GetMapping("/articles")
    public List<Article> listArticles(@RequestParam(required = false) String status) {
        return status == null ? articleRepo.findAll() : articleRepo.findByReviewStatus(status);
    }

    @GetMapping("/articles/{id}")
    public com.example.xinqiaobackend.api.ApiResponse<Article> articleDetail(@PathVariable Long id) {
        Article a = articleRepo.findById(id).orElse(null);
        if (a == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        return com.example.xinqiaobackend.api.ApiResponse.success(a);
    }

    @PostMapping("/articles/{id}/approve")
    public com.example.xinqiaobackend.api.ApiResponse<Object> approveArticle(@PathVariable Long id) {
        Article a = articleRepo.findById(id).orElse(null);
        if (a == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        a.setReviewStatus("APPROVED");
        articleRepo.save(a);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已通过");
    }

    @PostMapping("/articles/{id}/reject")
    public com.example.xinqiaobackend.api.ApiResponse<Object> rejectArticle(@PathVariable Long id) {
        Article a = articleRepo.findById(id).orElse(null);
        if (a == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        a.setReviewStatus("REJECTED");
        articleRepo.save(a);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已驳回");
    }

    @GetMapping("/posts")
    public List<Post> listPosts(@RequestParam(required = false) String status) {
        return status == null ? postRepo.findAll() : postRepo.findByReviewStatus(status);
    }

    @GetMapping("/posts/{id}")
    public com.example.xinqiaobackend.api.ApiResponse<Post> postDetail(@PathVariable Long id) {
        Post p = postRepo.findById(id).orElse(null);
        if (p == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        return com.example.xinqiaobackend.api.ApiResponse.success(p);
    }

    @PostMapping("/posts/{id}/approve")
    public com.example.xinqiaobackend.api.ApiResponse<Object> approvePost(@PathVariable Long id) {
        Post p = postRepo.findById(id).orElse(null);
        if (p == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        p.setReviewStatus("APPROVED");
        postRepo.save(p);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已通过");
    }

    @PostMapping("/posts/{id}/reject")
    public com.example.xinqiaobackend.api.ApiResponse<Object> rejectPost(@PathVariable Long id) {
        Post p = postRepo.findById(id).orElse(null);
        if (p == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        p.setReviewStatus("REJECTED");
        postRepo.save(p);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已驳回");
    }

    // Course review endpoints
    @GetMapping("/courses")
    public com.example.xinqiaobackend.api.ApiResponse<List<com.example.xinqiaobackend.dto.CourseWithLessonsDto>> listCourses(@RequestParam(required = false) String status) {
        List<Course> courses = status == null ? courseRepo.findAll() : courseRepo.findByReviewStatus(status);
        List<com.example.xinqiaobackend.dto.CourseWithLessonsDto> dtos = courses.stream().map(course -> {
            int lessonCount = lessonRepo.findByCourseIdOrderBySortOrderAsc(course.getId()).size();
            return new com.example.xinqiaobackend.dto.CourseWithLessonsDto(course, lessonCount);
        }).collect(java.util.stream.Collectors.toList());
        return com.example.xinqiaobackend.api.ApiResponse.success(dtos);
    }

    @GetMapping("/courses/{id}")
    public com.example.xinqiaobackend.api.ApiResponse<Course> courseDetail(@PathVariable Long id) {
        Course c = courseRepo.findById(id).orElse(null);
        if (c == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        return com.example.xinqiaobackend.api.ApiResponse.success(c);
    }

    @PostMapping("/courses/{id}/approve")
    public com.example.xinqiaobackend.api.ApiResponse<Object> approveCourse(@PathVariable Long id) {
        Course c = courseRepo.findById(id).orElse(null);
        if (c == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        c.setReviewStatus("APPROVED");
        courseRepo.save(c);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已通过");
    }

    @PostMapping("/courses/{id}/reject")
    public com.example.xinqiaobackend.api.ApiResponse<Object> rejectCourse(@PathVariable Long id) {
        Course c = courseRepo.findById(id).orElse(null);
        if (c == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        c.setReviewStatus("REJECTED");
        courseRepo.save(c);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已驳回");
    }
}