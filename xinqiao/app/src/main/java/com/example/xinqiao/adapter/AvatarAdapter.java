package com.example.xinqiao.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.xinqiao.R;

public class AvatarAdapter extends BaseAdapter {
    private Context context;
    private int[] avatarResIds;

    public AvatarAdapter(Context context, int[] avatarResIds) {
        this.context = context;
        this.avatarResIds = avatarResIds;
    }

    @Override
    public int getCount() {
        return avatarResIds.length;
    }

    @Override
    public Object getItem(int position) {
        return avatarResIds[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_avatar, parent, false);
        }

        ImageView imageView = convertView.findViewById(R.id.iv_avatar);
        
        // 使用Glide加载图片，避免OOM
        try {
            RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .override(200, 200) // 限制图片大小
                .centerCrop();
                
            Glide.with(context)
                .load(avatarResIds[position])
                .apply(options)
                .into(imageView);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return convertView;
    }
}