package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.api.ApiResponse;
import com.example.xinqiaobackend.api.PageResponse;
import com.example.xinqiaobackend.dto.UserDto;
import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class AdminUserController {
    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        Page<User> p = userRepository.findAll(PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.ASC, "id")));
        List<UserDto> items = new ArrayList<>();
        for (User u : p.getContent()) {
            if (username != null && username.trim().length() > 0 && (u.getUsername() == null || !u.getUsername().toLowerCase().contains(username.trim().toLowerCase()))) {
                continue;
            }
            if (role != null && role.trim().length() > 0) {
                boolean hasRole = u.getRoles() != null && u.getRoles().stream().anyMatch(r -> r.equalsIgnoreCase(role.trim()));
                if (!hasRole) continue;
            }
            if (status != null && status.trim().length() > 0) {
                String s = mapStatus(u.getReviewStatus());
                if (!status.equalsIgnoreCase(s)) continue;
            }
            items.add(toDto(u));
        }
        PageResponse<UserDto> resp = new PageResponse<>(items, p.getTotalElements(), page, size);
        return ApiResponse.success(resp);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDto> get(@PathVariable Long id) {
        Optional<User> opt = userRepository.findById(id);
        return opt.map(user -> ApiResponse.success(toDto(user)))
                .orElseGet(() -> ApiResponse.error(404, "用户不存在"));
    }

    public static class SaveUserPayload {
        public String username;
        public String role;
        public String status;
    }

    @PostMapping
    public ApiResponse<UserDto> create(@RequestBody SaveUserPayload payload) {
        if (payload.username == null || payload.username.trim().length() == 0) {
            return ApiResponse.error(400, "用户名不能为空");
        }
        if (userRepository.existsByUsername(payload.username)) {
            return ApiResponse.error(409, "用户名已存在");
        }
        User u = new User();
        u.setUsername(payload.username.trim());
        u.setPassword("$2a$10$defaultPasswordHashPlaceHolder");
        List<String> roles = new ArrayList<>();
        if (payload.role != null && payload.role.trim().length() > 0) roles.add(payload.role.trim().toUpperCase()); else roles.add("USER");
        u.setRoles(roles);
        if (payload.status != null) {
            u.setReviewStatus(mapStatusReverse(payload.status));
        }
        userRepository.save(u);
        return ApiResponse.success(toDto(u));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserDto> update(@PathVariable Long id, @RequestBody SaveUserPayload payload) {
        Optional<User> opt = userRepository.findById(id);
        if (!opt.isPresent()) return ApiResponse.error(404, "用户不存在");
        User u = opt.get();
        if (payload.role != null && payload.role.trim().length() > 0) {
            List<String> roles = new ArrayList<>();
            roles.add(payload.role.trim().toUpperCase());
            u.setRoles(roles);
        }
        if (payload.status != null) {
            u.setReviewStatus(mapStatusReverse(payload.status));
        }
        userRepository.save(u);
        return ApiResponse.success(toDto(u));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        if (!userRepository.existsById(id)) return ApiResponse.error(404, "用户不存在");
        userRepository.deleteById(id);
        return ApiResponse.successMessage("删除成功");
    }

    public static class BatchIds { public List<Long> ids; }

    @DeleteMapping("/batch")
    public ApiResponse<Object> batchDelete(@RequestBody BatchIds payload) {
        if (payload == null || payload.ids == null || payload.ids.isEmpty()) return ApiResponse.error(400, "无有效ID");
        userRepository.deleteAllById(payload.ids);
        return ApiResponse.successMessage("批量删除成功");
    }

    public static class StatusPayload { public String status; }

    @PutMapping("/{id}/status")
    public ApiResponse<UserDto> updateStatus(@PathVariable Long id, @RequestBody StatusPayload payload) {
        Optional<User> opt = userRepository.findById(id);
        if (!opt.isPresent()) return ApiResponse.error(404, "用户不存在");
        User u = opt.get();
        u.setReviewStatus(mapStatusReverse(payload.status));
        userRepository.save(u);
        return ApiResponse.success(toDto(u));
    }

    private UserDto toDto(User u) {
        UserDto d = new UserDto();
        d.setId(u.getId());
        d.setUsername(u.getUsername());
        String role = (u.getRoles() != null && !u.getRoles().isEmpty()) ? u.getRoles().get(0).toLowerCase() : "user";
        d.setRole(role);
        d.setStatus(mapStatus(u.getReviewStatus()));
        d.setCreatedAt("");
        d.setUpdatedAt("");
        return d;
    }

    private String mapStatus(String reviewStatus) {
        if (reviewStatus == null) return "inactive";
        switch (reviewStatus.toUpperCase()) {
            case "PENDING": return "pending";
            case "APPROVED": return "active";
            case "REJECTED": return "banned";
            default: return "inactive";
        }
    }

    private String mapStatusReverse(String status) {
        if (status == null) return "PENDING";
        switch (status.toLowerCase()) {
            case "pending": return "PENDING";
            case "active": return "APPROVED";
            case "banned": return "REJECTED";
            default: return "PENDING";
        }
    }
}