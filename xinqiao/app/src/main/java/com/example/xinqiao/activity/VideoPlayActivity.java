package com.example.xinqiao.activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.media3.datasource.okhttp.OkHttpDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import okhttp3.OkHttpClient;

public class VideoPlayActivity extends AppCompatActivity {
    private PlayerView playerView;
    private ExoPlayer player;
    private String videoPath;// 视频地址或键值（例如：video11）
    private int position;//传递视频详情界面点击的视频位置
    private List<String> candidateUrls;
    private int currentCandidateIndex = 0;

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
        position = getIntent().getIntExtra("position", 0);
        init();
    }

    /**
     * 初始化UI控件与播放器
     */
    private void init() {
        playerView = findViewById(R.id.player_view);
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(new OkHttpDataSource.Factory(okHttpClient)))
                .build();
        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                tryNextCandidate(error);
            }
        });
        play();
    }

    /**
     * 播放视频
     */
    private void play() {
        candidateUrls = resolveVideoCandidates(videoPath);
        currentCandidateIndex = 0;
        playCurrentCandidate();
    }

    private void playCurrentCandidate() {
        if (candidateUrls == null || candidateUrls.isEmpty() || currentCandidateIndex >= candidateUrls.size()) {
            Toast.makeText(this, "未找到可用视频链接", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = candidateUrls.get(currentCandidateIndex);
        try {
            MediaItem mediaItem = MediaItem.fromUri(url);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        } catch (Exception e) {
            Toast.makeText(this, "视频播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void tryNextCandidate(PlaybackException error) {
        currentCandidateIndex++;
        if (currentCandidateIndex < candidateUrls.size()) {
            Toast.makeText(this, "网络错误，切换备用源…", Toast.LENGTH_SHORT).show();
            playCurrentCandidate();
        } else {
            Toast.makeText(this, "播放失败: " + (error != null ? error.getMessage() : "未知错误"), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 将传入的键或 URL 解析为最终播放的 URL。
     * - 如果是 http/https 开头，直接返回
     * - 如果是类似 "video11" 的键值，按映射表转换为示例链接
     */
    private List<String> resolveVideoCandidates(String keyOrUrl) {
        Set<String> ordered = new LinkedHashSet<>();
        if (!TextUtils.isEmpty(keyOrUrl)) {
            String lower = keyOrUrl.toLowerCase();
            if (lower.startsWith("http://") || lower.startsWith("https://")) {
                ordered.add(keyOrUrl);
            } else {
                switch (lower) {
                    case "video11":
                    case "video61":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
                        break;
                    case "video12":
                    case "video62":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4");
                        break;
                    case "video21":
                    case "video71":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4");
                        break;
                    case "video22":
                    case "video72":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4");
                        break;
                    case "video31":
                    case "video81":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4");
                        break;
                    case "video32":
                    case "video82":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4");
                        break;
                    case "video41":
                    case "video91":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4");
                        break;
                    case "video42":
                    case "video92":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4");
                        break;
                    case "video51":
                    case "video101":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4");
                        break;
                    case "video52":
                    case "video102":
                        ordered.add("https://storage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4");
                        break;
                    default:
                        break;
                }
            }
        }
        // 备用源（更易在国内网络下访问）
        ordered.add("https://www.w3schools.com/html/mov_bbb.mp4");
        ordered.add("https://media.w3.org/2010/05/sintel/trailer.mp4");
        return new ArrayList<>(ordered);
    }
    /**
     * 点击后退键
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //把视频详情界面传递过来的被点击视频的位置传递回去
        Intent data = new Intent();
        data.putExtra("position", position);
        setResult(RESULT_OK, data);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
    }
}
