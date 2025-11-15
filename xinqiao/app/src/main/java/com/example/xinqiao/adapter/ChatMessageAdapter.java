package com.example.xinqiao.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import android.widget.PopupWindow;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.ChatHistory;
import com.example.xinqiao.mysql.DBUtils;
import com.example.xinqiao.util.ui.ImageLoader;
import com.example.xinqiao.util.AnalysisUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_DATE = 1;

    private final List<Object> items = new ArrayList<>();
    private final List<ChatHistory> messages = new ArrayList<>();
    private Context appContext;
    private String aiAvatarUrl;
    private String userDisplayName;
    private String userAvatarPath;
    private boolean userInfoRequested = false;
    private boolean userNicknameRequested = false;

    public interface OnMessageActionListener {
        void onDelete(ChatHistory message);
    }

    private OnMessageActionListener actionListener;
    public void setOnMessageActionListener(OnMessageActionListener l) { this.actionListener = l; }

    public ChatMessageAdapter(List<ChatHistory> init) {
        setData(init);
    }

    public void setData(List<ChatHistory> list) {
        messages.clear();
        if (list != null) messages.addAll(list);
        rebuildItems();
        notifyDataSetChanged();
    }

    public void removeById(int id) {
        // 从原始消息中移除并重建
        for (int i = 0; i < messages.size(); i++) {
            ChatHistory ch = messages.get(i);
            if (ch != null && ch.getId() == id) {
                messages.remove(i);
                break;
            }
        }
        rebuildItems();
        notifyDataSetChanged();
    }

    private void rebuildItems() {
        items.clear();
        String lastDay = null;
        for (ChatHistory m : messages) {
            if (m == null) continue;
            String dayKey = formatDayKey(m.getTimestamp());
            if (!dayKey.equals(lastDay)) {
                items.add(new DateItem(dayKey));
                lastDay = dayKey;
            }
            items.add(m);
        }
    }

    private String formatDayKey(long ts) {
        try {
            // 今天/昨天显示中文标识，其余显示 yyyy-MM-dd
            long now = System.currentTimeMillis();
            long oneDay = 24L * 60 * 60 * 1000;
            long startToday = truncateToDay(now);
            long startTs = truncateToDay(ts);
            if (startTs == startToday) return "今天";
            if (startTs == startToday - oneDay) return "昨天";
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(ts));
        } catch (Exception e) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(ts));
        }
    }

    private long truncateToDay(long ts) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(ts);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
    public int getItemViewType(int position) {
        Object o = items.get(position);
        return (o instanceof DateItem) ? TYPE_DATE : TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        // 初始化上下文与基础配置
        if (appContext == null) {
            appContext = parent.getContext().getApplicationContext();
            try {
                aiAvatarUrl = appContext.getString(R.string.ai_avatar_url);
            } catch (Exception ignored) {}
            userDisplayName = AnalysisUtils.readLoginUserName(appContext);
            // 异步拉取用户昵称
            ensureUserNicknameLoaded(appContext);
            if (userDisplayName == null || userDisplayName.trim().isEmpty()) userDisplayName = "我";
        }
        if (viewType == TYPE_DATE) {
            View v = inflater.inflate(R.layout.msg_date_separator, parent, false);
            return new DateVH(v);
        } else {
            View v = inflater.inflate(R.layout.msg_item, parent, false);
            return new MessageVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int vt = getItemViewType(position);
        if (vt == TYPE_DATE) {
            DateItem di = (DateItem) items.get(position);
            ((DateVH) holder).label.setText(di.label);
            return;
        }

        ChatHistory item = (ChatHistory) items.get(position);
        MessageVH h = (MessageVH) holder;
        boolean fromUser = item != null && item.getType() == 1;

        h.leftLayout.setVisibility(fromUser ? View.GONE : View.VISIBLE);
        h.rightLayout.setVisibility(fromUser ? View.VISIBLE : View.GONE);

        // 文本
        if (fromUser) {
            h.rightMsg.setText(item != null ? item.getContent() : "");
        } else {
            h.leftMsg.setText(item != null ? item.getContent() : "");
        }

        // 名称（头像下方）
        if (h.leftName != null) {
            h.leftName.setText(h.itemView.getContext().getString(R.string.ai_display_name));
        }
        if (h.rightName != null) {
            h.rightName.setText(userDisplayName != null ? userDisplayName : h.itemView.getContext().getString(R.string.user_display_name_default));
        }

        // 头像
        if (h.leftAvatar != null) {
            if (aiAvatarUrl != null && aiAvatarUrl.length() > 0) {
                ImageLoader.loadCircleImageFromUrl(h.itemView.getContext(), aiAvatarUrl, h.leftAvatar, R.drawable.ai_avatar_warm);
            } else {
                ImageLoader.loadCircleImage(h.itemView.getContext(), R.drawable.ai_avatar_warm, h.leftAvatar);
            }
        }
        if (h.rightAvatar != null) {
            ensureUserAvatarPathLoaded(h.itemView.getContext());
            if (userAvatarPath != null && userAvatarPath.length() > 0) {
                if (userAvatarPath.startsWith("data:image")) {
                    ImageLoader.loadCircleImageFromBase64(h.itemView.getContext(), userAvatarPath, h.rightAvatar, R.drawable.default_avatar);
                } else {
                    ImageLoader.loadCircleImageFromPath(h.itemView.getContext(), userAvatarPath, h.rightAvatar, R.drawable.default_avatar);
                }
            } else {
                ImageLoader.loadCircleImage(h.itemView.getContext(), R.drawable.default_avatar, h.rightAvatar);
            }
        }

        // 时间元信息
        String timeStr = "";
        if (item != null) {
            try {
                Date d = new Date(item.getTimestamp());
                timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(d);
            } catch (Exception ignored) {}
        }
        if (h.leftMeta != null) {
            h.leftMeta.setText("心灵守护者 · " + (timeStr.isEmpty() ? "--:--" : timeStr));
        }
        if (h.rightMeta != null) {
            h.rightMeta.setText((userDisplayName != null ? userDisplayName : h.itemView.getContext().getString(R.string.user_display_name_default)) + " · " + (timeStr.isEmpty() ? "--:--" : timeStr));
        }

        // 长按操作：复制 / 分享 / 删除
        View anchor = fromUser ? h.rightLayout : h.leftLayout;
        View msgView = fromUser ? h.rightMsg : h.leftMsg;
        if (anchor != null && msgView != null) {
            anchor.setOnLongClickListener(v -> {
                showActionsPopup(v.getContext(), anchor, item, fromUser);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // --- ViewHolders ---
    static class MessageVH extends RecyclerView.ViewHolder {
        LinearLayout leftLayout, rightLayout;
        TextView leftMsg, rightMsg;
        ImageView leftAvatar, rightAvatar;
        TextView leftMeta, rightMeta;
        TextView leftName, rightName;

        MessageVH(@NonNull View itemView) {
            super(itemView);
            leftLayout = itemView.findViewById(R.id.left_layout);
            rightLayout = itemView.findViewById(R.id.right_layout);
            leftMsg = itemView.findViewById(R.id.left_msg);
            rightMsg = itemView.findViewById(R.id.right_msg);
            leftAvatar = itemView.findViewById(R.id.left_avatar);
            rightAvatar = itemView.findViewById(R.id.right_avatar);
            leftMeta = itemView.findViewById(R.id.left_meta);
            rightMeta = itemView.findViewById(R.id.right_meta);
            leftName = itemView.findViewById(R.id.left_name);
            rightName = itemView.findViewById(R.id.right_name);
        }
    }

    static class DateVH extends RecyclerView.ViewHolder {
        TextView label;
        DateVH(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.tv_date);
        }
    }

    static class DateItem {
        final String label;
        DateItem(String label) { this.label = label; }
    }

    // --- PopupWindow ---
    private void showActionsPopup(Context ctx, View anchor, ChatHistory item, boolean fromUser) {
        View content = LayoutInflater.from(ctx).inflate(R.layout.msg_actions_popup, null, false);
        final PopupWindow pw = new PopupWindow(content,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        pw.setBackgroundDrawable(new ColorDrawable(0x00000000));
        pw.setOutsideTouchable(true);
        pw.setAnimationStyle(R.style.MsgActionsPopupAnimation);
        if (android.os.Build.VERSION.SDK_INT >= 21) pw.setElevation(8f);

        TextView tvCopy = content.findViewById(R.id.action_copy);
        TextView tvShare = content.findViewById(R.id.action_share);
        TextView tvDelete = content.findViewById(R.id.action_delete);

        tvCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("message", item.getContent()));
                Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show();
            }
            pw.dismiss();
        });

        tvShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, item.getContent());
            try {
                ctx.startActivity(Intent.createChooser(intent, "分享消息"));
            } catch (Exception ignored) {}
            pw.dismiss();
        });

        tvDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(item);
            } else {
                removeById(item.getId());
            }
            pw.dismiss();
        });

        int xoff = fromUser ? -dp(ctx, 8) : dp(ctx, 0);
        int yoff = -dp(ctx, 4);
        // 尽量贴近气泡边缘显示
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            pw.showAsDropDown(anchor, xoff, yoff, fromUser ? Gravity.END : Gravity.START);
        } else {
            pw.showAsDropDown(anchor, xoff, yoff);
        }
    }

    private int dp(Context ctx, int v) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return (int) (v * density + 0.5f);
    }

    private void ensureUserAvatarPathLoaded(Context ctx) {
        if (userInfoRequested) return;
        userInfoRequested = true;
        String nameForQuery = AnalysisUtils.readLoginUserName(ctx);
        if (nameForQuery == null || nameForQuery.trim().isEmpty()) return;
        try {
            DBUtils.getInstance(ctx).getUserAvatarPath(nameForQuery, new DBUtils.AvatarPathCallback() {
                @Override
                public void onSuccess(String avatarBase64) {
                    userAvatarPath = avatarBase64; // 保存Base64数据URI或null
                    notifyDataSetChanged();
                }

                @Override
                public void onError(java.sql.SQLException e) {
                    // 保持默认头像
                }
            });
        } catch (java.sql.SQLException ignored) {}
    }

    private void ensureUserNicknameLoaded(Context ctx) {
        if (userNicknameRequested) return;
        userNicknameRequested = true;
        String nameForQuery = AnalysisUtils.readLoginUserName(ctx);
        if (nameForQuery == null || nameForQuery.trim().isEmpty()) return;
        try {
            DBUtils.getInstance(ctx).getUserNickname(nameForQuery, new DBUtils.UserNicknameCallback() {
                @Override
                public void onSuccess(String nickname) {
                    if (nickname != null && !nickname.trim().isEmpty()) {
                        userDisplayName = nickname.trim();
                        notifyDataSetChanged();
                    }
                }

                @Override
                public void onError(java.sql.SQLException e) {
                    // 保持现有显示名
                }
            });
        } catch (java.sql.SQLException ignored) {}
    }
}
