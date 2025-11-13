package com.example.xinqiao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.TestReportItem;

import java.util.ArrayList;
import java.util.List;

public class TestReportAdapter extends RecyclerView.Adapter<TestReportAdapter.ViewHolder> {
    private List<TestReportItem> data = new ArrayList<>();
    private OnItemLongClickListener longClickListener;
    private OnItemClickListener clickListener;

    public TestReportAdapter(List<TestReportItem> data) {
        if (data != null) this.data = data;
    }

    public void setData(List<TestReportItem> list) {
        this.data = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TestReportItem item = data.get(position);
        holder.tvType.setText(item.type != null ? item.type : "");
        holder.tvDate.setText(item.date != null ? item.date : "");
        holder.tvScore.setText("分数：" + item.score);
        holder.tvRisk.setText("风险：" + (item.riskLevel != null ? item.riskLevel : ""));
        holder.tvDetails.setText(item.details != null ? item.details : "");

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(position, item);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(position, item);
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
        TextView tvType, tvDate, tvScore, tvRisk, tvDetails;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tv_type);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvScore = itemView.findViewById(R.id.tv_score);
            tvRisk = itemView.findViewById(R.id.tv_risk);
            tvDetails = itemView.findViewById(R.id.tv_details);
        }
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, TestReportItem item);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, TestReportItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
}
