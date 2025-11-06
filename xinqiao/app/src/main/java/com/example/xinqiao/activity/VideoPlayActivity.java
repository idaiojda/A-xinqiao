package com.example.xinqiao.activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;

public class VideoPlayActivity extends AppCompatActivity {
    private VideoView videoView;
    private MediaController controller;
    private String videoPath;// 视频地址或键值（例如：video11）
    private int position;//传递视频详情界面点击的视频位置
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置界面全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video_play);
        //设置此界面为横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //获取从播放记录界面传递过来的视频地址
        videoPath = getIntent().getStringExtra("videoPath");
        position=getIntent().getIntExtra("position",0);
        init();
    }
    /**
     * 初始化UI控件
     */
    private void init() {
        videoView = (VideoView) findViewById(R.id.videoView);
        controller = new MediaController(this);
        videoView.setMediaController(controller);
        play();
    }
    /**
     * 播放视频
     */
    private void play() {
        // 根据传入的 videoPath（可能是完整 URL 或类似 "video11" 的键）解析为可播放的 URL
        String url = resolveVideoUrl(videoPath);
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "未找到视频链接或链接不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = Uri.parse(url);
            videoView.setVideoURI(uri);
            videoView.start();
        } catch (Exception e) {
            Toast.makeText(this, "视频链接解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将传入的键或 URL 解析为最终播放的 URL。
     * - 如果是 http/https 开头，直接返回
     * - 如果是类似 "video11" 的键值，按映射表转换为示例链接
     */
    private String resolveVideoUrl(String keyOrUrl) {
        if (TextUtils.isEmpty(keyOrUrl)) return null;
        String lower = keyOrUrl.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return keyOrUrl;
        }
        // 示例视频链接（稳定公开样例来源：Google 测试视频桶）
        // https://storage.googleapis.com/gtv-videos-bucket/sample/
        switch (lower) {
            case "video11":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
            case "video12":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4";
            case "video21":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";
            case "video22":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4";
            case "video31":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4";
            case "video32":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4";
            case "video41":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4";
            case "video42":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4";
            case "video51":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4";
            case "video52":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4";
            case "video61":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
            case "video62":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4";
            case "video71":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";
            case "video72":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4";
            case "video81":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4";
            case "video82":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4";
            case "video91":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4";
            case "video92":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4";
            case "video101":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4";
            case "video102":
                return "https://storage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4";
            default:
                return null;
        }
    }
    /**
     * 点击后退键
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //把视频详情界面传递过来的被点击视频的位置传递回去
        Intent data=new Intent();
        data.putExtra("position", position);
        setResult(RESULT_OK, data);
        return super.onKeyDown(keyCode, event);
    }
}
