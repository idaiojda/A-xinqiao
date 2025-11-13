package com.example.xinqiao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.EmotionEntry;

import java.util.ArrayList;
import java.util.List;

public class EmotionDiaryAdapter extends RecyclerView.Adapter<EmotionDiaryAdapter.ViewHolder> {
    private List<EmotionEntry> data = new ArrayList<>();
    private OnItemLongClickListener longClickListener;
    private OnItemClickListener clickListener;

    public EmotionDiaryAdapter(List<EmotionEntry> data) {
        if (data != null) this.data = data;
    }

    public void setData(List<EmotionEntry> list) {
        this.data = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emotion_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmotionEntry entry = data.get(position);
        holder.tvDate.setText(entry.date != null ? entry.date : "");
        holder.tvMood.setText("心情：" + entry.mood + "/10");
        holder.tvNote.setText(entry.note != null ? entry.note : "");

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(position, entry);
                return true;
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(position, entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvMood, tvNote;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvMood = itemView.findViewById(R.id.tv_mood);
            tvNote = itemView.findViewById(R.id.tv_note);
        }
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, EmotionEntry entry);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, EmotionEntry entry);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
}
