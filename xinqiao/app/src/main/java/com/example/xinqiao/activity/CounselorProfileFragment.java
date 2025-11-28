package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.example.xinqiao.R;

public class CounselorProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_counselor_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View editBtn = view.findViewById(R.id.btn_edit_profile);
        View card = view.findViewById(R.id.card_profile_top);
        View logout = view.findViewById(R.id.btn_logout);
        View.OnClickListener go = v -> {
            android.content.Intent i = new android.content.Intent(getContext(), com.example.xinqiao.activity.CounselorInfoEditActivity.class);
            startActivity(i);
        };
        if (editBtn != null) editBtn.setOnClickListener(go);
        if (card != null) card.setOnClickListener(go);
        if (logout != null) logout.setOnClickListener(v -> {
            try {
                android.content.SharedPreferences sp = requireContext().getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor ed = sp.edit();
                ed.putBoolean("isLogin", false);
                ed.putBoolean("isCounselor", false);
                ed.remove("auth_token");
                ed.remove("counselor_application_status");
                ed.apply();
            } catch (Exception ignored) {}
            try {
                android.content.Intent i = new android.content.Intent(getContext(), com.example.xinqiao.activity.LoginActivity.class);
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                if (getActivity() != null) getActivity().finishAffinity();
            } catch (Exception ignored2) {}
            android.widget.Toast.makeText(getContext(), "已退出登录", android.widget.Toast.LENGTH_SHORT).show();
        });
    }
}
