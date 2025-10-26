package com.example.xinqiao.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.PlayHistoryAdapter;
import com.example.xinqiao.bean.VideoBean;
import com.example.xinqiao.mysql.DBUtils;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.mysql.MySQLHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlayHistoryActivity extends AppCompatActivity {
    private ListView lvHistory;
    private LinearLayout llEmpty;
    private PlayHistoryAdapter adapter;
    private List<VideoBean> historyList;
    private DBUtils db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_history);
        
        initViews();
        initDatabase();
    }

    private void initViews() {
        // 返回按钮
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        
        // 清空按钮
        findViewById(R.id.tv_clear).setOnClickListener(v -> showClearHistoryDialog());
        
        lvHistory = findViewById(R.id.lv_history);
        llEmpty = findViewById(R.id.ll_empty);
        
        historyList = new ArrayList<>();
        adapter = new PlayHistoryAdapter(this, historyList);
        adapter.setOnItemClickListener(new PlayHistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(VideoBean videoBean, int position) {
                // 跳转到视频播放界面
                Intent intent = new Intent(PlayHistoryActivity.this, VideoPlayActivity.class);
                intent.putExtra("videoPath", videoBean.videoPath);
                intent.putExtra("position", position);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(VideoBean videoBean, int position) {
                showDeleteHistoryDialog(videoBean, position);
            }
        });
        
        lvHistory.setAdapter(adapter);
    }

    private void initDatabase() {
        // 使用新的DBUtils初始化方式
        DBUtils.init(this, new DBUtils.InitCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    try {
                        db = DBUtils.getInstance(PlayHistoryActivity.this);
                        // 数据库初始化成功后加载播放历史
                        loadPlayHistory();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Toast.makeText(PlayHistoryActivity.this, "数据库连接失败", Toast.LENGTH_SHORT).show();
                        showEmptyView();
                    }
                });
            }
            
            @Override
            public void onError(SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(PlayHistoryActivity.this, "数据库连接失败", Toast.LENGTH_SHORT).show();
                    showEmptyView();
                });
            }
        });
    }

    private void loadPlayHistory() {
        if (db == null) {
            runOnUiThread(() -> showEmptyView());
            return;
        }

        String userName = AnalysisUtils.readLoginUserName(this);
        if (userName == null) {
            runOnUiThread(() -> showEmptyView());
            return;
        }

        // 从数据库加载播放历史（使用异步方法）
        db.getVideoPlayHistory(userName, new DBUtils.VideoHistoryCallback() {
            @Override
            public void onResult(List<VideoBean> history) {
                if (history != null && !history.isEmpty()) {
                    historyList.clear();
                    historyList.addAll(history);
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        showHistoryView();
                    });
                } else {
                    runOnUiThread(() -> showEmptyView());
                }
            }
        });
    }

    private void showHistoryView() {
        lvHistory.setVisibility(View.VISIBLE);
        llEmpty.setVisibility(View.GONE);
    }

    private void showEmptyView() {
        lvHistory.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
    }

    private void showDeleteHistoryDialog(VideoBean videoBean, int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除播放记录")
                .setMessage("确定要删除这条播放记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    deleteHistoryItem(videoBean, position);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清空播放历史")
                .setMessage("确定要清空所有播放历史吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    clearAllHistory();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteHistoryItem(VideoBean videoBean, int position) {
        if (db != null) {
            String userName = AnalysisUtils.readLoginUserName(this);
            if (userName != null) {
                // 使用异步方法删除播放记录
                db.deleteVideoPlayList(userName, videoBean.chapterId, videoBean.videoId, new DBUtils.DeleteHistoryCallback() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            historyList.remove(position);
                            runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                if (historyList.isEmpty()) {
                                    showEmptyView();
                                }
                                Toast.makeText(PlayHistoryActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(PlayHistoryActivity.this, "删除失败", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            }
        }
    }

    private void clearAllHistory() {
        if (db != null) {
            String userName = AnalysisUtils.readLoginUserName(this);
            if (userName != null) {
                // 使用异步方法清空播放历史
                db.clearVideoPlayHistory(userName, new DBUtils.ClearHistoryCallback() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            historyList.clear();
                            runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                showEmptyView();
                                Toast.makeText(PlayHistoryActivity.this, "清空成功", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(PlayHistoryActivity.this, "清空失败", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            }
        }
    }
}