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
import com.example.xinqiao.utils.AnalysisUtils;

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

                // 检查是否购买了课程
                new com.example.xinqiao.utils.PaymentUtils(VideoListActivity.this)
                    .checkCoursePurchased(AnalysisUtils.readLoginUserName(VideoListActivity.this),
                        chapterId,
                        new com.example.xinqiao.utils.PaymentUtils.PaymentCallback() {
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
                                builder.setTitle("购买课程")
                                    .setMessage("您还未购买该课程，是否立即购买？\n课程价格：￥99.00")
                                    .setPositiveButton("购买", (dialog, which) -> {
                                        // 执行购买操作
                                        new com.example.xinqiao.utils.PaymentUtils(VideoListActivity.this)
                                            .purchaseCourse(AnalysisUtils.readLoginUserName(VideoListActivity.this),
                                                chapterId,
                                                new com.example.xinqiao.utils.PaymentUtils.PaymentCallback() {
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
     * 设置视频列表本地数据
     */
    private void initData() {
        JSONArray jsonArray;
        InputStream is = null;
        try {
            is = getResources().getAssets().open("data.json");
            jsonArray = new JSONArray(read(is));
            videoList = new ArrayList<VideoBean>();
            for (int i = 0; i < jsonArray.length(); i++) {
                VideoBean bean = new VideoBean();
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                if (jsonObj.getInt("chapterId") == chapterId) {
                    bean.chapterId=jsonObj.getInt("chapterId");
                    bean.videoId=Integer.parseInt(jsonObj
                            .getString("videoId"));
                    bean.title=jsonObj.getString("title");
                    bean.secondTitle=jsonObj.getString("secondTitle");
                    bean.videoPath=jsonObj.getString("videoPath");
                    videoList.add(bean);
                }
                bean = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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