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
        View.OnClickListener go = v -> {
            android.content.Intent i = new android.content.Intent(getContext(), com.example.xinqiao.activity.CounselorInfoEditActivity.class);
            startActivity(i);
        };
        if (editBtn != null) editBtn.setOnClickListener(go);
        if (card != null) card.setOnClickListener(go);
    }
}