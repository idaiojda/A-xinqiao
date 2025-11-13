package com.example.xinqiao.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.HealthMetricsAdapter;
import com.example.xinqiao.bean.HealthMetricEntry;
import com.example.xinqiao.util.RecyclerViewOptimizer;
import com.example.xinqiao.util.MedicalRecordStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.formatter.PercentFormatter;

public class HealthMetricsFragment extends Fragment {
    private RecyclerView rvList;
    private Button btnAdd;
    private HealthMetricsAdapter adapter;
    private List<HealthMetricEntry> entries = new ArrayList<>();
    private PieChart pieChart;
    private TextView tvPieTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_health_metrics, container, false);
        rvList = view.findViewById(R.id.rv_health_metrics);
        btnAdd = view.findViewById(R.id.btn_add_metric);
        pieChart = view.findViewById(R.id.pie_metrics_dist);
        tvPieTitle = view.findViewById(R.id.tv_metrics_chart_title);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HealthMetricsAdapter(entries);
        rvList.setAdapter(adapter);
        RecyclerViewOptimizer.optimizeDefault(rvList);
        rvList.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fade_in));

        // 加载持久化数据
        List<HealthMetricEntry> saved = MedicalRecordStorage.loadHealthMetricEntries(requireContext());
        if (saved != null) {
            entries = saved;
            adapter.setData(entries);
            rvList.scheduleLayoutAnimation();
            renderMetricsPie();
        }

        // 长按编辑/删除
        adapter.setOnItemLongClickListener((position, entry) -> showActionDialog(inflater, position, entry));

        btnAdd.setOnClickListener(v -> showAddDialog(inflater));
        return view;
    }

    private void showAddDialog(LayoutInflater inflater) {
        View dialogView = inflater.inflate(R.layout.dialog_add_metric, null);
        Spinner spType = dialogView.findViewById(R.id.sp_metric_type);
        EditText etValue = dialogView.findViewById(R.id.et_metric_value);
        EditText etUnit = dialogView.findViewById(R.id.et_metric_unit);

        String[] types = new String[]{"心率", "血压", "体温", "体重", "血糖"};
        ArrayAdapter<String> adapterType = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(adapterType);

        new AlertDialog.Builder(requireContext())
                .setTitle("新增健康指标")
                .setView(dialogView)
                .setPositiveButton("保存", (d, which) -> {
                    String type = spType.getSelectedItem() != null ? spType.getSelectedItem().toString() : "";
                    String valueStr = etValue.getText() != null ? etValue.getText().toString().trim() : "";
                    String unit = etUnit.getText() != null ? etUnit.getText().toString().trim() : "";
                    float value = 0f;
                    try { value = Float.parseFloat(valueStr); } catch (Exception ignore) {}

                    HealthMetricEntry entry = new HealthMetricEntry();
                    entry.id = System.currentTimeMillis();
                    entry.date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                    entry.type = type;
                    entry.value = value;
                    entry.unit = unit;

                    entries.add(0, entry);
                    adapter.notifyItemInserted(0);
                    rvList.scrollToPosition(0);
                    MedicalRecordStorage.saveHealthMetricEntries(requireContext(), entries);
                    rvList.scheduleLayoutAnimation();
                    renderMetricsPie();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showActionDialog(LayoutInflater inflater, int position, HealthMetricEntry entry) {
        String[] actions = new String[]{"编辑", "删除"};
        new AlertDialog.Builder(requireContext())
                .setTitle("操作")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(inflater, position, entry);
                    } else if (which == 1) {
                        entries.remove(position);
                        adapter.notifyItemRemoved(position);
                        MedicalRecordStorage.saveHealthMetricEntries(requireContext(), entries);
                        rvList.scheduleLayoutAnimation();
                        renderMetricsPie();
                    }
                })
                .show();
    }

    private void showEditDialog(LayoutInflater inflater, int position, HealthMetricEntry entry) {
        View dialogView = inflater.inflate(R.layout.dialog_add_metric, null);
        Spinner spType = dialogView.findViewById(R.id.sp_metric_type);
        EditText etValue = dialogView.findViewById(R.id.et_metric_value);
        EditText etUnit = dialogView.findViewById(R.id.et_metric_unit);

        String[] types = new String[]{"心率", "血压", "体温", "体重", "血糖"};
        ArrayAdapter<String> adapterType = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(adapterType);

        // 预选类型
        int selected = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(entry.type)) { selected = i; break; }
        }
        spType.setSelection(selected);
        etValue.setText(String.valueOf(entry.value));
        etUnit.setText(entry.unit != null ? entry.unit : "");

        new AlertDialog.Builder(requireContext())
                .setTitle("编辑健康指标")
                .setView(dialogView)
                .setPositiveButton("保存", (d, which) -> {
                    String type = spType.getSelectedItem() != null ? spType.getSelectedItem().toString() : entry.type;
                    String valueStr = etValue.getText() != null ? etValue.getText().toString().trim() : String.valueOf(entry.value);
                    String unit = etUnit.getText() != null ? etUnit.getText().toString().trim() : (entry.unit != null ? entry.unit : "");
                    float value = entry.value;
                    try { value = Float.parseFloat(valueStr); } catch (Exception ignore) {}

                    entry.type = type;
                    entry.value = value;
                    entry.unit = unit;
                    adapter.notifyItemChanged(position);
                    MedicalRecordStorage.saveHealthMetricEntries(requireContext(), entries);
                    rvList.scheduleLayoutAnimation();
                    renderMetricsPie();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void renderMetricsPie() {
        if (pieChart == null) return;
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelColor(android.graphics.Color.parseColor("#111827"));
        pieChart.setCenterText("类型分布");
        pieChart.setCenterTextColor(android.graphics.Color.parseColor("#374151"));
        pieChart.setNoDataText("暂无健康指标数据");

        java.util.Map<String, Integer> countByType = new java.util.HashMap<>();
        for (HealthMetricEntry e : entries) {
            String t = e.type != null ? e.type : "其他";
            Integer c = countByType.get(t);
            countByType.put(t, (c == null ? 0 : c) + 1);
        }

        java.util.List<PieEntry> pieEntries = new java.util.ArrayList<>();
        java.util.List<Integer> entryColors = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> kv : countByType.entrySet()) {
            String type = kv.getKey();
            pieEntries.add(new PieEntry(kv.getValue(), type));
            entryColors.add(colorForType(type));
        }

        PieDataSet set = new PieDataSet(pieEntries, "");
        set.setColors(entryColors);
        set.setSliceSpace(2f);
        set.setSelectionShift(6f);

        PieData data = new PieData(set);
        data.setValueTextColor(android.graphics.Color.parseColor("#111827"));
        data.setValueTextSize(12f);
        data.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(data);

        // 图例配置：右上角垂直排列，置于图外
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextColor(android.graphics.Color.parseColor("#374151"));
        legend.setXEntrySpace(8f);
        legend.setYEntrySpace(4f);
        legend.setYOffset(0f);

        if (tvPieTitle != null) {
            tvPieTitle.setText("指标类型分布（总计 " + entries.size() + " 条）");
        }

        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private int colorForType(String type) {
        if (type == null) return android.graphics.Color.parseColor("#9CA3AF");
        switch (type) {
            case "心率":
                return android.graphics.Color.parseColor("#3B82F6"); // Blue
            case "血压":
                return android.graphics.Color.parseColor("#EF4444"); // Red
            case "体温":
                return android.graphics.Color.parseColor("#F59E0B"); // Amber
            case "体重":
                return android.graphics.Color.parseColor("#8B5CF6"); // Purple
            case "血糖":
                return android.graphics.Color.parseColor("#10B981"); // Green
            default:
                // 为未知类型基于哈希稳定取色
                int hash = Math.abs(type.hashCode());
                int[] palette = new int[] {
                        android.graphics.Color.parseColor("#06B6D4"), // Cyan
                        android.graphics.Color.parseColor("#F97316"), // Orange
                        android.graphics.Color.parseColor("#22C55E"), // Emerald
                        android.graphics.Color.parseColor("#A855F7"), // Violet
                        android.graphics.Color.parseColor("#EAB308")  // Yellow
                };
                return palette[hash % palette.length];
        }
    }
}
