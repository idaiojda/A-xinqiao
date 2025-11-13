package com.example.xinqiao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.room.entities.AuthorizationEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AuthorizationAdapter extends RecyclerView.Adapter<AuthorizationAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(AuthorizationEntity entity);
    }

    private final List<AuthorizationEntity> items = new ArrayList<>();
    private OnItemClickListener listener;

    public void setItems(List<AuthorizationEntity> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public List<AuthorizationEntity> getItems() {
        return new ArrayList<>(items);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_authorization, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AuthorizationEntity e = items.get(position);
        holder.tvCounselor.setText(e.counselorName != null ? e.counselorName : (e.counselorId != null ? e.counselorId : "未知咨询师"));
        holder.tvScopes.setText("权限：" + (e.scopes != null ? e.scopes : "-"));
        holder.tvDuration.setText("有效期：" + e.durationDays + "天  (" + formatDate(e.startTimestamp) + " ~ " + formatDate(e.endTimestamp) + ")");
        holder.tvStatus.setText("状态：" + (e.status != null ? e.status : "-"));
        holder.tvStatus.setTextColor("已生效".equals(e.status) ? 0xFF30B4FF : 0xFF999999);
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(e); });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCounselor;
        TextView tvScopes;
        TextView tvDuration;
        TextView tvStatus;
        VH(@NonNull View itemView) {
            super(itemView);
            tvCounselor = itemView.findViewById(R.id.tv_counselor_name);
            tvScopes = itemView.findViewById(R.id.tv_scopes);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }

    private String formatDate(long ts) {
        if (ts <= 0) return "-";
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(ts));
    }
}

