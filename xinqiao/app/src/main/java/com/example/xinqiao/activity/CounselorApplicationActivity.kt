package com.example.xinqiao.activity

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.xinqiao.R
import com.example.xinqiao.network.NetworkConfig
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

        try {
            val spLogin = getSharedPreferences("loginInfo", MODE_PRIVATE)
            val tokenExists = !spLogin.getString("auth_token", null).isNullOrEmpty()
            if (!tokenExists) {
                val spRemember = getSharedPreferences("login_info", MODE_PRIVATE)
                val remembered = spRemember.getBoolean("remember_password", false)
                val phone = spRemember.getString("username", null)
                val pwd = spRemember.getString("password", null)
                var obtained = false
                if (remembered && !phone.isNullOrBlank() && !pwd.isNullOrBlank()) {
                    try {
                        val lr = com.example.xinqiao.network.ApiJava.login(phone!!, pwd!!)
                        if (lr != null && lr.ok && lr.token != null) {
                            spLogin.edit().putString("auth_token", lr.token).apply()
                            obtained = true
                        } else {
                            val regOk = com.example.xinqiao.network.ApiJava.register(phone!!, pwd!!)
                            if (regOk) {
                                val lr2 = com.example.xinqiao.network.ApiJava.login(phone!!, pwd!!)
                                if (lr2 != null && lr2.ok && lr2.token != null) {
                                    spLogin.edit().putString("auth_token", lr2.token).apply()
                                    obtained = true
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                if (!obtained) {
                    Toast.makeText(this, "请先登录后再申请", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    return
                }
            }
        } catch (_: Throwable) {}

        lifecycleScope.launch {
            try {
                val raw = com.example.xinqiao.network.Http.api().myApplicationsRaw()
                if (raw.isSuccessful) {
                    val s = raw.body()?.string()
                    if (s != null) {
                        val jo = org.json.JSONObject(s)
                        val arr = jo.optJSONArray("data")
                        if (arr != null) {
                            var hasPending = false
                            for (i in 0 until arr.length()) {
                                val it = arr.optJSONObject(i)
                                if (it != null && it.optString("status", "").equals("pending", true)) { hasPending = true; break }
                            }
                            if (hasPending) {
                                tvStatus.text = "已有待审核申请"
                                btnSubmit.isEnabled = false
                            }
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
                val isLoggedIn = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try { com.example.xinqiao.network.Http.api().me().ok } catch (_: Exception) { false }
                }
                if (!isLoggedIn) {
                    tvStatus.text = "未登录"
                    Toast.makeText(this@CounselorApplicationActivity, "登录状态已失效，请重新登录", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@CounselorApplicationActivity, LoginActivity::class.java))
                    finish()
                    return@launch
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
                    if ((resp?.code ?: 0) == 401 || (msg != null && msg.contains("未登录"))) {
                        startActivity(Intent(this@CounselorApplicationActivity, LoginActivity::class.java))
                        finish()
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
        return try { com.example.xinqiao.network.Http.api().submitApplication(payload) } catch (_: Exception) { null }
    }
}
