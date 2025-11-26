package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.repository.UserRepository;
import com.example.xinqiaobackend.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username, @RequestParam String password, @RequestParam(required = false) String role) {
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "用户名已存在"));
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        if (role != null && role.trim().length() > 0) {
            u.setRoles(Arrays.asList(role.trim()));
        } else {
            u.setRoles(Arrays.asList("USER"));
        }
        userRepository.save(u);
        return ResponseEntity.ok(Collections.singletonMap("ok", true));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String password) {
        User u = userRepository.findByUsername(username).orElse(null);
        Map<String, Object> res = new HashMap<>();
        if (u == null || !passwordEncoder.matches(password, u.getPassword())) {
            res.put("ok", false);
            res.put("error", "用户名或密码错误");
            return res;
        }
        String token = jwtUtil.generateToken(u.getUsername(), u.getRoles());
        res.put("ok", true);
        res.put("token", token);
        return res;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, Object> res = new HashMap<>();
        User u = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                String username = jwtUtil.parse(token).getSubject();
                u = userRepository.findByUsername(username).orElse(null);
            } catch (Exception ignored) { }
        }
        if (u == null) {
            res.put("ok", false);
            res.put("error", "未登录");
            return res;
        }
        res.put("ok", true);
        res.put("username", u.getUsername());
        res.put("roles", u.getRoles());
        return res;
    }
}

