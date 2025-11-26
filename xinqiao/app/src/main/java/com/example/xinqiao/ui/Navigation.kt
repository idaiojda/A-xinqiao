package com.example.xinqiao.ui

import android.content.Context
import android.content.Intent
import com.example.xinqiao.activity.AppointmentDetailActivity

object Navigation {
    fun toAppointmentDetail(ctx: Context, consultantId: String, name: String, mode: String) {
        val intent = Intent(ctx, AppointmentDetailActivity::class.java)
        intent.putExtra(Routes.EXTRA_CONSULTANT_ID, consultantId)
        intent.putExtra(Routes.EXTRA_NAME, name)
        intent.putExtra(Routes.EXTRA_MODE, mode)
        ctx.startActivity(intent)
    }
}