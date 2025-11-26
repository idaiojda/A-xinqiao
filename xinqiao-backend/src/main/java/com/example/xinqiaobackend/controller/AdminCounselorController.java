package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/counselors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCounselorController {
    private final UserRepository userRepository;

    public AdminCounselorController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<String> search(@RequestParam(required = false, name = "query") String q) {
        List<User> list = userRepository.findByRoleAndStatus("COUNSELOR", null, q == null ? null : q.trim());
        return list.stream().map(User::getUsername).collect(Collectors.toList());
    }
}
