package com.example.xinqiao.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.example.xinqiao.R;

public class DefaultAvatarAdapter extends BaseAdapter {
    // 上下文对象
    private Context context;
    // 头像资源id数组
    private int[] avatarResIds;

    /**
     * 构造方法，初始化上下文和头像资源数组
     */
    public DefaultAvatarAdapter(Context context, int[] avatarResIds) {
        this.context = context;
        this.avatarResIds = avatarResIds;
    }

    /**
     * 返回头像数量
     */
    @Override
    public int getCount() {
        return avatarResIds.length;
    }

    /**
     * 获取指定位置的头像资源id
     */
    @Override
    public Object getItem(int position) {
        return avatarResIds[position];
    }

    /**
     * 获取item的id
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * 获取item视图，绑定头像图片
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_default_avatar, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.iv_avatar);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.imageView.setImageResource(avatarResIds[position]);
        return convertView;
    }

    /**
     * ViewHolder内部类，缓存item视图，提升性能
     */
    private static class ViewHolder {
        ImageView imageView;
    }
}