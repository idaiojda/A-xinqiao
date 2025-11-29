package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.repository.UserRepository;
import com.example.xinqiaobackend.modules.application.repository.CounselorApplicationRepository;
import com.example.xinqiaobackend.security.JwtUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final UserRepository userRepository;
    private final CounselorApplicationRepository applicationRepository;
    private final JwtUtil jwtUtil;

    public HealthController(UserRepository userRepository, CounselorApplicationRepository applicationRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> res = new HashMap<>();
        Map<String, Object> db = new HashMap<>();
        try {
            long u = userRepository.count();
            long a = applicationRepository.count();
            db.put("ok", true);
            db.put("users", u);
            db.put("applications", a);
        } catch (Exception e) {
            db.put("ok", false);
            db.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        Map<String, Object> jwt = new HashMap<>();
        try {
            String t = jwtUtil.generateToken("health", Arrays.asList("USER"));
            jwt.put("ok", true);
            jwt.put("sample", t.substring(0, Math.min(24, t.length())) + "...");
        } catch (Exception e) {
            jwt.put("ok", false);
            jwt.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        res.put("ok", (db.get("ok") == Boolean.TRUE) && (jwt.get("ok") == Boolean.TRUE));
        res.put("db", db);
        res.put("jwt", jwt);
        return res;
    }
}
