package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.example.xinqiao.R;

public class CounselorAppointmentsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_counselor_appointments, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("counselor_profile", android.content.Context.MODE_PRIVATE);
        android.widget.TextView tvName = view.findViewById(R.id.tv_counselor_name);
        android.widget.TextView tvTitle = view.findViewById(R.id.tv_counselor_title);
        android.widget.ImageView ivAvatar = view.findViewById(R.id.iv_counselor_avatar);

        String encName = sp.getString("display_name", null);
        String name = com.example.xinqiao.util.crypto.CryptoUtil.decrypt(encName);
        if (name == null || name.isEmpty()) {
            name = "心理师";
        }
        String encCert = sp.getString("cert", null);
        String cert = com.example.xinqiao.util.crypto.CryptoUtil.decrypt(encCert);
        if (cert == null || cert.isEmpty()) {
            cert = "国家二级心理咨询师";
        }
        tvName.setText(name);
        tvTitle.setText(cert);

        String avatarUri = sp.getString("avatar_uri", null);
        if (avatarUri != null) {
            try {
                android.net.Uri uri = android.net.Uri.parse(avatarUri);
                ivAvatar.setImageURI(uri);
            } catch (Exception ignored) {}
        }
    }
}