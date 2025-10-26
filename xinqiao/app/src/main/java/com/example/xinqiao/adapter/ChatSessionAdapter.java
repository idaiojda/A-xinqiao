package com.example.xinqiao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.ChatSession;

import java.util.List;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.ViewHolder> {
    private List<ChatSession> mSessionList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(ChatSession session);
    }

    public ChatSessionAdapter(List<ChatSession> sessionList) {
        mSessionList = sessionList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSession session = mSessionList.get(position);
        holder.tvChatTitle.setText(session.getTitle());
        holder.tvChatTime.setText(session.getFormattedTime());

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(session);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mSessionList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatTitle;
        TextView tvChatTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatTitle = itemView.findViewById(R.id.tv_chat_title);
            tvChatTime = itemView.findViewById(R.id.tv_chat_time);
        }
    }
}