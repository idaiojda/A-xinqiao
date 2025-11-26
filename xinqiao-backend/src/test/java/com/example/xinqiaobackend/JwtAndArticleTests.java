package com.example.xinqiaobackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb2;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=none",
        "app.jwt.secret=test-secret-123456789012345678901234567890",
        "app.jwt.expiration-seconds=3600"
})
public class JwtAndArticleTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void registerLoginThenUseToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .param("username", "alice")
                        .param("password", "pwd123")
                        .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .param("username", "alice")
                        .param("password", "pwd123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andReturn();
        String token = login.getResponse().getContentAsString().replaceAll(".*\"token\":\"(.*?)\".*", "$1");

        String body = "{\"title\":\"T\",\"content\":\"C\",\"tags\":[\"成长\"],\"images\":[],\"anonymous\":false,\"authorName\":\"alice\"}";
        mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("T"));
    }

    @Test
    public void articleCrudWithAdminRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .param("username", "admin")
                        .param("password", "pwd123")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk());
        String token = mockMvc.perform(post("/api/auth/login")
                        .param("username", "admin")
                        .param("password", "pwd123"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"token\":\"(.*?)\".*", "$1");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .param("title", "科普一")
                        .param("content", "内容")
                        .param("category", "科普"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("科普一"));

        mockMvc.perform(get("/api/articles/list"))
                .andExpect(status().isOk());
    }
}

