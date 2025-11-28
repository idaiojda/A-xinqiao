package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.api.ApiResponse;
import com.example.xinqiaobackend.api.PageResponse;
import com.example.xinqiaobackend.dto.UserDto;
import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.entity.UserInfo;
import com.example.xinqiaobackend.repository.UserRepository;
import com.example.xinqiaobackend.repository.UserInfoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.persistence.EntityManager;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class AdminUserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserInfoRepository userInfoRepository;
    private final EntityManager entityManager;

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder, UserInfoRepository userInfoRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userInfoRepository = userInfoRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        List<User> allUsers = userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<UserDto> items = new ArrayList<>();
        for (User u : allUsers) {
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
        List<UserInfo> infos = userInfoRepository.search(username);
        for (UserInfo info : infos) {
            UserDto d = new UserDto();
            d.setId(info.getUserId() != null ? info.getUserId().longValue() : null);
            d.setUsername(info.getUsername());
            d.setRole("user");
            String s = "inactive";
            d.setStatus(s);
            d.setCreatedAt(info.getCreatedAt() != null ? info.getCreatedAt().toString() : "");
            d.setUpdatedAt(info.getUpdatedAt() != null ? info.getUpdatedAt().toString() : "");
            boolean exists = items.stream().anyMatch(x -> x.getUsername() != null && x.getUsername().equalsIgnoreCase(d.getUsername()));
            if (!exists) {
                if (role != null && role.trim().length() > 0 && !"user".equalsIgnoreCase(role.trim())) continue;
                if (status != null && status.trim().length() > 0 && !status.equalsIgnoreCase(d.getStatus())) continue;
                items.add(d);
            }
        }
        int total = items.size();
        int start = Math.max((page - 1) * size, 0);
        int end = Math.min(start + size, total);
        List<UserDto> sliced = start < end ? items.subList(start, end) : new ArrayList<>();
        PageResponse<UserDto> resp = new PageResponse<>(sliced, total, page, size);
        return ApiResponse.success(resp);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Object> syncFromUserInfo(@RequestParam(required = false) String role,
                                                @RequestParam(required = false) String status) {
        int created = 0, skipped = 0, failed = 0;
        try {
            @SuppressWarnings("unchecked")
            List<Object> rows = entityManager.createNativeQuery("SELECT username FROM user_info WHERE username IS NOT NULL").getResultList();
            for (Object obj : rows) {
                String uname = obj != null ? obj.toString() : null;
                if (uname == null) { failed++; continue; }
                String trimmed = uname.trim();
                if (trimmed.isEmpty()) { skipped++; continue; }
                if (trimmed.length() > 64) trimmed = trimmed.substring(0, 64);
                if (userRepository.existsByUsername(trimmed)) { skipped++; continue; }
                try {
                    User u = new User();
                    u.setUsername(trimmed);
                    u.setPassword(passwordEncoder.encode("xq-default-123456"));
                    List<String> roles = new ArrayList<>();
                    roles.add((role != null && role.trim().length() > 0) ? role.trim().toUpperCase() : "USER");
                    u.setRoles(roles);
                    u.setReviewStatus((status != null && status.trim().length() > 0) ? mapStatusReverse(status) : "APPROVED");
                    userRepository.save(u);
                    created++;
                } catch (Exception ex) {
                    failed++;
                }
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "同步失败: " + e.getMessage());
        }
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("created", created);
        res.put("skipped", skipped);
        res.put("failed", failed);
        return ApiResponse.success(res);
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
        public String phone;
        public String avatar;
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
        u.setPassword(passwordEncoder.encode("xq-default-123456"));
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
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        d.setCreatedAt(now.toString());
        d.setUpdatedAt(now.toString());
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

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Object> resetPassword(@PathVariable Long id) {
        Optional<User> opt = userRepository.findById(id);
        if (!opt.isPresent()) return ApiResponse.error(404, "用户不存在");
        User u = opt.get();
        String newPwd = java.util.UUID.randomUUID().toString().substring(0, 8);
        u.setPassword(passwordEncoder.encode(newPwd));
        userRepository.save(u);
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("newPassword", newPwd);
        return ApiResponse.success(res);
    }
}
