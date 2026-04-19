package com.example.xinqiao.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.VideoListAdapter;
import com.example.xinqiao.bean.VideoBean;
import com.example.xinqiao.mysql.DBUtils;
import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.util.AnalysisUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VideoListActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_intro, tv_video, tv_chapter_intro;
    private ListView lv_video_list;
    private ScrollView sv_chapter_intro;
    private VideoListAdapter adapter;
    private List<VideoBean> videoList;
    private int chapterId;
    private String intro;
    private double coursePrice = 0.0; // 课程价格
    private boolean isPremium = false; // 是否付费课程
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        // 设置此界面为竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // 从课程界面传递过来的章节id
        chapterId = getIntent().getIntExtra("id", 0);
        // 从课程界面传递过来的章节简介
        intro = getIntent().getStringExtra("intro");
        // 从课程界面传递过来的价格信息
        isPremium = getIntent().getBooleanExtra("premium", false);
        coursePrice = getIntent().getDoubleExtra("price", 0.0);
        
        android.util.Log.d("VideoListActivity", "课程ID: " + chapterId + ", 是否付费: " + isPremium + ", 价格: " + coursePrice);
        
        // 初始化数据
        initData();
        init();
    }
    
    /**
     * 初始化界面UI控件
     */
    private void init() {
        tv_intro = (TextView) findViewById(R.id.tv_intro);
        tv_video = (TextView) findViewById(R.id.tv_video);
        lv_video_list = (ListView) findViewById(R.id.lv_video_list);
        tv_chapter_intro = (TextView) findViewById(R.id.tv_chapter_intro);
        sv_chapter_intro= (ScrollView) findViewById(R.id.sv_chapter_intro);
        adapter = new VideoListAdapter(this, new VideoListAdapter.OnSelectListener() {
            @Override
            public void onSelect(int position, ImageView iv) {
                if (!readLoginStatus()) {
                    Toast.makeText(VideoListActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }

                adapter.setSelectedPosition(position); // 设置适配器的选中项
                VideoBean bean = videoList.get(position);
                String videoPath = bean.videoPath;
                adapter.notifyDataSetChanged();// 更新列表框

                if (TextUtils.isEmpty(videoPath)) {
                    Toast.makeText(VideoListActivity.this, "本地没有此视频，暂无法播放", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 免费课程直接播放，付费课程需要检查购买状态
                if (!isPremium || coursePrice <= 0) {
                    // 免费课程，直接播放
                    savePlayHistory(videoList.get(position));
                    Intent intent = new Intent(VideoListActivity.this, VideoPlayActivity.class);
                    intent.putExtra("videoPath", videoPath);
                    intent.putExtra("position", position);
                    startActivityForResult(intent, 1);
                    return;
                }

                // 付费课程，检查是否购买
                new com.example.xinqiao.util.payment.PaymentUtils(VideoListActivity.this)
                    .checkCoursePurchased(AnalysisUtils.readLoginUserName(VideoListActivity.this),
                        chapterId,
                        new com.example.xinqiao.util.payment.PaymentUtils.PaymentCallback() {
                            @Override
                            public void onSuccess() {
                                // 已购买，记录播放历史并播放视频
                                savePlayHistory(videoList.get(position));
                                Intent intent = new Intent(VideoListActivity.this, VideoPlayActivity.class);
                                intent.putExtra("videoPath", videoPath);
                                intent.putExtra("position", position);
                                startActivityForResult(intent, 1);
                            }

                            @Override
                            public void onError(String message) {
                                // 未购买，显示购买对话框
                                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(VideoListActivity.this);
                                String priceText = String.format("￥%.2f", coursePrice);
                                builder.setTitle("购买课程")
                                    .setMessage("您还未购买该课程，是否立即购买？\n课程价格：" + priceText)
                                    .setPositiveButton("购买", (dialog, which) -> {
                                        // 执行购买操作
                                        new com.example.xinqiao.util.payment.PaymentUtils(VideoListActivity.this)
                                            .purchaseCourse(AnalysisUtils.readLoginUserName(VideoListActivity.this),
                                                chapterId,
                                                new com.example.xinqiao.util.payment.PaymentUtils.PaymentCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        Toast.makeText(VideoListActivity.this, "购买成功", Toast.LENGTH_SHORT).show();
                                                        // 购买成功后自动播放视频
                                                        savePlayHistory(videoList.get(position));
                                                        Intent intent = new Intent(VideoListActivity.this, VideoPlayActivity.class);
                                                        intent.putExtra("videoPath", videoPath);
                                                        intent.putExtra("position", position);
                                                        startActivityForResult(intent, 1);
                                                    }

                                                    @Override
                                                    public void onError(String message) {
                                                        if (message.contains("余额不足")) {
                                                            new android.app.AlertDialog.Builder(VideoListActivity.this)
                                                                .setTitle("余额不足")
                                                                .setMessage("是否前往充值？")
                                                                .setPositiveButton("去充值", (d, w) -> {
                                                                    Intent rechargeIntent = new Intent(VideoListActivity.this, RechargeActivity.class);
                                                                    startActivity(rechargeIntent);
                                                                })
                                                                .setNegativeButton("取消", null)
                                                                .show();
                                                        } else {
                                                            Toast.makeText(VideoListActivity.this, message, Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                            }
                        });
                }

        });
        lv_video_list.setAdapter(adapter);
        tv_intro.setOnClickListener(this);
        tv_video.setOnClickListener(this);
        adapter.setData(videoList);
        tv_chapter_intro.setText(intro);
        // 默认选中简介tab
        setTabSelected(true);
    }

    private void setTabSelected(boolean introSelected) {
        tv_intro.setSelected(introSelected);
        tv_video.setSelected(!introSelected);
        tv_intro.setTextColor(introSelected ? 0xFFFFFFFF : 0xFF30B4FF);
        tv_video.setTextColor(introSelected ? 0xFF30B4FF : 0xFFFFFFFF);
    }

    /**
     * 控件的点击事件
     */
    @Override
    public void onClick(View v) {
    if (v.getId() == R.id.tv_intro) { // 简介
        lv_video_list.setVisibility(View.GONE);
        sv_chapter_intro.setVisibility(View.VISIBLE);
        setTabSelected(true);
    } else if (v.getId() == R.id.tv_video) { // 视频
        lv_video_list.setVisibility(View.VISIBLE);
        sv_chapter_intro.setVisibility(View.GONE);
        setTabSelected(false);
    }
}

    /**
     * 从后端API加载视频列表数据
     */
    private void initData() {
        videoList = new ArrayList<>();
        
        // 在后台线程加载数据
        new Thread(() -> {
            try {
                android.util.Log.d("VideoListActivity", "开始从后端加载课时列表，课程ID: " + chapterId);
                
                retrofit2.Response<okhttp3.ResponseBody> response = 
                    kotlinx.coroutines.BuildersKt.runBlocking(
                        kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
                        (coroutineScope, continuation) -> 
                            com.example.xinqiao.network.Http.api().courseLessons((long)chapterId, continuation)
                    );
                
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    android.util.Log.d("VideoListActivity", "API响应: " + json);
                    
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<
                        com.example.xinqiao.network.ApiResp<java.util.List<com.example.xinqiao.counselor.CourseLessonDto>>
                    >(){}.getType();
                    
                    com.example.xinqiao.network.ApiResp<java.util.List<com.example.xinqiao.counselor.CourseLessonDto>> apiResp = 
                        gson.fromJson(json, type);
                    
                    if (apiResp != null && apiResp.getData() != null) {
                        android.util.Log.d("VideoListActivity", "成功加载 " + apiResp.getData().size() + " 个课时");
                        
                        // 转换为VideoBean
                        for (com.example.xinqiao.counselor.CourseLessonDto dto : apiResp.getData()) {
                            VideoBean bean = new VideoBean();
                            bean.chapterId = chapterId;
                            bean.videoId = (int) dto.getId();
                            bean.title = dto.getTitle();
                            bean.secondTitle = dto.getSecondTitle();
                            bean.videoPath = dto.getVideoUrl();
                            videoList.add(bean);
                        }
                    } else {
                        android.util.Log.w("VideoListActivity", "API返回数据为空");
                    }
                } else {
                    android.util.Log.e("VideoListActivity", "API请求失败: " + response.code());
                }
            } catch (Exception e) {
                android.util.Log.e("VideoListActivity", "加载课时列表失败: " + e.getMessage(), e);
            }
            
            // 在主线程更新UI
            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.setData(videoList);
                }
                
                // 如果加载失败，显示错误提示
                if (videoList.isEmpty()) {
                    Toast.makeText(VideoListActivity.this, "无法加载课时列表，请检查网络连接", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
    /**
     * 读取数据流,参数in是数据流
     */
    private String read(InputStream in) {
        BufferedReader reader = null;
        StringBuilder sb = null;
        String line=null;
        try {
            sb = new StringBuilder();//实例化一个StringBuilder对象
            //用InputStreamReader把in这个字节流转换成字符流BufferedReader
            reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine())!=null){//从reader中读取一行的内容判断是否为空
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } finally {
            try {
                if (in != null)
                    in.close();
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
    /**
     * 从SharedPreferences中读取登录状态
     */
    private boolean readLoginStatus() {
        SharedPreferences sp = getSharedPreferences("loginInfo",
                Context.MODE_PRIVATE);
        boolean isLogin = sp.getBoolean("isLogin", false);
        return isLogin;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!=null){
            // 接收播放界面回传过来的被选中的视频的位置
            int position=data.getIntExtra("position", 0);
            adapter.setSelectedPosition(position);// 设置被选中的位置
            // 目录选项卡被选中时所有图标的颜色值
            lv_video_list.setVisibility(View.VISIBLE);
            sv_chapter_intro.setVisibility(View.GONE);
            tv_intro.setBackgroundColor(Color.parseColor("#FFFFFF"));
            tv_video.setBackgroundColor(Color.parseColor("#30B4FF"));
            tv_intro.setTextColor(Color.parseColor("#000000"));
            tv_video.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }
    /**
     * 保存播放历史
     */
    private void savePlayHistory(VideoBean videoBean) {
        try {
            // 检查用户是否登录
            if (!readLoginStatus()) {
                return;
            }
            
            // 获取用户名
            String userName = AnalysisUtils.readLoginUserName(this);
            if (userName == null) {
                return;
            }
            
            // 使用新的DBUtils初始化方式
            DBUtils.init(this, new DBUtils.InitCallback() {
                @Override
                public void onSuccess() {
                    try {
                        DBUtils db = DBUtils.getInstance(VideoListActivity.this);
                        if (db != null && db.isDatabaseAvailable()) {
                            // 使用异步方法保存播放记录
                            db.saveVideoPlayList(videoBean, userName, new DBUtils.SavePlayListCallback() {
                                @Override
                                public void onResult(boolean success) {
                                    runOnUiThread(() -> {
                                        if (!success) {
                                            android.util.Log.w("VideoListActivity", "保存播放记录失败");
                                        } else {
                                            android.util.Log.d("VideoListActivity", "播放记录保存成功");
                                        }
                                    });
                                }
                            });
                        } else {
                            android.util.Log.w("VideoListActivity", "数据库连接失败，无法保存播放记录");
                        }
                    } catch (SQLException e) {
                        android.util.Log.e("VideoListActivity", "数据库连接异常: " + e.getMessage());
                    }
                }
                
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("VideoListActivity", "数据库初始化失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            android.util.Log.e("VideoListActivity", "保存播放历史时发生异常: " + e.getMessage());
            // 不阻止视频播放，只记录错误
        }
    }
}
