package com.example.xinqiaobackend.config;

import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@Configuration
public class StartupDataLoader {
    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            try {
                if (!userRepository.existsByUsername("admin")) {
                    User u = new User();
                    u.setUsername("admin");
                    u.setPassword(encoder.encode("admin123"));
                    u.setRoles(Arrays.asList("ADMIN"));
                    u.setReviewStatus("APPROVED");
                    userRepository.save(u);
                }
            } catch (Exception ignored) {}
        };
    }
}