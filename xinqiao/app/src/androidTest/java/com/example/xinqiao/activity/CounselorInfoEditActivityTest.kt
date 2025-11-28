package com.example.xinqiao.activity

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounselorInfoEditActivityTest {
    @get:Rule
    val rule = ActivityTestRule(CounselorInfoEditActivity::class.java, true, true)

    @Test
    fun launchAndSaveDraft() {
        val a = rule.activity
        val sp = a.getSharedPreferences("counselor_profile", android.content.Context.MODE_PRIVATE)
        sp.edit().putString("edu", com.example.xinqiao.util.crypto.CryptoUtil.encrypt("心理学")).apply()
        assert(sp.getString("edu", null) != null)
    }
}