package com.example.xinqiao.bean;
public class VideoBean {
    public int chapterId;// 章节Id
    public int videoId;// 视频Id
    public String title;// 章节标题
    public String secondTitle;// 视频标题
    public String videoPath;// 视频播放地址
    public long playTime;// 播放时间戳
    public double price;// 视频价格
    public boolean isPurchased;// 是否已购买
    
    public VideoBean() {
        this.price = 0.0;
        this.isPurchased = false;
    }
}