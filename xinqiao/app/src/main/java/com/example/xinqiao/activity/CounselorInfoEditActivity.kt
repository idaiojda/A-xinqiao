package com.example.xinqiao.activity

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.example.xinqiao.R
import com.example.xinqiao.util.crypto.CryptoUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.xinqiao.mysql.MySQLHelper
import java.sql.Connection
import java.sql.PreparedStatement
import org.json.JSONObject
import org.json.JSONArray
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class CounselorInfoEditActivity : AppCompatActivity() {
    private val sp by lazy { getSharedPreferences("counselor_profile", MODE_PRIVATE) }
    
    private lateinit var avatarView: ImageView
    private lateinit var etName: EditText
    private lateinit var etCity: EditText
    private lateinit var etBrief: EditText
    private lateinit var tvBriefCounter: TextView
    private lateinit var etEducation: AutoCompleteTextView
    private lateinit var etYears: AutoCompleteTextView
    private lateinit var etRichBio: EditText
    private lateinit var chipGroupExpertise: ChipGroup
    private lateinit var btnAddExpertiseTag: Button
    private lateinit var btnSubmit: Button
    
    private val expertiseTags = mutableListOf<String>()
    private val maxTags = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counselor_info_edit)
        
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        
        initViews()
        loadData()
        setupListeners()
        checkReviewStatus()
    }
    
    private fun initViews() {
        avatarView = findViewById(R.id.iv_counselor_avatar_edit)
        etName = findViewById(R.id.et_display_name)
        etCity = findViewById(R.id.et_city)
        etBrief = findViewById(R.id.et_brief_intro)
        tvBriefCounter = findViewById(R.id.tv_brief_counter)
        etEducation = findViewById(R.id.et_education)
        etYears = findViewById(R.id.et_years)
        etRichBio = findViewById(R.id.et_rich_bio)
        chipGroupExpertise = findViewById(R.id.chip_group_expertise)
        btnAddExpertiseTag = findViewById(R.id.btn_add_expertise_tag)
        btnSubmit = findViewById(R.id.btn_submit_changes)
    }
    
    private fun loadData() {
        // 加载头像
        val avatarUri = sp.getString("avatar_uri", null)
        if (avatarUri != null) {
            try {
                val uri = android.net.Uri.parse(avatarUri)
                avatarView.setImageURI(uri)
            } catch (_: Exception) {}
        }
        
        // 加载文本数据
        etName.setText(CryptoUtil.decrypt(sp.getString("display_name", null)) ?: "")
        etCity.setText(CryptoUtil.decrypt(sp.getString("city", null)) ?: "")
        etBrief.setText(CryptoUtil.decrypt(sp.getString("bio_brief", null)) ?: "")
        etEducation.setText(CryptoUtil.decrypt(sp.getString("edu", null)) ?: "")
        etYears.setText(CryptoUtil.decrypt(sp.getString("years", null)) ?: "")
        etRichBio.setText(CryptoUtil.decrypt(sp.getString("bio_rich", null)) ?: "")
        
        // 加载擅长领域标签
        val tagsJson = sp.getString("expertise_tags", null)
        if (tagsJson != null) {
            try {
                val tagsArray = JSONArray(tagsJson)
                expertiseTags.clear()
                for (i in 0 until tagsArray.length()) {
                    expertiseTags.add(tagsArray.getString(i))
                }
                updateExpertiseChips()
            } catch (_: Exception) {}
        }
        
        updateBriefCounter()
    }
    
    private fun setupListeners() {
        // 头像点击
        avatarView.setOnClickListener { selectAvatar() }
        
        // 学历背景下拉
        val eduOptions = arrayOf("本科", "硕士", "博士", "其他")
        val eduAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, eduOptions)
        etEducation.setAdapter(eduAdapter)
        etEducation.setOnClickListener { etEducation.showDropDown() }
        
        // 工作年限下拉
        val yearsOptions = (1..40).map { "$it 年" }.toTypedArray()
        val yearsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, yearsOptions)
        etYears.setAdapter(yearsAdapter)
        etYears.setOnClickListener { etYears.showDropDown() }
        
        // 文本变化监听
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveData()
                if (s == etBrief.editableText) {
                    updateBriefCounter()
                }
            }
        }
        
        etName.addTextChangedListener(textWatcher)
        etCity.addTextChangedListener(textWatcher)
        etBrief.addTextChangedListener(textWatcher)
        etEducation.addTextChangedListener(textWatcher)
        etYears.addTextChangedListener(textWatcher)
        etRichBio.addTextChangedListener(textWatcher)
        
        // 添加擅长领域标签按钮
        btnAddExpertiseTag.setOnClickListener { showAddExpertiseDialog() }
        
        // 提交按钮
        btnSubmit.setOnClickListener { confirmAndCommit() }
    }
    
    private fun updateBriefCounter() {
        val length = etBrief.text.length
        tvBriefCounter.text = "$length/200"
    }
    
    private fun showAddExpertiseDialog() {
        if (expertiseTags.size >= maxTags) {
            Toast.makeText(this, "最多只能添加${maxTags}个擅长领域", Toast.LENGTH_SHORT).show()
            return
        }
        
        val predefinedTags = arrayOf(
            "焦虑情绪", "抑郁情绪", "人际关系", "职场压力", "情感困扰",
            "家庭关系", "亲子教育", "婚姻问题", "个人成长", "情绪管理",
            "学业压力", "社交恐惧", "强迫症", "睡眠问题", "自我认知",
            "创伤修复", "压力管理", "青少年心理", "老年心理", "性心理"
        )
        
        val availableTags = predefinedTags.filter { !expertiseTags.contains(it) }.toTypedArray()
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("选择擅长领域")
        builder.setItems(availableTags) { dialog, which ->
            val selectedTag = availableTags[which]
            addExpertiseTag(selectedTag)
            dialog.dismiss()
        }
        builder.setNegativeButton("自定义") { dialog, _ ->
            dialog.dismiss()
            showCustomTagDialog()
        }
        builder.setNeutralButton("取消", null)
        builder.show()
    }
    
    private fun showCustomTagDialog() {
        if (expertiseTags.size >= maxTags) {
            Toast.makeText(this, "最多只能添加${maxTags}个擅长领域", Toast.LENGTH_SHORT).show()
            return
        }
        
        val input = EditText(this)
        input.hint = "请输入擅长领域（最多10个字）"
        input.maxLines = 1
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("自定义擅长领域")
        builder.setView(input)
        builder.setPositiveButton("确定") { dialog, _ ->
            val tag = input.text.toString().trim()
            if (tag.isEmpty()) {
                Toast.makeText(this, "请输入擅长领域", Toast.LENGTH_SHORT).show()
            } else if (tag.length > 10) {
                Toast.makeText(this, "擅长领域最多10个字", Toast.LENGTH_SHORT).show()
            } else if (expertiseTags.contains(tag)) {
                Toast.makeText(this, "该擅长领域已存在", Toast.LENGTH_SHORT).show()
            } else {
                addExpertiseTag(tag)
                dialog.dismiss()
            }
        }
        builder.setNegativeButton("取消", null)
        builder.show()
    }
    
    private fun addExpertiseTag(tag: String) {
        if (expertiseTags.size >= maxTags) {
            Toast.makeText(this, "最多只能添加${maxTags}个擅长领域", Toast.LENGTH_SHORT).show()
            return
        }
        
        expertiseTags.add(tag)
        updateExpertiseChips()
        saveExpertiseTags()
    }
    
    private fun removeExpertiseTag(tag: String) {
        expertiseTags.remove(tag)
        updateExpertiseChips()
        saveExpertiseTags()
    }
    
    private fun updateExpertiseChips() {
        chipGroupExpertise.removeAllViews()
        
        for (tag in expertiseTags) {
            val chip = Chip(this)
            chip.text = tag
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                removeExpertiseTag(tag)
            }
            chipGroupExpertise.addView(chip)
        }
    }
    
    private fun saveExpertiseTags() {
        val tagsArray = JSONArray(expertiseTags)
        sp.edit().putString("expertise_tags", tagsArray.toString()).apply()
    }
    
    private fun saveData() {
        sp.edit()
            .putString("display_name", CryptoUtil.encrypt(etName.text.toString()))
            .putString("city", CryptoUtil.encrypt(etCity.text.toString()))
            .putString("bio_brief", CryptoUtil.encrypt(etBrief.text.toString()))
            .putString("edu", CryptoUtil.encrypt(etEducation.text.toString()))
            .putString("years", CryptoUtil.encrypt(etYears.text.toString()))
            .putString("bio_rich", CryptoUtil.encrypt(etRichBio.text.toString()))
            .apply()
    }
    
    private fun selectAvatar() {
        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, 4011)
    }
    
    private fun confirmAndCommit() {
        val msg = "确定提交个人信息变更？"
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("确定") { _, _ -> commitChanges() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun commitChanges() {
        val payload = JSONObject()
        val name = etName.text.toString().trim()
        val city = etCity.text.toString().trim()
        val brief = etBrief.text.toString().trim()
        val edu = etEducation.text.toString().trim()
        val years = etYears.text.toString().trim()
        val rich = etRichBio.text.toString().trim()
        
        // 验证必填字段
        if (name.isEmpty()) {
            Toast.makeText(this, "请填写姓名", Toast.LENGTH_SHORT).show()
            return
        }
        
        val avatarUriStr = sp.getString("avatar_uri", null)
        var avatarB64: String? = null
        if (avatarUriStr != null) {
            try {
                val uri = android.net.Uri.parse(avatarUriStr)
                contentResolver.openInputStream(uri)?.use { ins ->
                    val bytes = ins.readBytes()
                    avatarB64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (_: Exception) {}
        }
        
        // 使用正确的字段名（与后端CounselorProfile实体类匹配）
        payload.put("displayName", name)
        payload.put("city", city)
        payload.put("briefIntro", brief)
        payload.put("education", edu)
        payload.put("workYears", years)
        payload.put("detailedIntro", rich)
        if (avatarB64 != null) payload.put("avatarBase64", avatarB64)
        
        // 添加擅长领域标签
        val tagsArray = JSONArray(expertiseTags)
        payload.put("tags", tagsArray)

        Thread {
            try {
                // 使用新的API接口保存咨询师资料
                val (success, errorMsg) = httpUpdateProfile(payload)
                if (success) {
                    // 保存成功后，更新SharedPreferences中的审核状态
                    sp.edit()
                        .putString("profile_status", "approved")
                        .putLong("last_update_time", System.currentTimeMillis())
                        .apply()
                    
                    runOnUiThread { 
                        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    return@Thread
                }
                
                // 如果新接口失败，显示错误信息
                runOnUiThread { 
                    val msg = if (errorMsg.isNotEmpty()) errorMsg else "提交失败，请稍后重试"
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    android.util.Log.e("CounselorInfoEdit", "提交失败: $msg")
                }
            } catch (e: Exception) {
                runOnUiThread { 
                    Toast.makeText(this, "提交失败: ${e.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("CounselorInfoEdit", "提交异常", e)
                }
            }
        }.start()
    }
    
    private fun httpUpdateProfile(payload: JSONObject): Pair<Boolean, String> {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
            
            // 获取token
            val loginSp = getSharedPreferences("loginInfo", MODE_PRIVATE)
            val token = loginSp.getString("auth_token", null)
            
            if (token == null) {
                return Pair(false, "未登录，请重新登录")
            }
            
            val media = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = RequestBody.create(media, payload.toString())
            val reqBuilder = Request.Builder()
                .url("http://10.0.2.2:8081/api/counselor/profile")
                .post(body)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
            
            android.util.Log.d("CounselorInfoEdit", "提交数据: ${payload.toString()}")
            
            val resp: Response = client.newCall(reqBuilder.build()).execute()
            val responseBody = resp.body?.string() ?: ""
            
            android.util.Log.d("CounselorInfoEdit", "响应状态码: ${resp.code}")
            android.util.Log.d("CounselorInfoEdit", "响应内容: $responseBody")
            
            if (resp.isSuccessful) {
                // 尝试解析响应
                try {
                    val json = JSONObject(responseBody)
                    val ok = json.optBoolean("ok", false)
                    val message = json.optString("message", "")
                    
                    if (ok) {
                        Pair(true, "")
                    } else {
                        Pair(false, message.ifEmpty { "保存失败" })
                    }
                } catch (e: Exception) {
                    // 如果无法解析JSON，但HTTP状态码是成功的，认为操作成功
                    Pair(true, "")
                }
            } else {
                val errorMsg = when (resp.code) {
                    401 -> "未授权，请重新登录"
                    403 -> "无权限访问"
                    404 -> "接口不存在"
                    500 -> "服务器错误: $responseBody"
                    else -> "请求失败(${resp.code}): $responseBody"
                }
                Pair(false, errorMsg)
            }
        } catch (e: Exception) { 
            android.util.Log.e("CounselorInfoEdit", "httpUpdateProfile failed", e)
            Pair(false, "网络错误: ${e.message}")
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 4011 && resultCode == android.app.Activity.RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(
                    uri, 
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            sp.edit().putString("avatar_uri", uri.toString()).apply()
            avatarView.setImageURI(uri)
            Toast.makeText(this, "头像已更新", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkReviewStatus() {
        Thread {
            try {
                // 首先尝试从新API加载咨询师资料
                val profileLoaded = loadProfileFromApi()
                if (profileLoaded) {
                    return@Thread
                }
                
                // 如果新API失败，尝试从数据库加载审核通过的资料
                val helper = MySQLHelper.getInstance()
                val conn: Connection = helper.getConnection()
                try {
                    val uid = com.example.xinqiao.util.AnalysisUtils.readUserId(this)
                    val sql = "SELECT id, payload FROM counselor_profile_reviews WHERE user_id=? AND status='approved' ORDER BY updated_at DESC LIMIT 1"
                    val ps = conn.prepareStatement(sql)
                    ps.setInt(1, uid)
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        val id = rs.getInt("id")
                        val lastApplied = sp.getInt("last_applied_review_id", -1)
                        if (id != lastApplied) {
                            val payloadStr = rs.getString("payload")
                            val obj = JSONObject(payloadStr)
                            val name = obj.optString("display_name", "")
                            val avatarB64 = obj.optString("avatar_base64", "")
                            if (name.isNotEmpty()) {
                                sp.edit().putString("display_name", CryptoUtil.encrypt(name)).apply()
                                try {
                                    val up = conn.prepareStatement("UPDATE user_info SET nickname=? WHERE user_id=?")
                                    up.setString(1, name)
                                    up.setInt(2, uid)
                                    up.executeUpdate()
                                } catch (_: Exception) {}
                            }
                            if (avatarB64.isNotEmpty()) {
                                try {
                                    val bytes = Base64.decode(avatarB64, Base64.NO_WRAP)
                                    val up = conn.prepareStatement("UPDATE user_info SET avatar=? WHERE user_id=?")
                                    up.setBytes(1, bytes)
                                    up.setInt(2, uid)
                                    up.executeUpdate()
                                } catch (_: Exception) {}
                            }
                            sp.edit().putInt("last_applied_review_id", id).apply()
                            runOnUiThread { 
                                Toast.makeText(this, "审核通过，信息已更新", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    helper.releaseConnection(conn)
                } catch (e: Exception) {
                    try { helper.releaseConnection(conn) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }.start()
    }
    
    private fun loadProfileFromApi(): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            val sp = getSharedPreferences("loginInfo", MODE_PRIVATE)
            val token = sp.getString("auth_token", null)
            
            val reqBuilder = Request.Builder()
                .url("http://10.0.2.2:8081/api/counselor/profile")
                .get()
            
            if (token != null) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            val resp: Response = client.newCall(reqBuilder.build()).execute()
            val responseBody = resp.body
            if (resp.isSuccessful && responseBody != null) {
                val bodyStr = responseBody.string()
                val json = JSONObject(bodyStr)
                
                // 解析并保存到SharedPreferences
                if (json.has("data")) {
                    val data = json.getJSONObject("data")
                    val displayName = data.optString("displayName", "")
                    val city = data.optString("city", "")
                    val briefIntro = data.optString("briefIntro", "")
                    val education = data.optString("education", "")
                    val workYears = data.optString("workYears", "")
                    val detailedIntro = data.optString("detailedIntro", "")
                    val avatarBase64 = data.optString("avatarBase64", "")
                    
                    // 加载擅长领域标签
                    val tagsArray = data.optJSONArray("tags")
                    if (tagsArray != null) {
                        expertiseTags.clear()
                        for (i in 0 until tagsArray.length()) {
                            expertiseTags.add(tagsArray.getString(i))
                        }
                        saveExpertiseTags()
                    }
                    
                    this.sp.edit()
                        .putString("display_name", CryptoUtil.encrypt(displayName))
                        .putString("city", CryptoUtil.encrypt(city))
                        .putString("bio_brief", CryptoUtil.encrypt(briefIntro))
                        .putString("edu", CryptoUtil.encrypt(education))
                        .putString("years", CryptoUtil.encrypt(workYears))
                        .putString("bio_rich", CryptoUtil.encrypt(detailedIntro))
                        .apply()
                    
                    if (avatarBase64.isNotEmpty()) {
                        // 保存头像到本地文件
                        try {
                            val bytes = Base64.decode(avatarBase64, Base64.NO_WRAP)
                            val file = java.io.File(cacheDir, "counselor_avatar.jpg")
                            java.io.FileOutputStream(file).use { it.write(bytes) }
                            this.sp.edit().putString("avatar_uri", android.net.Uri.fromFile(file).toString()).apply()
                        } catch (_: Exception) {}
                    }
                    
                    runOnUiThread {
                        loadData() // 重新加载数据到UI
                        updateExpertiseChips() // 更新擅长领域标签显示
                    }
                }
                resp.close()
                true
            } else {
                resp.close()
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("CounselorInfoEdit", "loadProfileFromApi failed", e)
            false
        }
    }

    private fun httpSubmitReview(payload: JSONObject): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
            val media = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = RequestBody.create(media, payload.toString())
            val req = Request.Builder()
                .url("http://10.0.2.2:8081/api/counselors/reviews")
                .post(body)
                .build()
            val resp: Response = client.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (_: Exception) { 
            false 
        }
    }
}