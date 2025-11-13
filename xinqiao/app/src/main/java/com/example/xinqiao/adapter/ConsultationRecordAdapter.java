package com.example.xinqiao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.ConsultationItem;

import java.util.ArrayList;
import java.util.List;

public class ConsultationRecordAdapter extends RecyclerView.Adapter<ConsultationRecordAdapter.ViewHolder> {
    private List<ConsultationItem> data = new ArrayList<>();
    private OnItemLongClickListener longClickListener;
    private OnItemClickListener clickListener;

    public ConsultationRecordAdapter(List<ConsultationItem> data) {
        if (data != null) this.data = data;
    }

    public void setData(List<ConsultationItem> list) {
        this.data = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_consultation_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConsultationItem item = data.get(position);
        holder.tvTitle.setText(item.title != null ? item.title : "");
        holder.tvType.setText("类型：" + (item.type != null ? item.type : ""));
        holder.tvDate.setText(item.date != null ? item.date : "");
        holder.tvMsg.setText("消息数：" + item.messageCount);
        holder.tvStatus.setText(item.status != null ? item.status : "");
        holder.tvSummary.setText(item.summary != null ? item.summary : "");

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(position, item);
                return true;
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvType, tvDate, tvMsg, tvStatus, tvSummary;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvType = itemView.findViewById(R.id.tv_type);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvMsg = itemView.findViewById(R.id.tv_message_count);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvSummary = itemView.findViewById(R.id.tv_summary);
        }
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, ConsultationItem item);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, ConsultationItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
}
