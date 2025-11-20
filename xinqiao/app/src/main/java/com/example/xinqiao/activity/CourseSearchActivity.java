package com.example.xinqiao.activity;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.CourseListAdapter;
import com.example.xinqiao.bean.CourseBean;
import com.example.xinqiao.util.AnalysisUtils;
import com.example.xinqiao.util.ui.RecyclerViewOptimizer;
import com.example.xinqiao.util.ui.SpacesItemDecoration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程搜索结果页：展示与关键词匹配的课程卡片列表
 */
public class CourseSearchActivity extends AppCompatActivity {
    private TextView tv_back;
    private TextView tv_main_title;
    private EditText et_search_course; // 来自全局标题栏（此页隐藏）
    private View ll_play_history_top;  // 来自全局标题栏（此页隐藏）

    private RecyclerView rvResults;
    private CourseListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_search);

        // 标题栏控件
        tv_back = findViewById(R.id.tv_back);
        tv_main_title = findViewById(R.id.tv_main_title);
        et_search_course = findViewById(R.id.et_search_course);
        ll_play_history_top = findViewById(R.id.ll_play_history_top);

        if (tv_main_title != null) {
            tv_main_title.setText(getString(R.string.course_search_title));
            tv_main_title.setVisibility(View.VISIBLE);
        }
        if (tv_back != null) {
            tv_back.setVisibility(View.VISIBLE);
            tv_back.setOnClickListener(v -> finish());
        }
        if (et_search_course != null) et_search_course.setVisibility(View.GONE);
        if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);

        // 结果列表
        rvResults = findViewById(R.id.rv_search_results);
        adapter = new CourseListAdapter(this);
        if (rvResults != null) {
            StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
            sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
            rvResults.setLayoutManager(sglm);
            int spacingPx = dpToPx(8);
            rvResults.addItemDecoration(new SpacesItemDecoration(spacingPx, true));
            rvResults.setClipToPadding(false);
            rvResults.setPadding(spacingPx, spacingPx, spacingPx, spacingPx);
            RecyclerViewOptimizer.optimizeDefault(rvResults);
            rvResults.setAdapter(adapter);
        }

        // 获取查询词并加载结果
        String query = getIntent() != null ? getIntent().getStringExtra("query") : null;
        if (query == null) query = "";
        loadAndFilter(query.trim());
    }

    private void loadAndFilter(String query) {
        final String q = query == null ? "" : query;
        new Thread(() -> {
            List<List<CourseBean>> data = new ArrayList<>();
            try {
                InputStream is = getResources().getAssets().open("chaptertitle.xml");
                data = AnalysisUtils.getCourseInfos(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<CourseBean> flat = flatten(data);
            List<CourseBean> filtered = new ArrayList<>();
            if (!q.isEmpty()) {
                for (CourseBean c : flat) {
                    String t1 = safe(c.title);
                    String t2 = safe(c.imgTitle);
                    String t3 = safe(c.intro);
                    if (t1.contains(q) || t2.contains(q) || t3.contains(q)) {
                        filtered.add(c);
                    }
                }
            } else {
                filtered = flat; // 空查询显示全部
            }
            final List<CourseBean> result = filtered;
            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.submitList(result);
                }
                if (result.isEmpty()) {
                    Toast.makeText(CourseSearchActivity.this, getString(R.string.course_search_no_result), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private List<CourseBean> flatten(List<List<CourseBean>> groups) {
        List<CourseBean> out = new ArrayList<>();
        if (groups != null) {
            for (List<CourseBean> g : groups) {
                if (g != null) out.addAll(g);
            }
        }
        return out;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
