package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.api.ApiResponse;
import com.example.xinqiaobackend.api.ErrorCode;
import com.example.xinqiaobackend.entity.CounselorProfile;
import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.repository.CounselorProfileRepository;
import com.example.xinqiaobackend.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/counselor/profile")
public class CounselorProfileController {
    private final CounselorProfileRepository repo;
    private final UserRepository userRepo;

    public CounselorProfileController(CounselorProfileRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @GetMapping("/{username}")
    public ApiResponse<CounselorProfile> publicProfile(@PathVariable String username) {
        java.util.Optional<CounselorProfile> opt = repo.findByUsername(username);
        if (!opt.isPresent()) return ApiResponse.error(ErrorCode.NOT_FOUND, "未找到");
        return ApiResponse.success(opt.get());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ApiResponse<CounselorProfile> myProfile(Authentication auth) {
        java.util.Optional<CounselorProfile> opt = repo.findByUsername(auth.getName());
        if (!opt.isPresent()) return ApiResponse.error(ErrorCode.NOT_FOUND, "未找到");
        return ApiResponse.success(opt.get());
    }

    @PostMapping
    @PreAuthorize("hasRole('COUNSELOR')")
    public ApiResponse<Object> update(Authentication auth, @RequestBody CounselorProfile payload) {
        java.util.Optional<CounselorProfile> opt = repo.findByUsername(auth.getName());
        CounselorProfile p = opt.orElseGet(() -> {
            User u = userRepo.findByUsername(auth.getName()).orElse(null);
            CounselorProfile np = new CounselorProfile();
            if (u != null) { np.setUserId(u.getId()); np.setUsername(u.getUsername()); }
            return np;
        });
        p.setTitle(payload.getTitle());
        p.setDefaultMode(payload.getDefaultMode());
        p.setBio(payload.getBio());
        p.setTags(payload.getTags());
        repo.save(p);
        return ApiResponse.successMessage("已更新");
    }
}