package com.example.xinqiaobackend.config;

import com.example.xinqiaobackend.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class CounselorAccessGuard implements HandlerInterceptor {
    private final UserRepository userRepository;

    public CounselorAccessGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/counselor/")) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return true;
            String username = auth.getName();
            com.example.xinqiaobackend.entity.User u = userRepository.findByUsername(username).orElse(null);
            if (u == null) return true;
            if (!"APPROVED".equalsIgnoreCase(u.getReviewStatus())) {
                response.setStatus(403);
                return false;
            }
        }
        return true;
    }
}