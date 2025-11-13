package com.example.xinqiao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.ChatHistory;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.VH> {
    private List<ChatHistory> data = new ArrayList<>();

    public ChatMessageAdapter(List<ChatHistory> init) {
        if (init != null) data = init;
    }

    public void setData(List<ChatHistory> list) {
        data = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_item_enhanced, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatHistory item = data.get(position);
        boolean fromUser = item != null && item.getType() == 1;

        h.leftLayout.setVisibility(fromUser ? View.GONE : View.VISIBLE);
        h.rightLayout.setVisibility(fromUser ? View.VISIBLE : View.GONE);

        if (fromUser) {
            h.rightMsg.setText(item != null ? item.getContent() : "");
            // 头像与元信息
            h.rightAvatar.setImageResource(R.drawable.default_avatar);
            h.rightMeta.setText(formatMeta("我", item));
        } else {
            h.leftMsg.setText(item != null ? item.getContent() : "");
            h.leftAvatar.setImageResource(R.drawable.main_ai_icon);
            h.leftMeta.setText(formatMeta("心灵守护者", item));
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    private String formatMeta(String name, ChatHistory item) {
        long ts = item != null ? item.getTimestamp() : 0L;
        String timeStr;
        if (ts > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeStr = sdf.format(new Date(ts));
        } else {
            timeStr = "";
        }
        return timeStr.isEmpty() ? name : (name + " · " + timeStr);
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout leftLayout, rightLayout;
        TextView leftMsg, rightMsg, leftMeta, rightMeta;
        ImageView leftAvatar, rightAvatar;

        VH(@NonNull View itemView) {
            super(itemView);
            leftLayout = itemView.findViewById(R.id.left_layout);
            rightLayout = itemView.findViewById(R.id.right_layout);
            leftMsg = itemView.findViewById(R.id.left_msg);
            rightMsg = itemView.findViewById(R.id.right_msg);
            leftMeta = itemView.findViewById(R.id.left_meta);
            rightMeta = itemView.findViewById(R.id.right_meta);
            leftAvatar = itemView.findViewById(R.id.left_avatar);
            rightAvatar = itemView.findViewById(R.id.right_avatar);
        }
    }
}
