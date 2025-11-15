package com.example.xinqiao.consultation.pro

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xinqiao.room.repository.UserRepository
import com.example.xinqiao.room.entity.UserInfo
import com.example.xinqiao.util.AnalysisUtils
import com.example.xinqiao.mysql.DBUtils
import com.example.xinqiao.util.PhoneUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.xinqiao.repository.MedicalRecordRepository
import com.example.xinqiao.room.entities.ConsultationEntity
import com.example.xinqiao.util.crypto.CryptoUtil

data class AppointmentUiState(
    val consultantId: String = "",
    val consultantName: String = "",
    val defaultMode: String = "文字咨询",
    val selectedMode: String = "文字咨询",
    val dates: List<String> = emptyList(),
    val selectedDate: String = "",
    val slots: List<SlotTime> = emptyList(),
    val selectedTime: SlotTime? = null,
    val basePrice: Int = 299,
    val price: Int = 299,
    val durationMinutes: Int = 60,
    val coupon: Int = 0,
    val remark: String = "",
    val maskedPhone: String = "",
    val nickname: String = "",
    val phoneOk: Boolean = false,
    val missingFields: String = "",
    val profileComplete: Boolean = false,
    val loginRequired: Boolean = false,
    val loadingSlots: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null
)

class AppointmentDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    private val repo = AppointmentRepository(ctx)
    private val userRepo = UserRepository(ctx)

    private val _ui = MutableStateFlow(AppointmentUiState())
    val ui: StateFlow<AppointmentUiState> = _ui

    private fun readToken(): String? {
        val sp = ctx.getSharedPreferences("loginInfo", Context.MODE_PRIVATE)
        return sp.getString("auth_token", null)
    }

    fun init(consultantId: String, name: String, defaultMode: String, price: Int = 299, durationMinutes: Int = 60) {
        val today = LocalDate.now()
        val fmt = DateTimeFormatter.ISO_DATE
        val dateList = (0 until 7).map { today.plusDays(it.toLong()).format(fmt) }
        _ui.value = _ui.value.copy(
            consultantId = consultantId,
            consultantName = name,
            defaultMode = defaultMode,
            selectedMode = defaultMode,
            dates = dateList,
            selectedDate = dateList.first(),
            basePrice = price,
            price = price,
            durationMinutes = durationMinutes
        )
        checkLoginAndProfile()
        loadSlots(dateList.first())
    }

    fun selectMode(mode: String) {
        val priceDelta = when (mode) {
            "语音咨询" -> 50
            "视频咨询" -> 100
            else -> 0
        }
        val base = _ui.value.basePrice
        _ui.value = _ui.value.copy(selectedMode = mode, price = base + priceDelta)
    }

    fun selectDate(date: String) {
        _ui.value = _ui.value.copy(selectedDate = date)
        loadSlots(date)
    }

    fun selectTime(slot: SlotTime) {
        if (!slot.available) return
        _ui.value = _ui.value.copy(selectedTime = slot)
    }

    fun updateRemark(text: String) {
        _ui.value = _ui.value.copy(remark = text)
    }

    fun applyCoupon(amount: Int) {
        _ui.value = _ui.value.copy(coupon = amount)
    }

    fun refreshLoginStatus() {
        checkLoginAndProfile()
    }

    private fun checkLoginAndProfile() {
        // 先尝试读取登录手机号（用户名）
        val loginUserName = AnalysisUtils.readLoginUserName(ctx)
        if (loginUserName.isNotBlank()) {
            // 已登录，按用户名查询资料
            viewModelScope.launch {
                userRepo.getUserByUsername(loginUserName, object : UserRepository.OperationCallback<UserInfo> {
                    override fun onSuccess(result: UserInfo?) {
                        val username = (result?.username ?: loginUserName)
                        val nickname = result?.nickname ?: ""
                        val phoneOk = PhoneUtils.isValidPhoneNumber(username)
                        val nicknameOk = nickname.isNotBlank()
                        val complete = phoneOk && nicknameOk
                        val masked = if (phoneOk) PhoneUtils.formatPhoneForDisplay(username) else ""
                        val missing = when {
                            !phoneOk && !nicknameOk -> "昵称、手机号"
                            !phoneOk -> "手机号"
                            !nicknameOk -> "昵称"
                            else -> ""
                        }
                        _ui.value = _ui.value.copy(
                            loginRequired = false,
                            profileComplete = complete,
                            maskedPhone = masked,
                            nickname = nickname,
                            phoneOk = phoneOk,
                            missingFields = missing
                        )

                        // Room 昵称为空时，回退到 MySQL 查询昵称
                        if (!nicknameOk && phoneOk) {
                            DBUtils.getInstance(ctx).getUserNickname(username, object : DBUtils.UserNicknameCallback {
                                override fun onSuccess(nick: String?) {
                                    val resolved = nick ?: ""
                                    val nicknameOk2 = resolved.isNotBlank()
                                    val complete2 = phoneOk && nicknameOk2
                                    val missing2 = when {
                                        !phoneOk && !nicknameOk2 -> "昵称、手机号"
                                        !phoneOk -> "手机号"
                                        !nicknameOk2 -> "昵称"
                                        else -> ""
                                    }
                                    _ui.value = _ui.value.copy(
                                        profileComplete = complete2,
                                        nickname = resolved,
                                        missingFields = missing2
                                    )
                                }
                                override fun onError(e: java.sql.SQLException?) {
                                    // 保持原状态，不额外处理
                                }
                            })
                        }
                    }

                    override fun onError(e: Exception) {
                        // 查询失败也视为已登录，仅标记资料不完整
                        val phoneOk = PhoneUtils.isValidPhoneNumber(loginUserName)
                        val masked = if (phoneOk) PhoneUtils.formatPhoneForDisplay(loginUserName) else ""
                        _ui.value = _ui.value.copy(
                            loginRequired = false,
                            profileComplete = false,
                            maskedPhone = masked,
                            phoneOk = phoneOk,
                            missingFields = if (phoneOk) "昵称" else "昵称、手机号"
                        )

                        // Room 查询失败时，尝试直接从 MySQL 获取昵称
                        if (phoneOk) {
                            DBUtils.getInstance(ctx).getUserNickname(loginUserName, object : DBUtils.UserNicknameCallback {
                                override fun onSuccess(nick: String?) {
                                    val resolved = nick ?: ""
                                    if (resolved.isNotBlank()) {
                                        _ui.value = _ui.value.copy(
                                            profileComplete = true,
                                            nickname = resolved,
                                            missingFields = ""
                                        )
                                    }
                                }
                                override fun onError(e: java.sql.SQLException?) {
                                    // 保持原状态
                                }
                            })
                        }
                    }
                })
            }
            return
        }

        // 若用户名为空，再按用户ID读取
        val uid = AnalysisUtils.readUserId(ctx)
        if (uid == -1) {
            _ui.value = _ui.value.copy(loginRequired = true, profileComplete = false, maskedPhone = "", nickname = "")
            return
        }
        viewModelScope.launch {
            userRepo.getUserById(uid, object : UserRepository.OperationCallback<UserInfo> {
                override fun onSuccess(result: UserInfo?) {
                    val username = result?.username ?: ""
                    val nickname = result?.nickname ?: ""
                    val phoneOk = PhoneUtils.isValidPhoneNumber(username)
                    val nicknameOk = nickname.isNotBlank()
                    val complete = phoneOk && nicknameOk
                    val masked = if (phoneOk) PhoneUtils.formatPhoneForDisplay(username) else ""
                    val missing = when {
                        !phoneOk && !nicknameOk -> "昵称、手机号"
                        !phoneOk -> "手机号"
                        !nicknameOk -> "昵称"
                        else -> ""
                    }
                    _ui.value = _ui.value.copy(
                        loginRequired = false,
                        profileComplete = complete,
                        maskedPhone = masked,
                        nickname = nickname,
                        phoneOk = phoneOk,
                        missingFields = missing
                    )

                    // 如果用户名或昵称在 Room 中缺失，回退到 MySQL 查询
                    if (!phoneOk || !nicknameOk) {
                        // 先尝试从 MySQL 计算当前用户名
                        DBUtils.getInstance(ctx).getCurrentUserName(ctx, object : DBUtils.UserNameCallback {
                            override fun onSuccess(name: String?) {
                                val u = if (!name.isNullOrBlank()) name else username
                                val phoneOk2 = PhoneUtils.isValidPhoneNumber(u)
                                val masked2 = if (phoneOk2) PhoneUtils.formatPhoneForDisplay(u) else ""
                                // 更新手机号显示
                                _ui.value = _ui.value.copy(maskedPhone = masked2, phoneOk = phoneOk2)

                                if (phoneOk2) {
                                    DBUtils.getInstance(ctx).getUserNickname(u, object : DBUtils.UserNicknameCallback {
                                        override fun onSuccess(nick: String?) {
                                            val resolved = nick ?: ""
                                            val nicknameOk2 = resolved.isNotBlank()
                                            val complete2 = phoneOk2 && nicknameOk2
                                            val missing2 = when {
                                                !phoneOk2 && !nicknameOk2 -> "昵称、手机号"
                                                !phoneOk2 -> "手机号"
                                                !nicknameOk2 -> "昵称"
                                                else -> ""
                                            }
                                            _ui.value = _ui.value.copy(
                                                profileComplete = complete2,
                                                nickname = resolved,
                                                missingFields = missing2
                                            )
                                        }
                                        override fun onError(e: java.sql.SQLException?) {
                                            // 保持当前状态
                                        }
                                    })
                                }
                            }
                            override fun onError(e: java.sql.SQLException?) {
                                // 保持当前状态
                            }
                        })
                    }
                }

                override fun onError(e: Exception) {
                    _ui.value = _ui.value.copy(loginRequired = true, profileComplete = false)
                }
            })
        }
    }

    private fun loadSlots(date: String) {
        _ui.value = _ui.value.copy(loadingSlots = true, error = null)
        val cid = _ui.value.consultantId
        val token = readToken()
        viewModelScope.launch {
            val res = repo.fetchSlots(cid, date, token)
            _ui.value = _ui.value.copy(loadingSlots = false)
            res.onSuccess { list ->
                _ui.value = _ui.value.copy(slots = list, selectedTime = null)
            }.onFailure { e ->
                _ui.value = _ui.value.copy(error = e.message)
            }
        }
    }

    fun submit(onResult: (Boolean, String?) -> Unit) {
        val s = _ui.value
        if (s.loginRequired || !s.profileComplete) {
            onResult(false, "请先登录并完善个人信息")
            return
        }
        if (s.selectedTime == null) {
            onResult(false, "请选择预约时间段")
            return
        }
        _ui.value = _ui.value.copy(submitting = true)
        val token = readToken()
        val req = AppointmentRequest(
            consultantId = s.consultantId,
            mode = s.selectedMode,
            date = s.selectedDate,
            time = s.selectedTime.start,
            remark = s.remark.ifBlank { null }
        )
        viewModelScope.launch {
            val result = repo.submitAppointment(req, token)
            _ui.value = _ui.value.copy(submitting = false)
            result.onSuccess { id ->
                // 预约成功后，保存一条专业咨询记录到个人诊疗档案
                try {
                    val userName = AnalysisUtils.readLoginUserName(ctx) ?: ""
                    if (userName.isNotBlank()) {
                        val repoMR = MedicalRecordRepository(ctx)
                        val entity = ConsultationEntity().apply {
                            this.userName = userName
                            this.sessionId = id ?: (System.currentTimeMillis().toString())
                            this.type = "pro"
                            this.title = "预约咨询 - ${s.consultantName}"
                            this.date = s.selectedDate
                            this.messageCount = 0
                            val summary = "预约成功：${s.selectedMode}，${s.selectedDate} ${s.selectedTime?.start ?: ""}，咨询师 ${s.consultantName}"
                            this.summaryEncrypted = CryptoUtil.encrypt(summary)
                            this.status = "待咨询"
                        }
                        // Room操作放到IO线程，避免阻塞UI
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                repoMR.addConsultation(entity)
                            } catch (_: Exception) {
                                // 持久化失败不影响主流程
                            }
                        }
                    }
                } catch (_: Exception) {
                    // 不影响用户提示
                }
                onResult(true, id)
            }.onFailure { e ->
                onResult(false, e.message)
            }
        }
    }
}
