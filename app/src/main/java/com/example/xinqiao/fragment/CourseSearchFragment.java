package com.example.xinqiao.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.CourseSearchActivity;
import com.example.xinqiao.adapter.CourseListAdapter;
import com.example.xinqiao.bean.CourseBean;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.util.RecyclerViewOptimizer;
import com.example.xinqiao.util.SpacesItemDecoration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 课程搜索页 Fragment：顶部搜索栏（搜索框 + 取消），下方热搜推荐（课程封面+标题）。
 * 点击取消：返回上一页（若无返回栈则关闭宿主 Activity）。
 * 点击课程卡片：跳转到对应课程页（VideoListActivity，通过 CourseListAdapter 实现）。
 */
public class CourseSearchFragment extends Fragment {
    private EditText etSearch;
    private TextView tvCancel;
    private RecyclerView rvHot;
    private CourseListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_course_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etSearch = view.findViewById(R.id.et_search);
        tvCancel = view.findViewById(R.id.tv_cancel);
        rvHot = view.findViewById(R.id.rv_hot_list);

        // 取消按钮：优先弹出 Fragment 返回栈，否则关闭宿主 Activity
        if (tvCancel != null) {
            tvCancel.setOnClickListener(v -> {
                hideKeyboard();
                if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    requireActivity().finish();
                }
            });
        }

        // 搜索框：IME_ACTION_SEARCH / 回车，跳转到 CourseSearchActivity 展示结果
        if (etSearch != null) {
            etSearch.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
                boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN;
                boolean isSearch = actionId == EditorInfo.IME_ACTION_SEARCH;
                if (isSearch || isEnter) {
                    String q = v.getText() != null ? v.getText().toString().trim() : "";
                    hideKeyboard();
                    if (!TextUtils.isEmpty(q)) {
                        Intent intent = new Intent(requireContext(), CourseSearchActivity.class);
                        intent.putExtra("query", q);
                        startActivity(intent);
                    }
                    return true;
                }
                return false;
            });
        }

        // 热搜推荐列表
        adapter = new CourseListAdapter(requireContext());
        if (rvHot != null) {
            StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
            sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
            rvHot.setLayoutManager(sglm);
            int spacing = dpToPx(8);
            rvHot.addItemDecoration(new SpacesItemDecoration(spacing, true));
            rvHot.setClipToPadding(false);
            rvHot.setPadding(spacing, spacing, spacing, spacing);
            RecyclerViewOptimizer.optimizeDefault(rvHot);
            rvHot.setAdapter(adapter);
        }

        // 加载热搜推荐（示例选择前 8 个课程）
        loadHotRecommendations();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            View v = getView();
            if (imm != null && v != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Throwable ignored) {}
    }

    private void loadHotRecommendations() {
        new Thread(() -> {
            List<List<CourseBean>> groups = new ArrayList<>();
            try {
                InputStream is = requireContext().getResources().getAssets().open("chaptertitle.xml");
                groups = AnalysisUtils.getCourseInfos(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<CourseBean> flat = new ArrayList<>();
            for (List<CourseBean> g : groups) {
                if (g != null) flat.addAll(g);
            }
            // 依据 id 简单排序，取前 8 作为热搜推荐
            Collections.sort(flat, new Comparator<CourseBean>() {
                @Override
                public int compare(CourseBean o1, CourseBean o2) {
                    return Integer.compare(o1.id, o2.id);
                }
            });
            final List<CourseBean> top8 = flat.size() > 8 ? flat.subList(0, 8) : flat;
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (adapter != null) adapter.submitList(new ArrayList<>(top8));
                });
            }
        }).start();
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}

