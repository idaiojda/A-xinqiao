package com.example.xinqiao.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.example.xinqiao.widget.SimpleScoreMarkerView;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.TestReportAdapter;
import com.example.xinqiao.bean.TestReportItem;
import com.example.xinqiao.repository.MedicalRecordRepository;
import com.example.xinqiao.room.entities.TestReportEntity;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.util.CryptoUtil;
import com.example.xinqiao.util.RecyclerViewOptimizer;
import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TestReportListFragment extends Fragment {
    private RecyclerView rvList;
    private TextView tvStartDate, tvEndDate;
    private Button btnFilter;
    private RadioGroup rgSort;
    private TestReportAdapter adapter;
    private List<TestReportItem> items = new ArrayList<>();
    private MedicalRecordRepository repo;
    private String userName;
    private LineChart lineScoreTrend;
    private Button btnToggleChart;
    private PieChart pieRiskDist;
    private Button btnToggleRisk;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_report_list, container, false);
        rvList = view.findViewById(R.id.rv_test_reports);
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        btnFilter = view.findViewById(R.id.btn_filter);
        rgSort = view.findViewById(R.id.rg_sort);
        btnToggleChart = view.findViewById(R.id.btn_toggle_chart);
        lineScoreTrend = view.findViewById(R.id.line_score_trend);
        btnToggleRisk = view.findViewById(R.id.btn_toggle_risk);
        pieRiskDist = view.findViewById(R.id.pie_risk_dist);

        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TestReportAdapter(items);
        rvList.setAdapter(adapter);
        RecyclerViewOptimizer.optimizeDefault(rvList);

        repo = new MedicalRecordRepository(requireContext());
        userName = AnalysisUtils.readLoginUserName(requireContext());

        initDefaultDates();
        refreshFromRepo();

        // 移除“筛选”按钮点击，改为即时生效
        tvStartDate.setOnClickListener(v -> pickDate(tvStartDate));
        tvEndDate.setOnClickListener(v -> pickDate(tvEndDate));

        // 排序切换即时触发
        rgSort.setOnCheckedChangeListener((group, checkedId) -> doFilter());

        // 图表切换
        if (btnToggleChart != null) {
            btnToggleChart.setOnClickListener(v -> {
                if (lineScoreTrend == null) return;
                if (lineScoreTrend.getVisibility() == View.VISIBLE) {
                    lineScoreTrend.setVisibility(View.GONE);
                    btnToggleChart.setText("显示图表");
                } else {
                    lineScoreTrend.setVisibility(View.VISIBLE);
                    btnToggleChart.setText("隐藏图表");
                    renderScoreTrendChart(items);
                }
            });
        }

        if (btnToggleRisk != null) {
            btnToggleRisk.setOnClickListener(v -> {
                if (pieRiskDist == null) return;
                if (pieRiskDist.getVisibility() == View.VISIBLE) {
                    pieRiskDist.setVisibility(View.GONE);
                    btnToggleRisk.setText("显示风险分布");
                } else {
                    pieRiskDist.setVisibility(View.VISIBLE);
                    btnToggleRisk.setText("隐藏风险分布");
                    renderRiskDistributionChart(items);
                }
            });
        }

        adapter.setOnItemLongClickListener((position, item) -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("报告详情预览")
                    .setMessage(item.details != null ? item.details : "")
                    .setPositiveButton("关闭", null)
                    .show();
        });

        adapter.setOnItemClickListener((position, item) -> {
            TestReportDetailDialogFragment dialog = TestReportDetailDialogFragment.newInstance(item);
            dialog.show(getParentFragmentManager(), "report_detail");
        });

        return view;
    }

    private void refreshFromRepo() {
        String start = tvStartDate.getText() != null ? tvStartDate.getText().toString() : null;
        String end = tvEndDate.getText() != null ? tvEndDate.getText().toString() : null;
        MedicalRecordRepository.TestReportsCallback cb = new MedicalRecordRepository.TestReportsCallback() {
            @Override
            public void onSuccess(List<TestReportEntity> list) {
                requireActivity().runOnUiThread(() -> applyRepoDataFilteredSorted(list));
            }
            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() -> applyRepoDataFilteredSorted(new ArrayList<>()));
            }
        };
        if (start == null || end == null || start.isEmpty() || end.isEmpty()) {
            repo.getTestReportsAsync(userName != null ? userName : "", true, cb);
        } else {
            repo.getTestReportsByDateRangeAsync(userName != null ? userName : "", start, end, cb);
        }
    }

    private void doFilter() { refreshFromRepo(); }

    private void applyRepoData(List<TestReportEntity> list) {
        items.clear();
        if (list != null) {
            for (TestReportEntity e : list) {
                TestReportItem item = new TestReportItem();
                item.id = e.id;
                item.reportId = e.reportId;
                item.type = e.type;
                item.score = e.score;
                item.riskLevel = e.riskLevel;
                item.date = e.date;
                item.details = CryptoUtil.decrypt(e.detailsEncrypted);
                items.add(item);
            }
        }
        adapter.setData(new ArrayList<>(items));
    }

    private void applyRepoDataFilteredSorted(List<TestReportEntity> list) {
        // 不再按类型/风险过滤，仅使用日期范围与排序
        List<TestReportEntity> source = list != null ? list : new ArrayList<>();

        // map to UI items
        List<TestReportItem> mapped = new ArrayList<>();
        for (TestReportEntity e : source) {
            TestReportItem item = new TestReportItem();
            item.id = e.id;
            item.reportId = e.reportId;
            item.type = e.type;
            item.score = e.score;
            item.riskLevel = e.riskLevel;
            item.date = e.date;
            item.details = CryptoUtil.decrypt(e.detailsEncrypted);
            mapped.add(item);
        }

        // sort
        boolean sortByScore = rgSort != null && rgSort.getCheckedRadioButtonId() == R.id.rb_sort_score;
        if (sortByScore) {
            mapped.sort((a, b) -> Float.compare(b.score, a.score));
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            mapped.sort((a, b) -> {
                try {
                    java.util.Date da = a.date != null ? sdf.parse(a.date) : null;
                    java.util.Date db = b.date != null ? sdf.parse(b.date) : null;
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return Long.compare(db.getTime(), da.getTime());
                } catch (Exception ex) {
                    return 0;
                }
            });
        }

        items = mapped;
        adapter.setData(new ArrayList<>(items));
        if (lineScoreTrend != null && lineScoreTrend.getVisibility() == View.VISIBLE) {
            renderScoreTrendChart(items);
        }
        if (pieRiskDist != null && pieRiskDist.getVisibility() == View.VISIBLE) {
            renderRiskDistributionChart(items);
        }
    }

    private void renderScoreTrendChart(List<TestReportItem> source) {
        if (lineScoreTrend == null) return;
        // 基于筛选后的数据，按日期聚合求均值
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Map<String, java.util.List<Float>> scoreMap = new java.util.LinkedHashMap<>();
        java.util.List<String> labels = new java.util.ArrayList<>();

        // 仅在选择了具体类型时更有意义；若为“全部”，仍以每日平均显示
        for (TestReportItem item : source) {
            String day = item.date != null ? item.date : "";
            if (!scoreMap.containsKey(day)) {
                scoreMap.put(day, new java.util.ArrayList<>());
            }
            scoreMap.get(day).add(item.score);
        }

        // 对日期排序
        java.util.List<String> sortedDays = new java.util.ArrayList<>(scoreMap.keySet());
        try {
            java.util.Collections.sort(sortedDays, (a, b) -> {
                try {
                    java.util.Date da = (a != null && !a.isEmpty()) ? sdf.parse(a) : null;
                    java.util.Date db = (b != null && !b.isEmpty()) ? sdf.parse(b) : null;
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return Long.compare(da.getTime(), db.getTime());
                } catch (Exception ex) { return 0; }
            });
        } catch (Exception ignore) {}

        java.util.List<Entry> entries = new java.util.ArrayList<>();
        labels.clear();
        int idx = 0;
        for (String day : sortedDays) {
            java.util.List<Float> scores = scoreMap.get(day);
            if (scores == null || scores.isEmpty()) continue;
            float sum = 0f;
            for (Float s : scores) sum += (s != null ? s : 0f);
            float avg = sum / scores.size();
            entries.add(new Entry(idx, avg));
            labels.add(day);
            idx++;
        }

        LineDataSet set = new LineDataSet(entries, "分数趋势");
        set.setColor(0xFF3F51B5);
        set.setCircleColor(0xFF3F51B5);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(set);
        lineScoreTrend.setData(data);
        lineScoreTrend.getDescription().setEnabled(false);
        lineScoreTrend.setDrawGridBackground(false);
        lineScoreTrend.setPinchZoom(true);

        XAxis xAxis = lineScoreTrend.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        lineScoreTrend.getAxisRight().setEnabled(false);
        lineScoreTrend.getAxisLeft().setGranularity(1f);

        // MarkerView 显示日期与分数
        try {
            SimpleScoreMarkerView marker = new SimpleScoreMarkerView(requireContext(), labels);
            lineScoreTrend.setMarker(marker);
        } catch (Exception ignore) {}

        lineScoreTrend.invalidate();
    }

    private void renderRiskDistributionChart(List<TestReportItem> source) {
        if (pieRiskDist == null) return;
        if (source == null) source = new ArrayList<>();

        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        counts.put("低风险", 0);
        counts.put("中风险", 0);
        counts.put("高风险", 0);
        counts.put("未知", 0);

        for (TestReportItem item : source) {
            String risk = item.riskLevel;
            if (risk == null || risk.trim().isEmpty()) {
                counts.put("未知", counts.get("未知") + 1);
            } else if (risk.contains("低")) {
                counts.put("低风险", counts.get("低风险") + 1);
            } else if (risk.contains("中")) {
                counts.put("中风险", counts.get("中风险") + 1);
            } else if (risk.contains("高")) {
                counts.put("高风险", counts.get("高风险") + 1);
            } else {
                counts.put("未知", counts.get("未知") + 1);
            }
        }

        int total = 0;
        for (Integer v : counts.values()) total += (v != null ? v : 0);
        java.util.List<PieEntry> entries = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                float percent = total > 0 ? (e.getValue() * 100f / total) : 0f;
                String label = String.format(java.util.Locale.getDefault(), "%s %d（%.0f%%）", e.getKey(), e.getValue(), percent);
                entries.add(new PieEntry(e.getValue(), label));
            }
        }

        pieRiskDist.getDescription().setEnabled(false);
        pieRiskDist.setUsePercentValues(true);
        pieRiskDist.setDrawEntryLabels(true);
        pieRiskDist.setEntryLabelColor(Color.DKGRAY);
        pieRiskDist.setEntryLabelTextSize(12f);
        pieRiskDist.setDrawHoleEnabled(true);
        pieRiskDist.setHoleColor(Color.TRANSPARENT);
        pieRiskDist.setCenterText("风险分布");

        if (entries.isEmpty()) {
            pieRiskDist.clear();
            pieRiskDist.invalidate();
            return;
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setSliceSpace(2f);
        set.setSelectionShift(6f);
        set.setDrawValues(false);
        java.util.List<Integer> colors = java.util.Arrays.asList(
                Color.parseColor("#4CAF50"), // 低风险-绿
                Color.parseColor("#FF9800"), // 中风险-橙
                Color.parseColor("#F44336"), // 高风险-红
                Color.parseColor("#9E9E9E")  // 未知-灰
        );
        set.setColors(colors);

        PieData data = new PieData(set);
        
        pieRiskDist.setData(data);
        pieRiskDist.invalidate();
    }

    private void initDefaultDates() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String end = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -30);
        String start = sdf.format(cal.getTime());
        tvStartDate.setText(start);
        tvEndDate.setText(end);
    }

    private void pickDate(TextView target) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH);
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
        android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(requireContext(), (view, y, m, d) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
            target.setText(date);
            // 日期变更后即时触发筛选
            doFilter();
        }, year, month, day);
        dlg.show();
    }
}
