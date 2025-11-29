package com.example.xinqiao.activity

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.xinqiao.R
import com.example.xinqiao.network.NetworkConfig
import com.example.xinqiao.util.AnalysisUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.content.Intent

class CounselorApplicationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counselor_application)

        

        val etRealName = findViewById<EditText>(R.id.et_real_name)
        val etPhone = findViewById<EditText>(R.id.et_phone)
        val etIntro = findViewById<EditText>(R.id.et_intro)
        val spQualification = findViewById<Spinner>(R.id.sp_qualification)
        val npYears = findViewById<NumberPicker>(R.id.np_years)
        val chipGroup = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_expertise)
        val btnSubmit = findViewById<Button>(R.id.btn_submit)
        val tvStatus = findViewById<TextView>(R.id.tv_status)

        spQualification.adapter = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.qualification_types,
            android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        npYears.minValue = 0
        npYears.maxValue = 40
        npYears.value = 3

        resources.getStringArray(R.array.expertise_options).forEach { label ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = label
            chip.isCheckable = true
            chip.isClickable = true
            chipGroup.addView(chip)
        }

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val spLogin = getSharedPreferences("loginInfo", MODE_PRIVATE)
                var token = spLogin.getString("auth_token", null)
                if (token.isNullOrEmpty()) {
                    val spRemember = getSharedPreferences("login_info", MODE_PRIVATE)
                    val remembered = spRemember.getBoolean("remember_password", false)
                    val phone = spRemember.getString("username", null)
                    val pwd = spRemember.getString("password", null)
                    if (remembered && !phone.isNullOrBlank() && !pwd.isNullOrBlank()) {
                        try {
                            val lr = com.example.xinqiao.network.ApiJava.login(phone!!, pwd!!)
                            if (lr != null && lr.ok && lr.token != null) {
                                spLogin.edit().putString("auth_token", lr.token).apply()
                            } else {
                                val regOk = com.example.xinqiao.network.ApiJava.register(phone!!, pwd!!)
                                if (regOk) {
                                    val lr2 = com.example.xinqiao.network.ApiJava.login(phone!!, pwd!!)
                                    if (lr2 != null && lr2.ok && lr2.token != null) {
                                        spLogin.edit().putString("auth_token", lr2.token).apply()
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Throwable) {}
        }

        lifecycleScope.launch {
            try {
                val raw = com.example.xinqiao.network.Http.api().myApplicationsRaw()
                if (raw.isSuccessful) {
                    val s = raw.body()?.string()
                    if (s != null) {
                        var hasPending = false
                        try {
                            val jo = org.json.JSONObject(s)
                            val arr = jo.optJSONArray("data")
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    val it = arr.optJSONObject(i)
                                    if (it != null && it.optString("status", "").equals("pending", true)) { hasPending = true; break }
                                }
                            } else {
                                val data = jo.optJSONObject("data")
                                if (data != null && data.optString("status", "").equals("pending", true)) { hasPending = true }
                            }
                        } catch (_: Exception) {}
                        if (hasPending) {
                            tvStatus.text = "已有待审核申请"
                            btnSubmit.isEnabled = false
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        btnSubmit.setOnClickListener {
            val realName = etRealName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val intro = etIntro.text.toString().trim()
            if (realName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "请填写姓名和联系方式", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val qualification = spQualification.selectedItem?.toString() ?: ""
            val years = npYears.value
            val expertise = mutableListOf<String>()
            chipGroup.checkedChipIds.forEach { id ->
                val chip = chipGroup.findViewById<com.google.android.material.chip.Chip>(id)
                chip?.text?.toString()?.let { expertise.add(it) }
            }
            lifecycleScope.launch {
                btnSubmit.isEnabled = false
                tvStatus.text = "提交中..."
                suspend fun ensureAuth(): Boolean {
                    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val spLogin = getSharedPreferences("loginInfo", MODE_PRIVATE)
                            val hasToken = !spLogin.getString("auth_token", null).isNullOrEmpty()
                            if (hasToken) {
                                try { if (com.example.xinqiao.network.Http.api().me().ok) return@withContext true } catch (_: Exception) {}
                            }
                            val spRemember = getSharedPreferences("login_info", MODE_PRIVATE)
                            val remembered = spRemember.getBoolean("remember_password", false)
                            val u = spRemember.getString("username", null)
                            val p = spRemember.getString("password", null)
                            var ok = false
                            if (remembered && !u.isNullOrBlank() && !p.isNullOrBlank()) {
                                try {
                                    val lr = com.example.xinqiao.network.ApiJava.login(u!!, p!!)
                                    if (lr != null && lr.ok && lr.token != null) {
                                        spLogin.edit().putString("auth_token", lr.token).apply(); ok = true
                                    } else {
                                        val regOk = com.example.xinqiao.network.ApiJava.register(u!!, p!!)
                                        if (regOk) {
                                            val lr2 = com.example.xinqiao.network.ApiJava.login(u!!, p!!)
                                            if (lr2 != null && lr2.ok && lr2.token != null) { spLogin.edit().putString("auth_token", lr2.token).apply(); ok = true }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            if (!ok) {
                                val fallbackUser = if (phone.isNotBlank()) phone else AnalysisUtils.readLoginUserName(this@CounselorApplicationActivity)
                                val fallbackPwd = if (!p.isNullOrBlank()) p!! else (if (remembered) p ?: phone else phone)
                                if (!fallbackUser.isNullOrBlank() && !fallbackPwd.isNullOrBlank()) {
                                    try {
                                        val lr3 = com.example.xinqiao.network.ApiJava.login(fallbackUser!!, fallbackPwd!!)
                                        if (lr3 != null && lr3.ok && lr3.token != null) { spLogin.edit().putString("auth_token", lr3.token).apply(); ok = true } else {
                                            val regOk2 = com.example.xinqiao.network.ApiJava.register(fallbackUser!!, fallbackPwd!!)
                                            if (regOk2) {
                                                val lr4 = com.example.xinqiao.network.ApiJava.login(fallbackUser!!, fallbackPwd!!)
                                                if (lr4 != null && lr4.ok && lr4.token != null) { spLogin.edit().putString("auth_token", lr4.token).apply(); ok = true }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                    if (ok) {
                                        try { spRemember.edit().putBoolean("remember_password", true).putString("username", fallbackUser).putString("password", fallbackPwd).apply() } catch (_: Exception) {}
                                    }
                                }
                            }
                            if (ok) {
                                try { return@withContext com.example.xinqiao.network.Http.api().me().ok } catch (_: Exception) { return@withContext true }
                            }
                            false
                        } catch (_: Exception) { false }
                    }
                }
                val authOk = ensureAuth()
                if (!authOk) {
                    tvStatus.text = "未登录"
                    Toast.makeText(this@CounselorApplicationActivity, "请先登录后再提交", Toast.LENGTH_SHORT).show()
                    btnSubmit.isEnabled = true
                    return@launch
                }
                var networkError = false
                val isLoggedIn = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try { com.example.xinqiao.network.Http.api().me().ok } catch (_: Exception) { networkError = true; false }
                }
                if (!isLoggedIn) {
                    if (networkError) {
                        tvStatus.text = "网络异常"
                        Toast.makeText(this@CounselorApplicationActivity, "网络异常，请稍后重试", Toast.LENGTH_SHORT).show()
                        btnSubmit.isEnabled = true
                        return@launch
                    }
                    val recovered = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val spLogin = getSharedPreferences("loginInfo", MODE_PRIVATE)
                            val isLoginFlag = spLogin.getBoolean("isLogin", false)
                            val spRemember = getSharedPreferences("login_info", MODE_PRIVATE)
                            val remembered = spRemember.getBoolean("remember_password", false)
                            val phone2 = spRemember.getString("username", null)
                            val pwd2 = spRemember.getString("password", null)
                            var ok = false
                            if (remembered && !phone2.isNullOrBlank() && !pwd2.isNullOrBlank()) {
                                try {
                                    val lr = com.example.xinqiao.network.ApiJava.login(phone2!!, pwd2!!)
                                    if (lr != null && lr.ok && lr.token != null) {
                                        spLogin.edit().putString("auth_token", lr.token).apply()
                                        ok = true
                                    } else {
                                        val regOk = com.example.xinqiao.network.ApiJava.register(phone2!!, pwd2!!)
                                        if (regOk) {
                                            val lr2 = com.example.xinqiao.network.ApiJava.login(phone2!!, pwd2!!)
                                            if (lr2 != null && lr2.ok && lr2.token != null) {
                                                spLogin.edit().putString("auth_token", lr2.token).apply()
                                                ok = true
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            if (!ok && isLoginFlag) {
                                ok = try { com.example.xinqiao.network.Http.api().me().ok } catch (_: Exception) { false }
                            }
                            ok
                        } catch (_: Exception) { false }
                    }
                    val stillOk = if (recovered) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try { com.example.xinqiao.network.Http.api().me().ok } catch (_: Exception) { false }
                    } else false
                    if (!stillOk) {
                        tvStatus.text = "提交中..."
                    }
                }
                val resp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    submit(realName, phone, intro, qualification, years, expertise)
                }
                val ok = resp?.ok == true || (resp?.code ?: 0) == 200
                val msg = resp?.message
                tvStatus.text = if (ok) "已提交，等待审核" else "提交失败"
                if (ok) {
                    val sp = getSharedPreferences("loginInfo", MODE_PRIVATE)
                    sp.edit().putString("counselor_application_status", "submitted").apply()
                    Toast.makeText(this@CounselorApplicationActivity, "提交成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val tip = if (msg.isNullOrBlank()) "提交失败，请稍后重试" else msg
                    Toast.makeText(this@CounselorApplicationActivity, tip, Toast.LENGTH_SHORT).show()
                    if ((resp?.code ?: 0) == 401 || (resp?.code ?: 0) == 403 || (msg != null && (msg.contains("未登录") || msg.contains("Forbidden", true)))) {
                        tvStatus.text = "未登录"
                        Toast.makeText(this@CounselorApplicationActivity, "登录状态已失效，请重新登录", Toast.LENGTH_SHORT).show()
                        try {
                            val intent = Intent(this@CounselorApplicationActivity, com.example.xinqiao.activity.LoginActivity::class.java)
                            startActivity(intent)
                        } catch (_: Exception) {}
                        btnSubmit.isEnabled = true
                    }
                }
                btnSubmit.isEnabled = true
            }
        }
    }

    private suspend fun submit(realName: String, phone: String, intro: String, qualification: String, years: Int, expertise: List<String>): com.example.xinqiao.network.ApiResp<Any>? {
        val payload = com.example.xinqiao.network.ApplicationPayload(
            realName = realName,
            idCard = null,
            phone = phone,
            qualificationType = qualification,
            certificateNo = null,
            years = years,
            expertise = expertise,
            materials = emptyList(),
            intro = intro
        )
        return try {
            val res = com.example.xinqiao.network.Http.api().submitApplication(payload)
            if (res.isSuccessful) {
                res.body()
            } else {
                // 尝试解析错误信息
                val errStr = res.errorBody()?.string()
                var errMsg = res.message()
                if (!errStr.isNullOrBlank()) {
                    try {
                        val jo = org.json.JSONObject(errStr)
                        errMsg = jo.optString("message", errMsg)
                    } catch (_: Exception) {
                        errMsg = errStr
                    }
                }
                com.example.xinqiao.network.ApiResp(false, res.code(), errMsg, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.xinqiao.network.ApiResp(false, 0, "网络异常: ${e.message}", null)
        }
    }
}
