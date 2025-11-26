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
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=none"
})
public class CommunityPostTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void createPostThenComment() throws Exception {
        String body = "{\"title\":\"t\",\"content\":\"c\",\"tags\":[\"tag\"],\"images\":[],\"anonymous\":false,\"authorName\":\"u\"}";
        MvcResult res = mockMvc.perform(post("/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Basic dXNlcjpwYXNzd29yZA==")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("t"))
                .andReturn();
        String json = res.getResponse().getContentAsString();
        String id = json.replaceAll(".*\"id\":\"(.*?)\".*", "$1");
        String commentBody = "{\"author\":\"u\",\"text\":\"hi\"}";
        mockMvc.perform(post("/community/posts/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Basic dXNlcjpwYXNzd29yZA==")
                        .content(commentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("hi"));
        mockMvc.perform(get("/community/posts/" + id + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("hi"));
    }
}
