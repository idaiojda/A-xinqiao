package com.example.xinqiao.ui

import android.content.Context
import android.content.Intent
import com.example.xinqiao.activity.AppointmentDetailActivity
import com.example.xinqiao.activity.ConsultationRoomActivity

object Navigation {
    fun toAppointmentDetail(ctx: Context, consultantId: String, name: String, mode: String) {
        val intent = Intent(ctx, AppointmentDetailActivity::class.java)
        intent.putExtra(Routes.EXTRA_CONSULTANT_ID, consultantId)
        intent.putExtra(Routes.EXTRA_NAME, name)
        intent.putExtra(Routes.EXTRA_MODE, mode)
        ctx.startActivity(intent)
    }
    
    fun toConsultationRoom(ctx: Context, chatId: String, targetName: String, appointmentType: String) {
        val mode = determineMode(appointmentType)
        val intent = Intent(ctx, ConsultationRoomActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("targetName", targetName)
        intent.putExtra("mode", mode)
        ctx.startActivity(intent)
    }
    
    private fun determineMode(appointmentType: String): String {
        return when {
            // 支持英文mode
            appointmentType.equals("text", ignoreCase = true) -> "text"
            appointmentType.equals("voice", ignoreCase = true) -> "voice"
            appointmentType.equals("video", ignoreCase = true) -> "video"
            // 支持中文mode
            appointmentType.contains("文字", ignoreCase = true) -> "text"
            appointmentType.contains("语音", ignoreCase = true) -> "voice"
            appointmentType.contains("视频", ignoreCase = true) -> "video"
            else -> appointmentType.lowercase() // 直接返回小写的原值
        }
    }
}