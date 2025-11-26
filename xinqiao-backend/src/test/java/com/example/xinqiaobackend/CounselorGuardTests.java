package com.example.xinqiaobackend;

import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.repository.UserRepository;
import com.example.xinqiaobackend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.jwt.secret=this_is_a_very_long_test_secret_key_for_jwt_256_bits",
        "app.jwt.expiration-seconds=3600"
})
public class CounselorGuardTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;

    @Test
    void unapprovedCounselor_blocked() throws Exception {
        User u = new User();
        u.setUsername("c_guard");
        u.setPassword("p");
        u.setRoles(java.util.Arrays.asList("COUNSELOR"));
        u.setReviewStatus("PENDING");
        userRepository.save(u);

        String token = jwtUtil.generateToken(u.getUsername(), Collections.singletonList("COUNSELOR"));
        mockMvc.perform(get("/api/counselor/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}