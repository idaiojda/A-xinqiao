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

class CounselorApplicationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counselor_application)

        // 覆盖后端端口为8082，便于本机联调（本地Express服务）
        try { getSharedPreferences("network_config", MODE_PRIVATE).edit().putString("base_url_override", "http://10.0.2.2:8082").apply() } catch (_: Throwable) {}

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
                tvStatus.text = "提交中..."
                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    submit(realName, phone, intro, qualification, years, expertise)
                }
                tvStatus.text = if (ok) "已提交，等待审核" else "提交失败"
                if (ok) {
                    val sp = getSharedPreferences("loginInfo", MODE_PRIVATE)
                    sp.edit().putString("counselor_application_status", "submitted").apply()
                    Toast.makeText(this@CounselorApplicationActivity, "提交成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CounselorApplicationActivity, "提交失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun submit(realName: String, phone: String, intro: String, qualification: String, years: Int, expertise: List<String>): Boolean {
        val base = NetworkConfig.getBaseUrl(this)
        val url = "$base/api/applications"
        val user = try { getSharedPreferences("loginInfo", MODE_PRIVATE).getString("loginUserName", null) } catch (_: Throwable) { null }
        val json = JSONObject()
            .put("user", user ?: JSONObject.NULL)
            .put("realName", realName)
            .put("phone", phone)
            .put("intro", intro)
            .put("qualificationType", qualification)
            .put("years", years)
            .put("expertise", org.json.JSONArray(expertise))
            .toString()
        val mt = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mt)
        val reqBuilder = Request.Builder().url(url).post(body)
        try {
            val token = getSharedPreferences("loginInfo", MODE_PRIVATE).getString("auth_token", null)
            if (!token.isNullOrEmpty()) reqBuilder.addHeader("Authorization", "Bearer $token")
        } catch (_: Throwable) {}
        return try {
            OkHttpClient().newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val s = resp.body?.string() ?: "{}"
                val obj = JSONObject(s)
                val code = obj.optInt("code", 0)
                code == 200
            }
        } catch (_: Exception) { false }
    }
}