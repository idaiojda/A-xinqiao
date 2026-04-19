package com.example.xinqiao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.ArticleSearchActivity;
import com.example.xinqiao.adapter.ArticleAdapter;
import com.example.xinqiao.bean.ArticleBean;
import com.example.xinqiao.util.ui.RecyclerViewOptimizer;
import com.example.xinqiao.util.ui.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章搜索页 Fragment：顶部搜索栏（搜索框 + 取消），下方热搜推荐（文章卡片）。
 * 点击取消：返回上一页（若无返回栈则关闭宿主 Activity）。
 * 点击文章卡片：跳转到文章详情页（由 ArticleAdapter 实现）。
 */
public class ArticleSearchFragment extends Fragment {
    private EditText etSearch;
    private TextView tvCancel;
    private RecyclerView rvHot;
    private ArticleAdapter adapter;

    // 进入本页时可尝试隐藏 MainActivity 的全局标题栏控件（若存在）
    private View topSearchBox; // R.id.et_search_course
    private View topPlayHistory; // R.id.ll_play_history_top
    private View titleBar; // R.id.title_bar
    private Integer prevTopSearchVisibility;
    private Integer prevTopHistoryVisibility;
    private Integer prevTitleBarVisibility;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_article_search, container, false);
        etSearch = root.findViewById(R.id.et_search);
        tvCancel = root.findViewById(R.id.tv_cancel);
        rvHot = root.findViewById(R.id.rv_hot_list);

        // 隐藏宿主中的全局标题栏元素（若存在）
        tryHideGlobalTitleBar();

        // 搜索回车/IME Search 跳转到搜索结果页
        if (etSearch != null) {
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
                boolean isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH;
                if (isEnter || isSearchAction) {
                    String q = v.getText() != null ? v.getText().toString().trim() : "";
                    if (!TextUtils.isEmpty(q)) {
                        Intent intent = new Intent(requireContext(), ArticleSearchActivity.class);
                        intent.putExtra("query", q);
                        startActivity(intent);
                    }
                    return true;
                }
                return false;
            });
        }

        // 取消按钮：返回上一页或关闭宿主
        if (tvCancel != null) {
            tvCancel.setOnClickListener(v -> {
                if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    requireActivity().finish();
                }
            });
        }

        // 热搜推荐列表
        adapter = new ArticleAdapter(requireContext(), new ArrayList<>(), false);
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

        // 加载热搜推荐（示例选择前 8 个文章）
        loadHotRecommendations();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 恢复宿主中的全局标题栏元素（若存在）
        restoreGlobalTitleBar();
    }

    private void loadHotRecommendations() {
        // 从后端加载已审核通过的文章
        new Thread(() -> {
            try {
                // 使用Kotlin协程加载文章
                java.util.List<com.example.xinqiao.counselor.ArticleDto> articles = 
                    kotlinx.coroutines.BuildersKt.runBlocking(
                        kotlinx.coroutines.Dispatchers.getIO(),
                        (scope, continuation) -> ArticleLoader.INSTANCE.loadArticles(0, 20, continuation)
                    );
                
                if (articles != null && !articles.isEmpty()) {
                    // 转换为 ArticleBean
                    java.util.List<ArticleBean> beans = convertToArticleBeans(articles);
                    
                    // 更新UI
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.setData(beans);
                            }
                        });
                    }
                } else {
                    // 加载失败，显示空状态
                    showEmptyState("暂无文章数据");
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 加载失败，显示空状态
                showEmptyState("加载失败：" + e.getMessage());
            }
        }).start();
    }
    
    private java.util.List<ArticleBean> convertToArticleBeans(java.util.List<com.example.xinqiao.counselor.ArticleDto> articles) {
        java.util.List<ArticleBean> beans = new java.util.ArrayList<>();
        
        for (int i = 0; i < articles.size(); i++) {
            com.example.xinqiao.counselor.ArticleDto dto = articles.get(i);
            ArticleBean bean = new ArticleBean();
            bean.articleId = (int) dto.getId(); // Long转int
            bean.title = dto.getTitle();
            bean.category = dto.getCategory() != null && !dto.getCategory().isEmpty() 
                ? dto.getCategory() 
                : "心理健康";
            bean.content = dto.getContent();
            // 生成摘要（取前100个字符）
            bean.summary = dto.getContent().length() > 100 
                ? dto.getContent().substring(0, 100) + "..." 
                : dto.getContent();
            
            // 处理图片：解析drawable://前缀
            String imageStr = dto.getImage();
            if (imageStr != null && !imageStr.isEmpty() && imageStr.startsWith("drawable://")) {
                // 提取drawable资源名称，例如 "drawable://bg_11" -> "bg_11"
                String drawableName = imageStr.substring("drawable://".length());
                try {
                    // 通过反射获取drawable资源ID
                    int resId = getResources().getIdentifier(drawableName, "drawable", requireContext().getPackageName());
                    if (resId != 0) {
                        bean.imageResId = resId;
                    } else {
                        // 如果找不到资源，使用占位图片
                        bean.imageResId = R.drawable.bg_11;
                    }
                } catch (Exception e) {
                    bean.imageResId = R.drawable.bg_11;
                }
            } else {
                // 没有图片或格式不对，使用占位图片
                bean.imageResId = R.drawable.bg_11;
            }
            
            // 使用publishedAt作为时间戳
            try {
                if (dto.getPublishedAt() != null && !dto.getPublishedAt().isEmpty()) {
                    // 尝试解析ISO 8601格式的时间戳
                    java.time.Instant instant = java.time.Instant.parse(dto.getPublishedAt());
                    bean.readTimestamp = instant.toEpochMilli();
                } else {
                    bean.readTimestamp = System.currentTimeMillis();
                }
            } catch (Exception e) {
                bean.readTimestamp = System.currentTimeMillis();
            }
            
            beans.add(bean);
        }
        
        return beans;
    }
    
    private void showEmptyState(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.setData(new ArrayList<>());
                }
                // 可以在这里添加Toast或Snackbar提示
                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void tryHideGlobalTitleBar() {
        try {
            View rootActivity = requireActivity().findViewById(android.R.id.content);
            if (rootActivity != null) {
                titleBar = requireActivity().findViewById(R.id.title_bar);
                topSearchBox = requireActivity().findViewById(R.id.et_search_course);
                topPlayHistory = requireActivity().findViewById(R.id.ll_play_history_top);
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
            }
        } catch (Exception ignored) {}
    }

    private void restoreGlobalTitleBar() {
        try {
            if (titleBar != null && prevTitleBarVisibility != null) titleBar.setVisibility(prevTitleBarVisibility);
            if (topSearchBox != null && prevTopSearchVisibility != null) topSearchBox.setVisibility(prevTopSearchVisibility);
            if (topPlayHistory != null && prevTopHistoryVisibility != null) topPlayHistory.setVisibility(prevTopHistoryVisibility);
        } catch (Exception ignored) {}
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
