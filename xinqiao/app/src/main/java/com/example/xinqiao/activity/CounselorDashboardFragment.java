package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.example.xinqiao.R;

public class CounselorDashboardFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_counselor_dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 排班管理
        View btnSchedule = view.findViewById(R.id.btn_schedule_management);
        if (btnSchedule != null) {
            btnSchedule.setOnClickListener(v -> {
                try {
                    android.content.Intent i = new android.content.Intent(requireContext(), com.example.xinqiao.activity.CounselorScheduleActivity.class);
                    startActivity(i);
                } catch (Exception ignored) {}
            });
        }

        // 测评管理（基础占位：提示开发中）
        View btnAssessment = view.findViewById(R.id.btn_assessment_management);
        if (btnAssessment != null) {
            btnAssessment.setOnClickListener(v -> {
                android.widget.Toast.makeText(requireContext(), "测评管理开发中", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        // 内容管理：切换到底部导航“内容管理”页签
        View btnContent = view.findViewById(R.id.btn_content_management);
        if (btnContent != null) {
            btnContent.setOnClickListener(v -> {
                try {
                    androidx.fragment.app.FragmentActivity act = getActivity();
                    if (act instanceof com.example.xinqiao.activity.CounselorMainActivity) {
                        com.google.android.material.bottomnavigation.BottomNavigationView nav = act.findViewById(R.id.bottom_navigation);
                        if (nav != null) {
                            nav.setSelectedItemId(R.id.nav_content);
                        }
                    }
                } catch (Exception ignored) {}
            });
        }

        // 档案管理（基础占位：提示开发中）
        View btnRecords = view.findViewById(R.id.btn_records_management);
        if (btnRecords != null) {
            btnRecords.setOnClickListener(v -> {
                android.widget.Toast.makeText(requireContext(), "档案管理开发中", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }
}
