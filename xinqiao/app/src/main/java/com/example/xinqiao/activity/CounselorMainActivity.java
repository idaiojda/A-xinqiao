package com.example.xinqiao.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.xinqiao.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CounselorMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_main);

        fragmentManager = getSupportFragmentManager();
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        setupBottomNavigation();
        
        // 默认显示工作台
        if (savedInstanceState == null) {
            switchFragment(new CounselorDashboardFragment());
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            
            if (item.getItemId() == R.id.nav_dashboard) {
                fragment = new CounselorDashboardFragment();
            } else if (item.getItemId() == R.id.nav_appointments) {
                fragment = new CounselorAppointmentsFragment();
            } else if (item.getItemId() == R.id.nav_content) {
                fragment = new CounselorContentFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                fragment = new CounselorProfileFragment();
            }

            if (fragment != null) {
                switchFragment(fragment);
                return true;
            }
            
            return false;
        });
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}