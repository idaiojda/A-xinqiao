package com.example.xinqiao.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.VideoBean;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlayHistoryAdapter extends BaseAdapter {
    // 上下文对象
    private Context context;
    // 播放历史数据列表
    private List<VideoBean> historyList;
    // 布局加载器
    private LayoutInflater inflater;
    // item点击事件监听器
    private OnItemClickListener listener;

    /**
     * item点击事件接口，包含点击和删除
     */
    public interface OnItemClickListener {
        void onItemClick(VideoBean videoBean, int position);
        void onDeleteClick(VideoBean videoBean, int position);
    }

    /**
     * 构造方法，初始化上下文和数据
     */
    public PlayHistoryAdapter(Context context, List<VideoBean> historyList) {
        this.context = context;
        this.historyList = historyList;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * 设置item点击事件监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 设置新的数据并刷新列表
     */
    public void setData(List<VideoBean> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    /**
     * 返回历史记录总数
     */
    @Override
    public int getCount() {
        return historyList != null ? historyList.size() : 0;
    }

    /**
     * 获取指定位置的数据对象
     */
    @Override
    public VideoBean getItem(int position) {
        return historyList.get(position);
    }

    /**
     * 获取item的id
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * 获取item视图，绑定数据和事件
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_play_history, parent, false);
            holder = new ViewHolder();
            holder.ivThumbnail = convertView.findViewById(R.id.iv_thumbnail);
            holder.tvTitle = convertView.findViewById(R.id.tv_title);
            holder.tvSubtitle = convertView.findViewById(R.id.tv_subtitle);
            holder.tvTime = convertView.findViewById(R.id.tv_time);
            holder.ivDelete = convertView.findViewById(R.id.iv_delete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        VideoBean videoBean = getItem(position);
        
        // 设置视频信息
        holder.tvTitle.setText(videoBean.title);
        holder.tvSubtitle.setText(videoBean.secondTitle);
        
        // 设置播放时间
        if (videoBean.playTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.tvTime.setText("播放时间: " + sdf.format(new Date(videoBean.playTime)));
        } else {
            holder.tvTime.setText("播放时间: 未知");
        }

        // 设置点击事件，回调item点击
        convertView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(videoBean, position);
            }
        });

        // 设置删除按钮点击事件，回调删除
        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(videoBean, position);
            }
        });

        return convertView;
    }

    /**
     * ViewHolder内部类，缓存item视图，提升性能
     */
    static class ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvTime;
        ImageView ivDelete;
    }
} 