package com.example.xinqiao.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.DefaultAvatarAdapter;

public class AvatarSelectorDialog extends Dialog {
    private OnAvatarSelectedListener listener;
    private int[] defaultAvatars;

    public interface OnAvatarSelectedListener {
        void onDefaultAvatarSelected(int avatarResId);
        void onGallerySelected();
    }

    public AvatarSelectorDialog(Context context, int[] defaultAvatars, OnAvatarSelectedListener listener) {
        super(context);
        this.defaultAvatars = defaultAvatars;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_avatar_selector, null);
        setContentView(view);

        GridView gridView = view.findViewById(R.id.gv_default_avatars);
        Button btnGallery = view.findViewById(R.id.btn_gallery);

        DefaultAvatarAdapter adapter = new DefaultAvatarAdapter(getContext(), defaultAvatars);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            if (listener != null) {
                listener.onDefaultAvatarSelected(defaultAvatars[position]);
            }
            dismiss();
        });

        btnGallery.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGallerySelected();
            }
            dismiss();
        });
    }
}