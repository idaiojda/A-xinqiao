package com.example.xinqiao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.view.AiView;
import com.example.xinqiao.utils.BitmapUtils;
import com.example.xinqiao.utils.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {
    // 聊天消息数据列表
    private List<AiView.Msg> mMsgList;
    private Bitmap userAvatarBitmap;

    /**
     * ViewHolder 内部类，缓存每个item的视图，提升性能
     * 包含左右两种布局，分别显示收到和发出的消息
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftlayout;   // 左侧消息布局（收到的消息）
        LinearLayout rightlayout;  // 右侧消息布局（发出的消息）
        TextView leftMsg;          // 左侧消息文本
        TextView rightMsg;         // 右侧消息文本

        public ViewHolder(View view) {
            super(view);
            leftlayout = (LinearLayout) view.findViewById(R.id.left_layout);
            rightlayout = (LinearLayout) view.findViewById(R.id.right_layout);
            leftMsg = (TextView) view.findViewById(R.id.left_msg);
            rightMsg = (TextView) view.findViewById(R.id.right_msg);
        }
    }

    /**
     * 构造方法，传入消息数据列表
     */
    public MsgAdapter(List<AiView.Msg> msgList, Bitmap userAvatarBitmap) {
        mMsgList = msgList;
        this.userAvatarBitmap = userAvatarBitmap;
    }
    // 兼容旧用法
    public MsgAdapter(List<AiView.Msg> msgList) {
        this(msgList, null);
    }

    public void setUserAvatarBitmap(Bitmap bitmap) {
        this.userAvatarBitmap = bitmap;
        notifyDataSetChanged();
    }

    /**
     * 创建ViewHolder，加载每个item的布局
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from((parent.getContext())).inflate(R.layout.msg_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 绑定数据到ViewHolder，根据消息类型切换左右布局
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiView.Msg msg = mMsgList.get(position);
        String content = msg.getContent();
        if (content == null) {
            content = "";
        }
        
        // 加载AI头像
        ImageView leftAvatar = holder.itemView.findViewById(R.id.left_avatar);
        if (leftAvatar != null) {
            int targetWidth = leftAvatar.getWidth() > 0 ? leftAvatar.getWidth() : 44;
            int targetHeight = leftAvatar.getHeight() > 0 ? leftAvatar.getHeight() : 44;
            try {
                Drawable drawable = BitmapUtils.decodeSampledDrawableFromResource(
                        leftAvatar.getContext(), 
                        R.drawable.ic_ai_avatar, 
                        targetWidth, 
                        targetHeight);
                leftAvatar.setImageDrawable(drawable);
            } catch (Exception e) {
                // 如果加载失败，使用Glide作为备选方案
                Glide.with(leftAvatar.getContext().getApplicationContext()) // 使用ApplicationContext避免生命周期问题
                    .load(R.drawable.ic_ai_avatar)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(leftAvatar);
            }
        }

        if(msg.getType() == AiView.Msg.TYPE_RECEIVED){
            // 如果是收到消息，则显示左边的消息布局，将右边的消息布局隐藏
            holder.leftlayout.setVisibility(View.VISIBLE);
            holder.rightlayout.setVisibility(View.GONE);
            holder.leftMsg.setText(content);
        }
        else if(msg.getType() == AiView.Msg.TYPE_SENT){
            // 如果是发出的消息，则显示右边的消息布局，将左边的消息布局隐藏
            holder.rightlayout.setVisibility(View.VISIBLE);
            holder.leftlayout.setVisibility(View.GONE);
            holder.rightMsg.setText(content);
            // 设置用户头像
            ImageView rightAvatar = (ImageView) holder.rightlayout.getChildAt(1);
            if (userAvatarBitmap != null) {
                try {
                    // 获取ImageView的尺寸作为目标尺寸
                    int targetWidth = rightAvatar.getWidth() > 0 ? rightAvatar.getWidth() : 100;
                    int targetHeight = rightAvatar.getHeight() > 0 ? rightAvatar.getHeight() : 100;
                    
                    // 直接使用Glide加载原始Bitmap并让Glide处理缩放和裁剪
                    // 避免手动创建缩小的Bitmap，防止Bitmap被回收导致错误
                    Glide.with(rightAvatar.getContext().getApplicationContext()) // 使用ApplicationContext避免生命周期问题
                        .load(userAvatarBitmap)
                        .circleCrop()
                        .into(rightAvatar);
                } catch (Exception e) {
                    // 如果处理失败，直接使用原始Bitmap
                    Glide.with(rightAvatar.getContext().getApplicationContext()) // 使用ApplicationContext避免生命周期问题
                        .load(userAvatarBitmap)
                        .circleCrop()
                        .into(rightAvatar);
                }
            } else {
                // 使用BitmapUtils加载并缩放图片，避免OOM
                try {
                    // 获取ImageView的尺寸作为目标尺寸
                    int targetWidth = rightAvatar.getWidth() > 0 ? rightAvatar.getWidth() : 100;
                    int targetHeight = rightAvatar.getHeight() > 0 ? rightAvatar.getHeight() : 100;
                    
                    // 使用BitmapUtils加载并缩放图片
                    Drawable drawable = BitmapUtils.decodeSampledDrawableFromResource(
                            rightAvatar.getContext(), 
                            R.drawable.ic_user_avatar, 
                            targetWidth, 
                            targetHeight);
                    
                    rightAvatar.setImageDrawable(drawable);
                } catch (Exception e) {
                    // 如果加载失败，使用Glide作为备选方案
                    Glide.with(rightAvatar.getContext())
                        .load(R.drawable.ic_user_avatar)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .circleCrop()
                        .into(rightAvatar);
                }
            }
        }
    }

    /**
     * 返回消息总数
     */
    @Override
    public int getItemCount() {
        return mMsgList.size();
    }
}

