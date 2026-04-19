package com.example.xinqiaobackend.security;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        logger.debug("Processing request: " + request.getRequestURI());
        logger.debug("Authorization header: " + (header != null ? "EXISTS" : "NONE"));
        
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                String username = claims.getSubject();
                List<String> roles = claims.get("roles", List.class);
                
                logger.debug("Token parsed successfully. Username: " + username + ", Roles: " + roles);
                
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (roles != null) {
                    for (String r : roles) authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                }
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                logger.debug("Authentication set successfully");
            } catch (Exception e) {
                logger.error("JWT parsing failed", e);
            }
        }
        chain.doFilter(request, response);
    }
}

