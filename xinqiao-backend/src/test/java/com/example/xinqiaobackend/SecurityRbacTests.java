package com.example.xinqiaobackend;

import com.example.xinqiaobackend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.jwt.secret=this_is_a_very_long_test_secret_key_for_jwt_256_bits",
        "app.jwt.expiration-seconds=3600"
})
public class SecurityRbacTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void adminPing_requiresAdminRole() throws Exception {
        String adminToken = jwtUtil.generateToken("admin", Collections.singletonList("ADMIN"));
        String userToken = jwtUtil.generateToken("user", Collections.singletonList("USER"));

        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void counselorPing_requiresCounselorRole() throws Exception {
        String counselorToken = jwtUtil.generateToken("counselor", Collections.singletonList("COUNSELOR"));
        String adminToken = jwtUtil.generateToken("admin", Collections.singletonList("ADMIN"));

        mockMvc.perform(get("/api/counselor/ping").header("Authorization", "Bearer " + counselorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/counselor/ping").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/counselor/ping"))
                .andExpect(status().isUnauthorized());
    }
}