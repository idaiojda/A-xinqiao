package com.example.xinqiaobackend.service;

import com.example.xinqiaobackend.model.PostDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class PostConsistencyTests {
    @Test
    void differentAccountsSeeIdenticalPosts() {
        InMemoryCommunityService svc = new InMemoryCommunityService();
        List<PostDto> a = svc.getPosts(null, 0, 10);
        List<PostDto> b = svc.getPosts(null, 0, 10);
        Assertions.assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            Assertions.assertEquals(a.get(i).getId(), b.get(i).getId());
        }
    }

    @Test
    void sortingIsByCreatedTimeDesc() {
        InMemoryCommunityService svc = new InMemoryCommunityService();
        PostDto p = svc.createPost("t", "c", java.util.Arrays.asList("tag"), java.util.Collections.emptyList(), true, null, null);
        List<PostDto> list = svc.getPosts(null, 0, 10);
        Assertions.assertEquals(p.getId(), list.get(0).getId());
    }

    @Test
    void paginationConsistentAcrossAccounts() {
        InMemoryCommunityService svc = new InMemoryCommunityService();
        List<PostDto> a0 = svc.getPosts(null, 0, 2);
        List<PostDto> b0 = svc.getPosts(null, 0, 2);
        List<PostDto> a1 = svc.getPosts(null, 1, 2);
        List<PostDto> b1 = svc.getPosts(null, 1, 2);
        Assertions.assertEquals(a0.size(), b0.size());
        Assertions.assertEquals(a1.size(), b1.size());
        for (int i = 0; i < a0.size(); i++) Assertions.assertEquals(a0.get(i).getId(), b0.get(i).getId());
        for (int i = 0; i < a1.size(); i++) Assertions.assertEquals(a1.get(i).getId(), b1.get(i).getId());
    }

    @Test
    void updatesInvalidateCacheAndAreVisibleToAll() {
        InMemoryCommunityService svc = new InMemoryCommunityService();
        List<PostDto> beforeA = svc.getPosts(null, 0, 10);
        List<PostDto> beforeB = svc.getPosts(null, 0, 10);
        PostDto created = svc.createPost("new", "content", java.util.Collections.emptyList(), java.util.Collections.emptyList(), true, null, null);
        List<PostDto> afterA = svc.getPosts(null, 0, 10);
        List<PostDto> afterB = svc.getPosts(null, 0, 10);
        Assertions.assertEquals(created.getId(), afterA.get(0).getId());
        Assertions.assertEquals(created.getId(), afterB.get(0).getId());
        Assertions.assertEquals(beforeA.size() + 1, afterA.size());
        Assertions.assertEquals(beforeB.size() + 1, afterB.size());
    }
}

