package com.example.xinqiao.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.CourseSearchActivity;
import com.example.xinqiao.adapter.CourseListAdapter;
import com.example.xinqiao.bean.CourseBean;
import com.example.xinqiao.util.AnalysisUtils;
import com.example.xinqiao.dao.CourseClickDao;
import com.example.xinqiao.util.ui.RecyclerViewOptimizer;
import com.example.xinqiao.util.ui.SpacesItemDecoration;

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
    // 顶部全局标题栏中的控件（来自 MainActivity），进入本页需隐藏
    private View topSearchBox; // R.id.et_search_course
    private View topPlayHistory; // R.id.ll_play_history_top
    private View titleBar; // R.id.title_bar
    // 底部导航栏（来自 MainActivity），进入搜索页需隐藏
    private View bottomBar; // R.id.main_bottom_bar
    private Integer prevTopSearchVisibility;
    private Integer prevTopHistoryVisibility;
    private Integer prevTitleBarVisibility;
    private Integer prevBottomBarVisibility;

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

        // 防止进入页面时自动弹出键盘：清除焦点并隐藏软键盘
        try {
            view.setFocusableInTouchMode(true);
            view.requestFocus();
            requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            if (etSearch != null) etSearch.clearFocus();
        } catch (Throwable ignored) {}

        // 隐藏 MainActivity 标题栏里的课程搜索框与播放历史，避免重复显示
        titleBar = requireActivity().findViewById(R.id.title_bar);
        topSearchBox = requireActivity().findViewById(R.id.et_search_course);
        topPlayHistory = requireActivity().findViewById(R.id.ll_play_history_top);
        bottomBar = requireActivity().findViewById(R.id.main_bottom_bar);
        if (titleBar != null) {
            prevTitleBarVisibility = titleBar.getVisibility();
            titleBar.setVisibility(View.GONE);
        }
        if (topSearchBox != null) {
            prevTopSearchVisibility = topSearchBox.getVisibility();
            topSearchBox.setVisibility(View.GONE);
        }
        if (topPlayHistory != null) {
            prevTopHistoryVisibility = topPlayHistory.getVisibility();
            topPlayHistory.setVisibility(View.GONE);
        }
        // 隐藏底部导航栏
        if (bottomBar != null) {
            prevBottomBarVisibility = bottomBar.getVisibility();
            bottomBar.setVisibility(View.GONE);
        }

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

        // 热搜推荐列表（单列样式，左图右文）
        adapter = new CourseListAdapter(requireContext(), R.layout.course_search_item);
        if (rvHot != null) {
            LinearLayoutManager llm = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
            rvHot.setLayoutManager(llm);
            int spacing = dpToPx(8);
            rvHot.addItemDecoration(new SpacesItemDecoration(spacing, true));
            rvHot.setClipToPadding(false);
            rvHot.setPadding(spacing, spacing, spacing, spacing);
            RecyclerViewOptimizer.optimizeDefault(rvHot);
            rvHot.setAdapter(adapter);
        }

        // 加载热搜推荐：使用点击热度（videoplaylist聚合），失败时回退为前8课程
        loadHotRecommendations();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 恢复 MainActivity 标题栏控件的可见性
        try {
            if (titleBar != null && prevTitleBarVisibility != null) {
                titleBar.setVisibility(prevTitleBarVisibility);
            }
            if (topSearchBox != null && prevTopSearchVisibility != null) {
                topSearchBox.setVisibility(prevTopSearchVisibility);
            }
            if (topPlayHistory != null && prevTopHistoryVisibility != null) {
                topPlayHistory.setVisibility(prevTopHistoryVisibility);
            }
            // 恢复底部导航栏
            if (bottomBar != null && prevBottomBarVisibility != null) {
                bottomBar.setVisibility(prevBottomBarVisibility);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        // 返回本页时确保标题栏与外层搜索框保持隐藏，避免出现双搜索框
        try {
            if (titleBar == null) titleBar = requireActivity().findViewById(R.id.title_bar);
            if (topSearchBox == null) topSearchBox = requireActivity().findViewById(R.id.et_search_course);
            if (topPlayHistory == null) topPlayHistory = requireActivity().findViewById(R.id.ll_play_history_top);
            if (bottomBar == null) bottomBar = requireActivity().findViewById(R.id.main_bottom_bar);
            if (titleBar != null) titleBar.setVisibility(View.GONE);
            if (topSearchBox != null) topSearchBox.setVisibility(View.GONE);
            if (topPlayHistory != null) topPlayHistory.setVisibility(View.GONE);
            if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        } catch (Throwable ignored) {}
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
            // 1) 解析课程XML，建立 id -> CourseBean 的索引与扁平列表
            List<List<CourseBean>> groups = new ArrayList<>();
            try {
                InputStream is = requireContext().getResources().getAssets().open("chaptertitle.xml");
                groups = AnalysisUtils.getCourseInfos(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
            final List<CourseBean> flat = new ArrayList<>();
            for (List<CourseBean> g : groups) {
                if (g != null) flat.addAll(g);
            }
            // 构建索引用于根据 chapterId 查找 CourseBean
            final java.util.HashMap<Integer, CourseBean> index = new java.util.HashMap<>();
            for (CourseBean cb : flat) {
                index.put(cb.id, cb);
            }

            // 2) 调用点击热度DAO，按chapterId聚合并取前8
            try {
                CourseClickDao dao = new CourseClickDao(requireContext());
                dao.getGlobalCourseClickRankAsync(8, new CourseClickDao.ChapterCountCallback() {
                    @Override
                    public void onSuccess(List<CourseClickDao.ChapterCount> list) {
                        List<CourseBean> result = new ArrayList<>();
                        for (CourseClickDao.ChapterCount cc : list) {
                            CourseBean bean = index.get(cc.chapterId);
                            if (bean != null) {
                                result.add(bean);
                            }
                        }
                        // 若返回数量不足，使用剩余课程按id补足到8条
                        if (result.size() < 8) {
                            // 按id排序补充
                            List<CourseBean> sorted = new ArrayList<>(flat);
                            Collections.sort(sorted, new Comparator<CourseBean>() {
                                @Override
                                public int compare(CourseBean o1, CourseBean o2) {
                                    return Integer.compare(o1.id, o2.id);
                                }
                            });
                            for (CourseBean cb : sorted) {
                                if (result.size() >= 8) break;
                                boolean already = false;
                                for (CourseBean r : result) {
                                    if (r.id == cb.id) { already = true; break; }
                                }
                                if (!already) result.add(cb);
                            }
                        }
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (adapter != null) adapter.submitList(new ArrayList<>(result));
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        // 3) 回退策略：按id排序取前8
                        List<CourseBean> sorted = new ArrayList<>(flat);
                        Collections.sort(sorted, new Comparator<CourseBean>() {
                            @Override
                            public int compare(CourseBean o1, CourseBean o2) {
                                return Integer.compare(o1.id, o2.id);
                            }
                        });
                        final List<CourseBean> top8 = sorted.size() > 8 ? sorted.subList(0, 8) : sorted;
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (adapter != null) adapter.submitList(new ArrayList<>(top8));
                            });
                        }
                    }
                });
            } catch (Throwable t) {
                // DAO 构造或调用异常时，直接回退
                List<CourseBean> sorted = new ArrayList<>(flat);
                Collections.sort(sorted, new Comparator<CourseBean>() {
                    @Override
                    public int compare(CourseBean o1, CourseBean o2) {
                        return Integer.compare(o1.id, o2.id);
                    }
                });
                final List<CourseBean> top8 = sorted.size() > 8 ? sorted.subList(0, 8) : sorted;
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (adapter != null) adapter.submitList(new ArrayList<>(top8));
                    });
                }
            }
        }).start();
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
