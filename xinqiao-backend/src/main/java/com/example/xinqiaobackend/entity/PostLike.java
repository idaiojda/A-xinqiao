package com.example.xinqiaobackend.entity;

import javax.persistence.*;

@Entity
@Table(name = "post_likes")
public class PostLike {

    @EmbeddedId
    private PostLikeId id = new PostLikeId();

    @ManyToOne(optional = false)
    @MapsId("postId")
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    public PostLikeId getId() { return id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; if (post != null) id.setPostId(post.getId()); }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; if (user != null) id.setUserId(user.getId()); }
}
