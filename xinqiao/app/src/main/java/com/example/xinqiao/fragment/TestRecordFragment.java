package com.example.xinqiao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.xinqiao.R;
import com.example.xinqiao.activity.TestReportActivity;
import com.example.xinqiao.bean.TestRecord;
import com.example.xinqiao.dao.TestRecordDao;
import com.example.xinqiao.util.ImageLoader;
import com.example.xinqiao.util.RecyclerViewOptimizer;
import com.example.xinqiao.view.ExercisesView;
import com.example.xinqiao.utils.AnalysisUtils;
import java.util.ArrayList;
import java.util.List;

public class TestRecordFragment extends Fragment {
    private TextView tvTabUnfinished, tvTabFinished, tvTabPending, tvTitle, tvOrderTip;
    private RecyclerView rvList;
    private View indicatorUnfinished, indicatorFinished, indicatorPending;
    private View emptyView;
    private int currentTab = 1; // 0未完成 1已完成 2待支付
    private TestRecordDao testRecordDao;
    private String userName;
    private TestRecordAdapter adapter;
    private List<TestRecord> showList = new ArrayList<>();
    private ProgressBar progressBar;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable loadTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_record, container, false);
        tvTitle = view.findViewById(R.id.tv_title);
        tvOrderTip = view.findViewById(R.id.tv_order_tip);
        tvTabUnfinished = view.findViewById(R.id.tv_tab_unfinished);
        tvTabFinished = view.findViewById(R.id.tv_tab_finished);
        tvTabPending = view.findViewById(R.id.tv_tab_pending);
        indicatorUnfinished = view.findViewById(R.id.indicator_unfinished);
        indicatorFinished = view.findViewById(R.id.indicator_finished);
        indicatorPending = view.findViewById(R.id.indicator_pending);
        rvList = view.findViewById(R.id.rv_test_record);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // 初始化空状态视图
        emptyView = LayoutInflater.from(getContext()).inflate(R.layout.empty_test_record, (ViewGroup) rvList.getParent(), false);
        emptyView.findViewById(R.id.btn_go_test).setOnClickListener(v -> {
            // 跳转到测评列表页面 - 使用MainActivity的方式切换到习题页面
            if (getActivity() != null) {
                if (getActivity() instanceof com.example.xinqiao.activity.MainActivity) {
                    // 使用commitNow确保立即执行，避免异步导致的问题
                    getParentFragmentManager().beginTransaction()
                        .remove(this)
                        .commitNow();
                    
                    com.example.xinqiao.activity.MainActivity mainActivity = 
                        (com.example.xinqiao.activity.MainActivity) getActivity();
                    // 调用MainActivity中的方法切换到习题页面(索引为1)
                    mainActivity.onClick(mainActivity.findViewById(R.id.bottom_bar_exercises_btn));
                }
            }
        });
        ((ViewGroup) rvList.getParent()).addView(emptyView);
        emptyView.setVisibility(View.GONE);
        tvTitle.setText("测试记录");
        tvOrderTip.setText("查不到测评完成的报告？请点击此处进行订单查询");
        tvTabUnfinished.setOnClickListener(v -> switchTab(0));
        tvTabFinished.setOnClickListener(v -> switchTab(1));
        tvTabPending.setOnClickListener(v -> switchTab(2));
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            // 修复返回逻辑，确保Fragment被正确移除
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        view.findViewById(R.id.btn_more_test).setOnClickListener(v -> Toast.makeText(getContext(), "查看更多精彩测评", Toast.LENGTH_SHORT).show());
        progressBar = view.findViewById(R.id.progressBar);
        testRecordDao = new TestRecordDao(requireContext());
        userName = AnalysisUtils.readLoginUserName(requireContext());
        adapter = new TestRecordAdapter(showList);
        rvList.setAdapter(adapter);
        
        // 优化RecyclerView性能
        RecyclerViewOptimizer.optimizeDefault(rvList);
        loadRecords(currentTab);
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        // 只有当列表为空时才重新加载，避免重复加载
        if (showList == null || showList.isEmpty()) {
            loadRecords(currentTab);
        }
    }
    private void switchTab(int tab) {
        currentTab = tab;
        tvTabUnfinished.setTextColor(tab==0?0xFF222222:0xFFBBBBBB);
        tvTabFinished.setTextColor(tab==1?0xFF222222:0xFFBBBBBB);
        tvTabPending.setTextColor(tab==2?0xFF222222:0xFFBBBBBB);
        indicatorUnfinished.setVisibility(tab==0?View.VISIBLE:View.INVISIBLE);
        indicatorFinished.setVisibility(tab==1?View.VISIBLE:View.INVISIBLE);
        indicatorPending.setVisibility(tab==2?View.VISIBLE:View.INVISIBLE);
        // 防抖处理
        if (loadTask != null) handler.removeCallbacks(loadTask);
        loadTask = () -> loadRecords(tab);
        handler.postDelayed(loadTask, 300);
    }
    private void loadRecords(int status) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        android.util.Log.d("TestRecordDebug", "查询 userName=" + userName + ", status=" + status);
        
        testRecordDao.getTestRecordsByStatusAsync(userName, status, new TestRecordDao.TestRecordCallback() {
            @Override
            public void onSuccess(List<TestRecord> newList) {
                android.util.Log.d("TestRecordDebug", "查到数据条数=" + (newList == null ? 0 : newList.size()));
                if (getActivity() != null && isAdded()) {
                    showList.clear();
                    if (newList != null) {
                        // 使用HashSet去除重复记录 - 同时基于title和reportId去重
                        java.util.Set<String> titleSet = new java.util.HashSet<>();
                        java.util.List<TestRecord> uniqueList = new java.util.ArrayList<>();
                        
                        for (TestRecord record : newList) {
                            // 使用title作为唯一标识，确保相同标题的测试只显示一次
                            if (!titleSet.contains(record.title)) {
                                titleSet.add(record.title);
                                uniqueList.add(record);
                                android.util.Log.d("TestRecordDebug", "添加记录: " + record.title + ", reportId=" + record.reportId);
                            } else {
                                android.util.Log.d("TestRecordDebug", "过滤重复记录: " + record.title + ", reportId=" + record.reportId);
                            }
                        }
                        
                        showList.addAll(uniqueList);
                        android.util.Log.d("TestRecordDebug", "去重后数据条数=" + uniqueList.size());
                    }
                    adapter.notifyDataSetChanged();
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    
                    // 显示或隐藏空状态视图
                    if (showList.isEmpty()) {
                        rvList.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        rvList.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    }
                }
            }
            
            @Override
            public void onError(Exception e) {
                android.util.Log.e("TestRecordDebug", "查询失败: " + e.getMessage());
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "加载数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    static class TestRecordAdapter extends RecyclerView.Adapter<TestRecordAdapter.VH> {
        List<TestRecord> data;
        private int lastPosition = -1; // 用于记录最后一个动画位置
        
        TestRecordAdapter(List<TestRecord> data){ this.data = data; }
        
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test_record, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TestRecord r = data.get(position);
            holder.tvTitle.setText(r.title);
            holder.tvDesc.setText(r.desc);
            
            // 确保图片资源ID有效
            final int imageResId;
            if (r.imageResId != 0) {
                imageResId = r.imageResId;
            } else {
                // 如果imageResId为0，根据title获取对应的id，然后使用getBackgroundResById方法
                int id = getExerciseIdByTitle(r.title);
                imageResId = com.example.xinqiao.view.ExercisesView.getBackgroundResById(id);
            }
            
            // 使用ImageLoader工具类加载图片
            ImageLoader.loadImage(holder.ivCover.getContext(), imageResId, holder.ivCover);
            
            holder.tvDate.setText(r.date);
            holder.tvFree.setVisibility(r.isFree ? View.VISIBLE : View.GONE);
            holder.btnReport.setVisibility(r.status==1 ? View.VISIBLE : View.GONE);
            
            // 设置状态标签
            String statusText = "";
            int statusColor = 0;
            switch (r.status) {
                case 0:
                    statusText = "未完成";
                    statusColor = 0xFFFF9800; // 橙色
                    break;
                case 1:
                    statusText = "已完成";
                    statusColor = 0xFF4CAF50; // 绿色
                    break;
                case 2:
                    statusText = "待支付";
                    statusColor = 0xFFE91E63; // 粉色
                    break;
            }
            holder.tvStatus.setText(statusText);
            holder.tvStatus.setTextColor(statusColor);
            
            // 添加条目动画
            setAnimation(holder.itemView, position);
            holder.btnReport.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), TestReportActivity.class);
                intent.putExtra("reportId", r.reportId);
                v.getContext().startActivity(intent);
            });
            holder.itemView.setOnClickListener(v -> {
                if (r.status == 0) {
                    // 未完成状态，跳转到测评详情页继续测评
                    Intent intent = new Intent(v.getContext(), com.example.xinqiao.activity.ExercisesDetailActivity.class);
                    intent.putExtra("reportId", r.reportId);
                    intent.putExtra("currentIndex", r.currentIndex);
                    intent.putExtra("answers", r.answers);
                    intent.putExtra("id", 0); // 可选：传递测评id
                    intent.putExtra("title", r.title);
                    // 其它必要参数
                    v.getContext().startActivity(intent);
                } else if (r.status == 2) {
                    // 待支付状态，跳转到报告页面进行支付
                    Intent intent = new Intent(v.getContext(), TestReportActivity.class);
                    intent.putExtra("reportId", r.reportId);
                    v.getContext().startActivity(intent);
                }
            });
        }
        
        // 根据测评标题获取对应的id
        private static int getExerciseIdByTitle(String title) {
            // 这里根据title匹配对应的id
            // 注意：这里的匹配逻辑需要与ExercisesBean中的title一致
            if (title.contains("恋爱心理成熟度")) return 1;
            if (title.contains("社交恐惧症量表")) return 2;
            if (title.contains("交友能力")) return 3;
            if (title.contains("汉密顿抑郁量表")) return 4;
            if (title.contains("焦虑程度")) return 5;
            if (title.contains("抑郁症程度")) return 6;
            if (title.contains("精神压力")) return 7;
            if (title.contains("抑郁应对方式")) return 8;
            if (title.contains("回避型依恋")) return 9;
            if (title.contains("人生质量")) return 10;
            // 默认返回1
            return 1;
        }
        @Override
        public int getItemCount() { return data == null ? 0 : data.size(); }
        // 添加条目动画方法
        private void setAnimation(View viewToAnimate, int position) {
            if (position > lastPosition) {
                android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(
                        viewToAnimate.getContext(), android.R.anim.fade_in);
                animation.setDuration(350);
                viewToAnimate.startAnimation(animation);
                lastPosition = position;
            }
        }
        
        // 当RecyclerView被回收时清除动画状态
        @Override
        public void onViewDetachedFromWindow(@NonNull VH holder) {
            super.onViewDetachedFromWindow(holder);
            holder.itemView.clearAnimation();
        }
        
        static class VH extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvTitle, tvDesc, tvDate, tvFree, tvStatus;
            Button btnReport;
            VH(View itemView){
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvDesc = itemView.findViewById(R.id.tv_desc);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvFree = itemView.findViewById(R.id.tv_free);
                btnReport = itemView.findViewById(R.id.btn_report);
                tvStatus = itemView.findViewById(R.id.tv_status);
            }
        }
    }
}