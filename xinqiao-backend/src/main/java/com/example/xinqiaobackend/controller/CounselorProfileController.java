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

    @GetMapping
    @PreAuthorize("hasRole('COUNSELOR')")
    public ApiResponse<CounselorProfile> getProfile(Authentication auth) {
        java.util.Optional<CounselorProfile> opt = repo.findByUsername(auth.getName());
        if (!opt.isPresent()) {
            // 返回空的profile，前端可以判断是否需要创建
            return ApiResponse.error(ErrorCode.NOT_FOUND, "未找到个人资料");
        }
        return ApiResponse.success(opt.get());
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
    public ApiResponse<CounselorProfile> update(Authentication auth, @RequestBody CounselorProfile payload) {
        try {
            String username = auth.getName();
            org.slf4j.LoggerFactory.getLogger(CounselorProfileController.class)
                .info("Updating profile for user: {}", username);
            
            // 查找现有的profile
            java.util.Optional<CounselorProfile> opt = repo.findByUsername(username);
            CounselorProfile p;
            
            if (opt.isPresent()) {
                // 更新现有profile
                p = opt.get();
                org.slf4j.LoggerFactory.getLogger(CounselorProfileController.class)
                    .info("Found existing profile with id: {}", p.getId());
            } else {
                // 创建新profile
                org.slf4j.LoggerFactory.getLogger(CounselorProfileController.class)
                    .info("Creating new profile for user: {}", username);
                
                User u = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
                
                p = new CounselorProfile();
                p.setUserId(u.getId());
                p.setUsername(u.getUsername());
                
                org.slf4j.LoggerFactory.getLogger(CounselorProfileController.class)
                    .info("New profile created with userId: {}, username: {}", u.getId(), u.getUsername());
            }
            
            // 更新所有字段
            if (payload.getDisplayName() != null) p.setDisplayName(payload.getDisplayName());
            if (payload.getCity() != null) p.setCity(payload.getCity());
            if (payload.getBriefIntro() != null) p.setBriefIntro(payload.getBriefIntro());
            if (payload.getEducation() != null) p.setEducation(payload.getEducation());
            if (payload.getWorkYears() != null) p.setWorkYears(payload.getWorkYears());
            if (payload.getDetailedIntro() != null) p.setDetailedIntro(payload.getDetailedIntro());
            if (payload.getAvatarBase64() != null) p.setAvatarBase64(payload.getAvatarBase64());
            if (payload.getTitle() != null) p.setTitle(payload.getTitle());
            if (payload.getDefaultMode() != null) p.setDefaultMode(payload.getDefaultMode());
            if (payload.getTags() != null) p.setTags(payload.getTags());
            
            // 更新定价字段
            if (payload.getPriceText() != null) p.setPriceText(payload.getPriceText());
            if (payload.getPriceVoice() != null) p.setPriceVoice(payload.getPriceVoice());
            if (payload.getPriceVideo() != null) p.setPriceVideo(payload.getPriceVideo());
            
            // 验证定价字段
            String validationError = validatePricing(p);
            if (validationError != null) {
                return ApiResponse.error(ErrorCode.BAD_REQUEST, validationError);
            }
            
            // 自动设置状态为已批准
            p.setStatus("approved");
            
            org.slf4j.LoggerFactory.getLogger(CounselorProfileController.class)
                .info("Saving profile with displayName: {}, city: {}", p.getDisplayName(), p.getCity());
            
            CounselorProfile saved = repo.save(p);
            
            org.slf4j.LoggerFactory.getLogger(CounselorProfileController.class)
                .info("Profile saved successfully with id: {}", saved.getId());
            
            return ApiResponse.success(saved);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(CounselorProfileController.class)
                .error("Error updating profile for user: " + auth.getName(), e);
            throw e;
        }
    }
    
    @PutMapping
    @PreAuthorize("hasRole('COUNSELOR')")
    public ApiResponse<CounselorProfile> updatePut(Authentication auth, @RequestBody CounselorProfile payload) {
        return update(auth, payload);
    }
    
    /**
     * 验证定价字段的合法性
     * @param profile 咨询师资料
     * @return 错误信息，如果验证通过则返回null
     */
    private String validatePricing(CounselorProfile profile) {
        Double priceText = profile.getPriceText();
        Double priceVoice = profile.getPriceVoice();
        Double priceVideo = profile.getPriceVideo();
        
        // 验证价格必须为非负数
        if (priceText != null && priceText < 0) {
            return "文字咨询价格不能为负数";
        }
        if (priceVoice != null && priceVoice < 0) {
            return "语音咨询价格不能为负数";
        }
        if (priceVideo != null && priceVideo < 0) {
            return "视频咨询价格不能为负数";
        }
        
        // 验证至少有一种咨询形式的价格不为null
        if (priceText == null && priceVoice == null && priceVideo == null) {
            return "至少需要设置一种咨询形式的价格";
        }
        
        return null; // 验证通过
    }
}