package com.example.xinqiao.consultation.pro

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xinqiao.room.repository.UserRepository
import com.example.xinqiao.room.entity.UserInfo
import com.example.xinqiao.utils.AnalysisUtils
import com.example.xinqiao.utils.PhoneUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AppointmentUiState(
    val consultantId: String = "",
    val consultantName: String = "",
    val defaultMode: String = "文字咨询",
    val selectedMode: String = "文字咨询",
    val dates: List<String> = emptyList(),
    val selectedDate: String = "",
    val slots: List<SlotTime> = emptyList(),
    val selectedTime: SlotTime? = null,
    val price: Int = 299,
    val durationMinutes: Int = 60,
    val coupon: Int = 0,
    val remark: String = "",
    val maskedPhone: String = "",
    val nickname: String = "",
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
        _ui.value = _ui.value.copy(selectedMode = mode, price = 299 + priceDelta)
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

    private fun checkLoginAndProfile() {
        val uid = AnalysisUtils.readUserId(ctx)
        val login = uid != -1
        if (!login) {
            _ui.value = _ui.value.copy(loginRequired = true, profileComplete = false, maskedPhone = "", nickname = "")
            return
        }
        viewModelScope.launch {
            userRepo.getUserById(uid, object : UserRepository.OperationCallback<UserInfo> {
                override fun onSuccess(result: UserInfo?) {
                    val username = result?.username ?: ""
                    val nickname = result?.nickname ?: ""
                    val phoneOk = PhoneUtils.isValidPhoneNumber(username)
                    val complete = phoneOk && nickname.isNotBlank()
                    val masked = if (phoneOk) PhoneUtils.formatPhoneForDisplay(username) else ""
                    _ui.value = _ui.value.copy(
                        loginRequired = false,
                        profileComplete = complete,
                        maskedPhone = masked,
                        nickname = nickname
                    )
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
                onResult(true, id)
            }.onFailure { e ->
                onResult(false, e.message)
            }
        }
    }
}

