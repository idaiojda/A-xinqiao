package com.example.xinqiao.fragment;

import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.ConsultationRecordAdapter;
import com.example.xinqiao.bean.ConsultationItem;
import com.example.xinqiao.activity.ConsultationDetailActivity;
import com.example.xinqiao.activity.ConsultantDetailActivity;
import com.example.xinqiao.repository.MedicalRecordRepository;
import com.example.xinqiao.room.entities.ConsultationEntity;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.util.CryptoUtil;
import com.example.xinqiao.util.RecyclerViewOptimizer;
import android.widget.Toast;
import android.content.ActivityNotFoundException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConsultationRecordsFragment extends Fragment {
    private RecyclerView rvList;
    private Button btnFilter;
    private TextView tvStartDate, tvEndDate;
    private RadioGroup rgType;
    private ConsultationRecordAdapter adapter;
    private List<ConsultationItem> items = new ArrayList<>();
    private MedicalRecordRepository repo;
    private String userName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_consultation_records, container, false);
        rvList = view.findViewById(R.id.rv_consultation_records);
        btnFilter = view.findViewById(R.id.btn_filter);
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        rgType = view.findViewById(R.id.rg_type);

        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConsultationRecordAdapter(items);
        rvList.setAdapter(adapter);
        RecyclerViewOptimizer.optimizeDefault(rvList);

        repo = new MedicalRecordRepository(requireContext());
        userName = AnalysisUtils.readLoginUserName(requireContext());
        initDefaultDates();
        refreshFromRepo();

        adapter.setOnItemLongClickListener((position, item) -> showActionDialog(inflater, position, item));
        adapter.setOnItemClickListener((position, item) -> {
            int sid = safeParseSessionId(item.sessionId);
            boolean isAi = item != null && item.type != null && item.type.equalsIgnoreCase("ai");

            if (isAi) {
                // AI 咨询记录应进入 AI 详情页，若未注册则提示，不再错误回退到咨询师页
                try {
                    Intent it = new Intent(requireContext(), ConsultationDetailActivity.class);
                    it.putExtra("title", item.title);
                    it.putExtra("sessionId", sid);
                    startActivity(it);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(requireContext(), "AI 详情页未注册，请更新清单或版本", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 专业咨询记录进入咨询师详情页
                try {
                    Intent fallback = new Intent(requireContext(), ConsultantDetailActivity.class);
                    fallback.putExtra("title", item.title);
                    fallback.putExtra("sessionId", sid);
                    startActivity(fallback);
                } catch (ActivityNotFoundException e2) {
                    Toast.makeText(requireContext(), "咨询师详情页未注册，已取消跳转", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // 移除“筛选”按钮点击，改为即时生效
        tvStartDate.setOnClickListener(v -> pickDate(tvStartDate));
        tvEndDate.setOnClickListener(v -> pickDate(tvEndDate));
        // 类型切换即时触发筛选
        rgType.setOnCheckedChangeListener((group, checkedId) -> doFilter());
        return view;
    }

    private void showActionDialog(LayoutInflater inflater, int position, ConsultationItem item) {
        String[] actions = new String[]{"查看摘要"};
        new AlertDialog.Builder(requireContext())
                .setTitle(item.title != null ? item.title : "记录")
                .setItems(actions, (dialog, which) -> {
                    // 简化：仅展示摘要内容
                    new AlertDialog.Builder(requireContext())
                            .setTitle("摘要")
                            .setMessage(item.summary != null ? item.summary : "暂无摘要")
                            .setPositiveButton("关闭", null)
                            .show();
                })
                .show();
    }

    private void refreshFromRepo() {
        repo.getConsultationsAsync(userName != null ? userName : "", true,
                new MedicalRecordRepository.ConsultationsCallback() {
                    @Override
                    public void onSuccess(List<ConsultationEntity> list) {
                        requireActivity().runOnUiThread(() -> applyRepoData(list));
                    }
                    @Override
                    public void onError(Exception e) {
                        requireActivity().runOnUiThread(() -> applyRepoData(new ArrayList<>()));
                    }
                });
    }

    private void doFilter() {
        String start = tvStartDate.getText() != null ? tvStartDate.getText().toString().trim() : "";
        String end = tvEndDate.getText() != null ? tvEndDate.getText().toString().trim() : "";
        int checkedId = rgType.getCheckedRadioButtonId();
        final String typeFilter;
        if (checkedId == R.id.rb_type_ai) typeFilter = "ai";
        else if (checkedId == R.id.rb_type_pro) typeFilter = "pro";
        else typeFilter = null;

        MedicalRecordRepository.ConsultationsCallback cb = new MedicalRecordRepository.ConsultationsCallback() {
            @Override
            public void onSuccess(List<ConsultationEntity> list) {
                List<ConsultationEntity> base = list != null ? list : new ArrayList<>();
                if (typeFilter != null) {
                    List<ConsultationEntity> filtered = new ArrayList<>();
                    for (ConsultationEntity e : base) {
                        if (typeFilter.equalsIgnoreCase(e.type)) filtered.add(e);
                    }
                    requireActivity().runOnUiThread(() -> applyRepoData(filtered));
                } else {
                    requireActivity().runOnUiThread(() -> applyRepoData(base));
                }
            }
            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() -> applyRepoData(new ArrayList<>()));
            }
        };

        if (start.isEmpty() || end.isEmpty()) {
            repo.getConsultationsAsync(userName != null ? userName : "", true, cb);
        } else {
            repo.getConsultationsByDateRangeAsync(userName != null ? userName : "", start, end, cb);
        }
    }

    private void applyRepoData(List<ConsultationEntity> list) {
        List<ConsultationItem> ui = new ArrayList<>();
        for (ConsultationEntity e : list) {
            ConsultationItem item = new ConsultationItem();
            item.id = e.id;
            item.sessionId = e.sessionId;
            item.type = e.type;
            item.title = e.title != null ? e.title : ("咨询会话 " + (e.sessionId != null ? e.sessionId : ""));
            item.date = e.date;
            item.messageCount = e.messageCount;
            item.status = e.status;
            item.summary = CryptoUtil.decrypt(e.summaryEncrypted);
            ui.add(item);
        }
        items = ui;
        adapter.setData(items);
    }

    private void initDefaultDates() {
        String end = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long sevenDays = 6L * 24L * 60L * 60L * 1000L;
        String start = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(System.currentTimeMillis() - sevenDays));
        tvStartDate.setText(start);
        tvEndDate.setText(end);
    }

    private void pickDate(TextView target) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String mm = String.format(Locale.getDefault(), "%02d", month + 1);
            String dd = String.format(Locale.getDefault(), "%02d", dayOfMonth);
            target.setText(year + "-" + mm + "-" + dd);
            // 日期变更后即时触发筛选
            doFilter();
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private int safeParseSessionId(String sidStr) {
        if (sidStr == null) return 0;
        try {
            return Integer.parseInt(sidStr.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
