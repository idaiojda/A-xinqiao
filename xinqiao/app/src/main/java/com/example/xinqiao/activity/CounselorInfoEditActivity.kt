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

class CounselorInfoEditActivity : AppCompatActivity() {
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var btnSubmit: Button
    private lateinit var btnHistory: ImageView
    private val sp by lazy { getSharedPreferences("counselor_profile", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counselor_info_edit)
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        btnHistory = findViewById(R.id.btn_history)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        btnSubmit = findViewById(R.id.btn_submit_changes)

        viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val v = when (viewType) {
                    0 -> inflater.inflate(R.layout.page_qualification_edit, parent, false)
                    1 -> inflater.inflate(R.layout.page_pricing_edit, parent, false)
                    else -> inflater.inflate(R.layout.page_bio_edit, parent, false)
                }
                return object : RecyclerView.ViewHolder(v) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                when (position) {
                    0 -> bindQualification(holder.itemView)
                    1 -> bindPricing(holder.itemView)
                    2 -> bindBio(holder.itemView)
                }
            }
            override fun getItemCount(): Int = 3
            override fun getItemViewType(position: Int): Int = position
        }
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = when (pos) { 0 -> "资质信息"; 1 -> "服务定价"; else -> "个人简介" }
        }.attach()

        btnSubmit.setOnClickListener { confirmAndCommit() }
        btnHistory.setOnClickListener { showHistory() }
        checkReviewStatus()
    }

    private fun bindQualification(v: View) {
        val etEdu: AutoCompleteTextView = v.findViewById(R.id.et_education)
        val etCert: AutoCompleteTextView = v.findViewById(R.id.et_certifications)
        val etYears: AutoCompleteTextView = v.findViewById(R.id.et_years)
        val tvStatus: TextView = v.findViewById(R.id.tv_verify_status)
        val encEdu = sp.getString("edu", null)
        val encCert = sp.getString("cert", null)
        val encYears = sp.getString("years", null)
        etEdu.setText(CryptoUtil.decrypt(encEdu) ?: "", false)
        etCert.setText(CryptoUtil.decrypt(encCert) ?: "", false)
        etYears.setText(CryptoUtil.decrypt(encYears) ?: "", false)
        tvStatus.text = sp.getString("qual_status", "待审核")
        val eduOptions = arrayOf("本科", "硕士", "博士", "其他")
        val certOptions = arrayOf("国家二级心理咨询师", "国家三级心理咨询师", "注册心理咨询师", "督导师", "其他")
        val yearsOptions = (1..40).map { "$it 年" }.toTypedArray()

        fun <T> setDropDown(view: AutoCompleteTextView, opts: Array<T>) {
            val adp = ArrayAdapter(this, android.R.layout.simple_list_item_1, opts)
            view.setAdapter(adp)
            view.setOnItemClickListener { _, _, _, _ ->
                saveQualification(etEdu.text.toString(), etCert.text.toString(), etYears.text.toString())
            }
            view.setOnClickListener { view.showDropDown() }
        }

        setDropDown(etEdu, eduOptions)
        setDropDown(etCert, certOptions)
        setDropDown(etYears, yearsOptions)
        v.findViewById<Button>(R.id.btn_upload_files).setOnClickListener { selectFiles() }
    }

    private fun saveQualification(edu: String, cert: String, years: String) {
        sp.edit().putString("edu", CryptoUtil.encrypt(edu)).putString("cert", CryptoUtil.encrypt(cert)).putString("years", CryptoUtil.encrypt(years)).apply()
        appendLog("qualification", "draft", hash(edu + cert + years))
    }

    private fun bindPricing(v: View) {
        val etBase: EditText = v.findViewById(R.id.et_base_rate)
        val spUnit: Spinner = v.findViewById(R.id.sp_rate_unit)
        val etText: EditText = v.findViewById(R.id.et_text_consult_rate)
        val etVoice: EditText = v.findViewById(R.id.et_voice_consult_rate)
        val etVideo: EditText = v.findViewById(R.id.et_video_consult_rate)
        val encBase = sp.getString("base_rate", null)
        val unit = sp.getString("base_unit", "按小时")
        val encText = sp.getString("rate_text", null)
        val encVoice = sp.getString("rate_voice", null)
        val encVideo = sp.getString("rate_video", null)
        etBase.setText(CryptoUtil.decrypt(encBase))
        etText.setText(CryptoUtil.decrypt(encText))
        etVoice.setText(CryptoUtil.decrypt(encVoice))
        etVideo.setText(CryptoUtil.decrypt(encVideo))
        val units = arrayOf("按小时", "按次")
        spUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units)
        spUnit.setSelection(units.indexOf(unit).coerceAtLeast(0))
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { savePricing(etBase.text.toString(), spUnit.selectedItem.toString(), etText.text.toString(), etVoice.text.toString(), etVideo.text.toString()) }
        }
        etBase.addTextChangedListener(watcher)
        etText.addTextChangedListener(watcher)
        etVoice.addTextChangedListener(watcher)
        etVideo.addTextChangedListener(watcher)
        spUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) { savePricing(etBase.text.toString(), units[position], etText.text.toString(), etVoice.text.toString(), etVideo.text.toString()) }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun savePricing(base: String, unit: String, text: String, voice: String, video: String) {
        sp.edit().putString("base_rate", CryptoUtil.encrypt(base)).putString("base_unit", unit).putString("rate_text", CryptoUtil.encrypt(text)).putString("rate_voice", CryptoUtil.encrypt(voice)).putString("rate_video", CryptoUtil.encrypt(video)).apply()
        appendLog("pricing", "draft", hash(base + unit + text + voice + video))
    }

    private fun bindBio(v: View) {
        val avatarView: ImageView? = v.findViewById(R.id.iv_counselor_avatar_edit)
        val etName: EditText? = v.findViewById(R.id.et_display_name)
        val etBrief: EditText = v.findViewById(R.id.et_brief_intro)
        val etRich: EditText = v.findViewById(R.id.et_rich_bio)
        val encBrief = sp.getString("bio_brief", null)
        val encRich = sp.getString("bio_rich", null)
        val encName = sp.getString("display_name", null)
        val avatarUri = sp.getString("avatar_uri", null)
        etBrief.setText(CryptoUtil.decrypt(encBrief))
        etRich.setText(CryptoUtil.decrypt(encRich))
        etName?.setText(CryptoUtil.decrypt(encName) ?: "")
        if (avatarView != null && avatarUri != null) {
            try {
                val uri = android.net.Uri.parse(avatarUri)
                avatarView.setImageURI(uri)
            } catch (_: Exception) {}
        }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { saveBio(etBrief.text.toString(), etRich.text.toString()) }
        }
        etBrief.addTextChangedListener(watcher)
        etRich.addTextChangedListener(watcher)
        etName?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = etName.text.toString()
                sp.edit().putString("display_name", CryptoUtil.encrypt(name)).apply()
                appendLog("profile", "name", hash(name))
            }
        })
        avatarView?.setOnClickListener { selectAvatar() }
    }

    private fun saveBio(brief: String, rich: String) {
        val b = brief.take(200)
        sp.edit().putString("bio_brief", CryptoUtil.encrypt(b)).putString("bio_rich", CryptoUtil.encrypt(rich)).apply()
        appendLog("bio", "draft", hash(b + rich))
    }

    private fun selectFiles() {
        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE)
        intent.type = "application/*"
        intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))
        startActivityForResult(intent, 3011)
    }

    private fun selectAvatar() {
        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, 4011)
    }

    private fun confirmAndCommit() {
        val msg = "确定提交关键信息变更？提交后将进入审核流程"
        AlertDialog.Builder(this).setMessage(msg).setPositiveButton("确定") { _, _ -> commitChanges() }.setNegativeButton("取消", null).show()
    }

    private fun commitChanges() {
        val payload = JSONObject()
        val name = CryptoUtil.decrypt(sp.getString("display_name", null)) ?: ""
        val edu = CryptoUtil.decrypt(sp.getString("edu", null)) ?: ""
        val cert = CryptoUtil.decrypt(sp.getString("cert", null)) ?: ""
        val years = CryptoUtil.decrypt(sp.getString("years", null)) ?: ""
        val baseRate = CryptoUtil.decrypt(sp.getString("base_rate", null)) ?: ""
        val baseUnit = sp.getString("base_unit", "按小时") ?: "按小时"
        val rateText = CryptoUtil.decrypt(sp.getString("rate_text", null)) ?: ""
        val rateVoice = CryptoUtil.decrypt(sp.getString("rate_voice", null)) ?: ""
        val rateVideo = CryptoUtil.decrypt(sp.getString("rate_video", null)) ?: ""
        val brief = CryptoUtil.decrypt(sp.getString("bio_brief", null)) ?: ""
        val rich = CryptoUtil.decrypt(sp.getString("bio_rich", null)) ?: ""
        val userId = com.example.xinqiao.util.AnalysisUtils.readUserId(this)
        val phoneMasked = sp.getString("phone_masked", null)
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
        payload.put("display_name", name)
        payload.put("education", edu)
        payload.put("certification", cert)
        payload.put("years", years)
        payload.put("base_rate", baseRate)
        payload.put("base_unit", baseUnit)
        payload.put("rate_text", rateText)
        payload.put("rate_voice", rateVoice)
        payload.put("rate_video", rateVideo)
        payload.put("bio_brief", brief)
        payload.put("bio_rich", rich)
        payload.put("user_id", userId)
        if (phoneMasked != null) payload.put("phone", phoneMasked)
        payload.put("submitted_at", System.currentTimeMillis())
        if (avatarB64 != null) payload.put("avatar_base64", avatarB64)

        Thread {
            try {
                val httpOk = httpSubmitReview(payload)
                if (httpOk) {
                    sp.edit().putString("qual_status", "待审核").apply()
                    appendLog("commit", "submit_http", hash(payload.toString()))
                    runOnUiThread { Toast.makeText(this, "已提交到管理员后台，等待审核", Toast.LENGTH_SHORT).show() }
                    return@Thread
                }
                val helper = MySQLHelper.getInstance()
                val conn: Connection = helper.getConnection()
                try {
                    conn.createStatement().execute(
                        "CREATE TABLE IF NOT EXISTS counselor_profile_reviews (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "user_id INT NOT NULL, " +
                            "status VARCHAR(16) NOT NULL, " +
                            "payload TEXT, " +
                            "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                            "INDEX idx_user_status (user_id, status), " +
                            "FOREIGN KEY (user_id) REFERENCES user_info(user_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                    )
                    val ps: PreparedStatement = conn.prepareStatement(
                        "INSERT INTO counselor_profile_reviews (user_id, status, payload) VALUES (?, 'pending', ?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS
                    )
                    val uid = com.example.xinqiao.util.AnalysisUtils.readUserId(this)
                    ps.setInt(1, uid)
                    ps.setString(2, payload.toString())
                    ps.executeUpdate()
                    val rs = ps.generatedKeys
                    var reviewId = -1
                    if (rs.next()) reviewId = rs.getInt(1)
                    helper.releaseConnection(conn)
                    sp.edit().putString("qual_status", "待审核").putInt("pending_review_id", reviewId).apply()
                    appendLog("commit", "submit", reviewId.toString())
                    runOnUiThread { Toast.makeText(this, "已提交，等待管理员审核", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) {
                    try { helper.releaseConnection(conn) } catch (_: Exception) {}
                    enqueueLocalReview(payload)
                    runOnUiThread { Toast.makeText(this, "网络/数据库不可用，已离线排队", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                enqueueLocalReview(payload)
                runOnUiThread { Toast.makeText(this, "数据库未初始化或网络不可用，已离线排队", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun showHistory() {
        val logs = sp.getString("profile_logs", "")
        AlertDialog.Builder(this).setTitle("历史记录").setMessage(logs ?: "").setPositiveButton("关闭", null).show()
    }

    private fun appendLog(section: String, action: String, value: String) {
        val now = System.currentTimeMillis()
        val line = "$now|$section|$action|$value\n"
        val old = sp.getString("profile_logs", "") ?: ""
        sp.edit().putString("profile_logs", old + line).apply()
    }

    private fun hash(s: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val h = md.digest(s.toByteArray())
        return h.joinToString("") { String.format("%02x", it) }.take(32)
    }

    private fun createPricingVersion() {
        val base = CryptoUtil.decrypt(sp.getString("base_rate", null)) ?: ""
        val unit = sp.getString("base_unit", "按小时") ?: "按小时"
        val text = CryptoUtil.decrypt(sp.getString("rate_text", null)) ?: ""
        val voice = CryptoUtil.decrypt(sp.getString("rate_voice", null)) ?: ""
        val video = CryptoUtil.decrypt(sp.getString("rate_video", null)) ?: ""
        val snapshot = "base=$base($unit);文字=$text;语音=$voice;视频=$video"
        val ts = System.currentTimeMillis()
        val entry = "$ts|pricing|version|${hash(snapshot)}\n"
        val old = sp.getString("pricing_versions", "") ?: ""
        sp.edit().putString("pricing_versions", old + entry).apply()
        appendLog("pricing", "version", hash(snapshot))
        Toast.makeText(this, "已创建定价版本", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3011 && resultCode == android.app.Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val mm = contentResolver.getType(uri) ?: ""
            if (!(mm.startsWith("image/") || mm == "application/pdf")) {
                Toast.makeText(this, "不支持的文件类型", Toast.LENGTH_SHORT).show()
                return
            }
            val inS = contentResolver.openInputStream(uri) ?: return
            val bytes = inS.readBytes()
            inS.close()
            val dg = hash(String(bytes))
            appendLog("qualification_file", "selected", dg)
            Toast.makeText(this, "文件已验证", Toast.LENGTH_SHORT).show()
        } else if (requestCode == 4011 && resultCode == android.app.Activity.RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            sp.edit().putString("avatar_uri", uri.toString()).apply()
            appendLog("profile", "avatar", hash(uri.toString()))
            val current = window.decorView.findViewById<ImageView?>(R.id.iv_counselor_avatar_edit)
            current?.setImageURI(uri)
            Toast.makeText(this, "头像已更新", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkReviewStatus() {
        Thread {
            try {
                val helper = MySQLHelper.getInstance()
                val conn: Connection = helper.getConnection()
                try {
                    flushLocalReviewQueue(conn, helper)
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
                            sp.edit().putString("qual_status", "已认证").putInt("last_applied_review_id", id).apply()
                            appendLog("commit", "approved", id.toString())
                            runOnUiThread { Toast.makeText(this, "管理员审核通过，信息已更新", Toast.LENGTH_SHORT).show() }
                        }
                    }
                } catch (e: Exception) {
                    try { helper.releaseConnection(conn) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun enqueueLocalReview(payload: JSONObject) {
        try {
            val arrStr = sp.getString("local_review_queue", "[]") ?: "[]"
            val arr = JSONArray(arrStr)
            val obj = JSONObject()
            obj.put("ts", System.currentTimeMillis())
            obj.put("payload", payload)
            arr.put(obj)
            sp.edit().putString("local_review_queue", arr.toString()).apply()
            appendLog("commit", "queued", hash(payload.toString()))
        } catch (_: Exception) {}
    }

    private fun flushLocalReviewQueue(conn: Connection, helper: MySQLHelper) {
        try {
            val arrStr = sp.getString("local_review_queue", null) ?: return
            val arr = JSONArray(arrStr)
            if (arr.length() == 0) return
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS counselor_profile_reviews (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "status VARCHAR(16) NOT NULL, " +
                    "payload TEXT, " +
                    "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_user_status (user_id, status), " +
                    "FOREIGN KEY (user_id) REFERENCES user_info(user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            )
            val ps = conn.prepareStatement("INSERT INTO counselor_profile_reviews (user_id, status, payload) VALUES (?, 'pending', ?)")
            val uid = com.example.xinqiao.util.AnalysisUtils.readUserId(this)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val payload = obj.getJSONObject("payload")
                ps.setInt(1, uid)
                ps.setString(2, payload.toString())
                ps.addBatch()
            }
            ps.executeBatch()
            sp.edit().remove("local_review_queue").apply()
            appendLog("commit", "flushed", uid.toString())
            Toast.makeText(this, "离线审核队列已同步", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    private fun httpSubmitReview(payload: JSONObject): Boolean {
        return try {
            val client = OkHttpClient.Builder().connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS).readTimeout(10, java.util.concurrent.TimeUnit.SECONDS).writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS).build()
            val media = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = RequestBody.create(media, payload.toString())
            val req = Request.Builder().url("http://10.0.2.2:3003/api/counselors/reviews").post(body).build()
            val resp: Response = client.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (_: Exception) { false }
    }
}