package com.example.xinqiao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.HealthMetricEntry;

import java.util.ArrayList;
import java.util.List;

public class HealthMetricsAdapter extends RecyclerView.Adapter<HealthMetricsAdapter.ViewHolder> {
    private List<HealthMetricEntry> data = new ArrayList<>();
    private OnItemLongClickListener longClickListener;

    public HealthMetricsAdapter(List<HealthMetricEntry> data) {
        if (data != null) this.data = data;
    }

    public void setData(List<HealthMetricEntry> list) {
        this.data = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_health_metric, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HealthMetricEntry entry = data.get(position);
        holder.tvDate.setText(entry.date != null ? entry.date : "");
        holder.tvType.setText("类型：" + (entry.type != null ? entry.type : ""));
        String unit = entry.unit != null ? entry.unit : "";
        holder.tvValue.setText("数值：" + entry.value + (unit.isEmpty() ? "" : (" " + unit)));

        Evaluation eval = evaluate(entry);
        holder.tvEval.setText("评判：" + eval.text);
        holder.tvEval.setTextColor(eval.color);

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(position, entry);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvType, tvValue, tvEval;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_metric_date);
            tvType = itemView.findViewById(R.id.tv_metric_type);
            tvValue = itemView.findViewById(R.id.tv_metric_value);
            tvEval = itemView.findViewById(R.id.tv_metric_eval);
        }
    }

    private Evaluation evaluate(HealthMetricEntry e) {
        String type = e.type != null ? e.type : "";
        float v = e.value;
        // 默认
        Evaluation res = new Evaluation("无标准", Color.parseColor("#6B7280"));
        switch (type) {
            case "心率": // bpm
                if (v < 50) res = new Evaluation("偏低", Color.parseColor("#6366F1"));
                else if (v < 60) res = new Evaluation("略低", Color.parseColor("#3B82F6"));
                else if (v <= 100) res = new Evaluation("正常", Color.parseColor("#10B981"));
                else if (v <= 120) res = new Evaluation("偏高", Color.parseColor("#F59E0B"));
                else res = new Evaluation("过高", Color.parseColor("#EF4444"));
                break;
            case "血压": // 以收缩压估算 mmHg
                if (v < 90) res = new Evaluation("偏低", Color.parseColor("#3B82F6"));
                else if (v <= 120) res = new Evaluation("正常", Color.parseColor("#10B981"));
                else if (v <= 139) res = new Evaluation("偏高", Color.parseColor("#F59E0B"));
                else res = new Evaluation("高血压", Color.parseColor("#EF4444"));
                break;
            case "体温": // ℃
                if (v < 35.0f) res = new Evaluation("过低", Color.parseColor("#3B82F6"));
                else if (v < 36.1f) res = new Evaluation("略低", Color.parseColor("#6366F1"));
                else if (v <= 37.2f) res = new Evaluation("正常", Color.parseColor("#10B981"));
                else if (v <= 37.9f) res = new Evaluation("低热", Color.parseColor("#F59E0B"));
                else if (v <= 38.5f) res = new Evaluation("中等发热", Color.parseColor("#F97316"));
                else res = new Evaluation("高热", Color.parseColor("#EF4444"));
                break;
            case "体重": // 无身高无法评估, 提示BMI
                res = new Evaluation("需身高计算BMI", Color.parseColor("#6B7280"));
                break;
            case "血糖": // mmol/L，默认为空腹参考
                if (v < 3.9f) res = new Evaluation("偏低", Color.parseColor("#3B82F6"));
                else if (v <= 6.1f) res = new Evaluation("正常(空腹)", Color.parseColor("#10B981"));
                else if (v <= 7.0f) res = new Evaluation("临界偏高", Color.parseColor("#F59E0B"));
                else res = new Evaluation("偏高", Color.parseColor("#EF4444"));
                break;
        }
        if ("无标准".equals(res.text)) {
            // 对用户自定义或未知类型，依据历史统计作简单评判
            java.util.List<HealthMetricEntry> sameType = new java.util.ArrayList<>();
            for (HealthMetricEntry item : data) {
                if (item.type != null && item.type.equals(type)) {
                    sameType.add(item);
                }
            }
            if (sameType.size() >= 3) {
                float mean = 0f;
                for (HealthMetricEntry item : sameType) mean += item.value;
                mean /= sameType.size();
                float var = 0f;
                for (HealthMetricEntry item : sameType) {
                    float d = item.value - mean;
                    var += d * d;
                }
                var /= sameType.size();
                float sd = (float)Math.sqrt(var);
                if (sd < 1e-6) {
                    res = new Evaluation("正常", Color.parseColor("#10B981"));
                } else if (v > mean + 1.5f * sd) {
                    res = new Evaluation("偏高(历史分析)", Color.parseColor("#F59E0B"));
                } else if (v < mean - 1.5f * sd) {
                    res = new Evaluation("偏低(历史分析)", Color.parseColor("#3B82F6"));
                } else {
                    res = new Evaluation("正常(历史分析)", Color.parseColor("#10B981"));
                }
            } else {
                res = new Evaluation("样本不足", Color.parseColor("#6B7280"));
            }
        }
        return res;
    }

    static class Evaluation {
        final String text;
        final int color;
        Evaluation(String t, int c) { this.text = t; this.color = c; }
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, HealthMetricEntry entry);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }
}
