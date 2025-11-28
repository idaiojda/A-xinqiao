package com.example.xinqiaobackend;

import com.example.xinqiaobackend.modules.application.entity.CounselorApplication;
import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.modules.application.repository.CounselorApplicationRepository;
import com.example.xinqiaobackend.repository.UserRepository;
import com.example.xinqiaobackend.security.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.jwt.secret=this_is_a_very_long_test_secret_key_for_jwt_256_bits",
        "app.jwt.expiration-seconds=3600"
})
public class UserToCounselorFlowTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private CounselorApplicationRepository appRepo;

    @Test
    void user_submit_then_admin_approve_becomes_counselor() throws Exception {
        // prepare user
        User u = new User();
        u.setUsername("u_apply");
        u.setPassword("p");
        u.setRoles(new java.util.ArrayList<>(java.util.Arrays.asList("USER")));
        u.setReviewStatus("NONE");
        u = userRepository.save(u);

        String userToken = jwtUtil.generateToken(u.getUsername(), Collections.singletonList("USER"));
        String adminToken = jwtUtil.generateToken("admin", Collections.singletonList("ADMIN"));

        // before approval, counselor endpoint should be forbidden even if role missing
        mockMvc.perform(get("/api/counselor/ping").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // submit application
        String payload = "{" +
                "\"realName\":\"张三\"," +
                "\"qualificationType\":\"国家三级\"," +
                "\"years\":3," +
                "\"expertise\":[\"焦虑\",\"抑郁\"]," +
                "\"materials\":[\"http://example.com/cert.jpg\"]," +
                "\"intro\":\"擅长认知行为疗法\"" +
                "}";
        mockMvc.perform(post("/api/applications").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // verify user status PENDING and no counselor role
        User reloaded = userRepository.findById(u.getId()).orElseThrow();
        Assertions.assertEquals("PENDING", reloaded.getReviewStatus());
        Assertions.assertFalse(reloaded.getRoles().contains("COUNSELOR"));
        List<CounselorApplication> apps = appRepo.findByUserId(u.getId());
        Assertions.assertFalse(apps.isEmpty());
        Long appId = apps.get(0).getId();

        // approve
        mockMvc.perform(post("/api/admin/applications/" + appId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // after approval, user should have counselor role and APPROVED status
        reloaded = userRepository.findById(u.getId()).orElseThrow();
        Assertions.assertEquals("APPROVED", reloaded.getReviewStatus());
        Assertions.assertTrue(reloaded.getRoles().contains("COUNSELOR"));

        // counselor endpoint still requires role, but our token has USER only; generate counselor token
        String counselorToken = jwtUtil.generateToken(u.getUsername(), Collections.singletonList("COUNSELOR"));
        mockMvc.perform(get("/api/counselor/ping").header("Authorization", "Bearer " + counselorToken))
                .andExpect(status().isOk());
    }
}
