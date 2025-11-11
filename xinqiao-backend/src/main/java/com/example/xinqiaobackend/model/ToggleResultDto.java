package com.example.xinqiaobackend.model;

public class ToggleResultDto {
    private boolean liked;
    private boolean collected;

    public ToggleResultDto() {}
    public ToggleResultDto(boolean liked, boolean collected) {
        this.liked = liked; this.collected = collected;
    }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public boolean isCollected() { return collected; }
    public void setCollected(boolean collected) { this.collected = collected; }
}

