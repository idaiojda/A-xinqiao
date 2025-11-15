package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.ArticleAdapter;
import com.example.xinqiao.bean.ArticleBean;
import com.example.xinqiao.dao.ArticleHistoryDao;
import com.example.xinqiao.util.ui.ArticleImageResolver;
import com.example.xinqiao.util.AnalysisUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章阅读历史页面：展示当前登录用户的文章阅读历史，支持清空历史。
 */
public class ArticleHistoryActivity extends AppCompatActivity {
    private TextView tv_back;
    private TextView tv_main_title;
    private EditText et_search_course; // 标题栏课程搜索（隐藏）
    private View ll_play_history_top;  // 标题栏播放历史（隐藏）

    private RecyclerView rvHistory;
    private ArticleAdapter adapter;
    private ArticleHistoryDao historyDao;
    private String userName;

    private View tvClear; // 清空按钮（位于页面内容区）
    private View emptyState; // 空状态容器
    private View btnExplore; // 空状态跳转按钮

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_history);

        tv_back = findViewById(R.id.tv_back);
        tv_main_title = findViewById(R.id.tv_main_title);
        et_search_course = findViewById(R.id.et_search_course);
        ll_play_history_top = findViewById(R.id.ll_play_history_top);
        // 统一历史页面标题栏样式：使用浅蓝 #99B8FF 背景
        View titleBar = findViewById(R.id.title_bar);
        if (titleBar != null) {
            titleBar.setBackgroundResource(R.drawable.topbar_history_bg);
        }

        if (tv_main_title != null) {
            tv_main_title.setText("阅读历史");
            tv_main_title.setVisibility(View.VISIBLE);
        }
        if (tv_back != null) {
            tv_back.setVisibility(View.VISIBLE);
            tv_back.setOnClickListener(v -> finish());
        }
        if (et_search_course != null) et_search_course.setVisibility(View.GONE);
        if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);

        rvHistory = findViewById(R.id.rv_history_list);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        // 阅读历史页展示阅读进度徽标
        adapter = new ArticleAdapter(this, new ArrayList<>(), true);
        rvHistory.setAdapter(adapter);

        tvClear = findViewById(R.id.tv_clear_history);
        historyDao = new ArticleHistoryDao(this);
        userName = AnalysisUtils.readLoginUserName(this);

        if (tvClear != null) {
            tvClear.setOnClickListener(v -> {
                if (userName == null || userName.isEmpty()) {
                    Toast.makeText(this, "未登录，无法清空历史", Toast.LENGTH_SHORT).show();
                    return;
                }
                historyDao.deleteArticleHistoryAsync(userName, success -> {
                    Toast.makeText(this, success ? "已清空历史" : "清空失败", Toast.LENGTH_SHORT).show();
                    loadHistory();
                });
            });
        }

        // 空状态按钮：跳转到文章搜索页
        emptyState = findViewById(R.id.ll_empty_state);
        btnExplore = findViewById(R.id.btn_go_explore);
        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> {
                try {
                    startActivity(new android.content.Intent(this, com.example.xinqiao.activity.ArticleSearchActivity.class));
                } catch (Exception e) {
                    Toast.makeText(this, "跳转失败", Toast.LENGTH_SHORT).show();
                }
            });
        }

        loadHistory();
    }

    

    private void loadHistory() {
        if (userName == null || userName.isEmpty()) {
            adapter.setData(new ArrayList<>());
            return;
        }
        historyDao.getArticleHistoryAsync(userName, history -> {
            List<ArticleBean> list = history != null ? history : new ArrayList<>();
            // 为缺失图片的历史记录补全图片资源ID，确保每条都展示对应图片
            ArticleImageResolver.enrichImages(list);
            adapter.setData(list);
            // 切换空状态与列表显示
            if (emptyState != null) emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            rvHistory.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    /**
     * XML onClick 回调：点击返回图标，关闭当前页面回到上一页。
     */
    public void onBackClicked(View v) {
        finish();
    }
}
