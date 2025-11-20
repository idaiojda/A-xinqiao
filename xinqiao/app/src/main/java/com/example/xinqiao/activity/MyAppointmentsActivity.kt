package com.example.xinqiao.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import kotlinx.coroutines.launch

class MyAppointmentsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyAppointmentsScreen(onBack = { finish() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyAppointmentsScreen(onBack: () -> Unit) {
    val themeColor = Color(0xFF2F54EB)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Scaffold(topBar = {
        androidx.compose.foundation.layout.Box {
            AndroidView(
                factory = { ctx ->
                    View(ctx).apply { setBackgroundResource(com.example.xinqiao.R.drawable.topbar_history_bg) }
                },
                modifier = Modifier.matchParentSize()
            )
            TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
            ), title = { Text("我的预约", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } })
        }
    }) { padding ->
        val repo = remember { com.example.xinqiao.user.UserAppointmentRepository(ctx) }
        var items by remember { mutableStateOf(listOf<AppointmentItem>()) }
        LaunchedEffect(Unit) {
            val res = repo.listMine()
            res.onSuccess { list ->
                items = list.map { AppointmentItem(it.counselor, it.counselor, "", it.startTime.replace('T', ' '), when (it.status) {
                    "PENDING" -> "待咨询"
                    "APPROVED" -> "待咨询"
                    "REJECTED" -> "已驳回"
                    "COMPLETED" -> "已完成"
                    "CANCELLED" -> "已取消"
                    else -> it.status
                }, it.id) }
            }
        }
        LazyColumn(modifier = Modifier
            .padding(padding)
            .fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(items) { it ->
                var showReschedule by remember { mutableStateOf(false) }
                var newDate by remember { mutableStateOf("") }
                var newTime by remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()
                AppointmentCard(it, onClick = {
                    com.example.xinqiao.ui.Navigation.toAppointmentDetail(ctx, it.consultantId, it.name, it.mode)
                }, onCancel = {
                    if (it.id > 0) {
                        scope.launch {
                            val ok = com.example.xinqiao.user.UserAppointmentRepository(ctx).cancel(it.id).getOrDefault(false)
                            if (ok) {
                                val res = com.example.xinqiao.user.UserAppointmentRepository(ctx).listMine()
                                res.onSuccess { list ->
                                    items = list.map { a -> AppointmentItem(a.counselor, a.counselor, "", a.startTime.replace('T', ' '), when (a.status) {
                                        "PENDING" -> "待咨询"
                                        "APPROVED" -> "待咨询"
                                        "REJECTED" -> "已驳回"
                                        "COMPLETED" -> "已完成"
                                        "CANCELLED" -> "已取消"
                                        else -> a.status
                                    }, a.id) }
                                }
                            }
                        }
                    }
                }, onReschedule = {
                    showReschedule = true
                })
                if (showReschedule) {
                    androidx.compose.material3.AlertDialog(onDismissRequest = { showReschedule = false }, confirmButton = {
                        Button(onClick = {
                            if (it.id > 0 && newDate.isNotBlank() && newTime.isNotBlank()) {
                                scope.launch {
                                    val ok = com.example.xinqiao.user.UserAppointmentRepository(ctx).reschedule(it.id, newDate, newTime).getOrDefault(false)
                                    android.widget.Toast.makeText(ctx, if (ok) "改期成功" else "改期失败", android.widget.Toast.LENGTH_SHORT).show()
                                    if (ok) {
                                        val res = com.example.xinqiao.user.UserAppointmentRepository(ctx).listMine()
                                        res.onSuccess { list ->
                                            items = list.map { a -> AppointmentItem(a.counselor, a.counselor, "", a.startTime.replace('T', ' '), when (a.status) {
                                                "PENDING" -> "待咨询"
                                                "APPROVED" -> "待咨询"
                                                "REJECTED" -> "已驳回"
                                                "COMPLETED" -> "已完成"
                                                "CANCELLED" -> "已取消"
                                                else -> a.status
                                            }, a.id) }
                                        }
                                    }
                                    showReschedule = false
                                }
                            } else {
                                android.widget.Toast.makeText(ctx, "请先选择日期与时间", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("确认") }
                    }, dismissButton = { Button(onClick = { showReschedule = false }) { Text("取消") } }, title = { Text("改期") }, text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("当前选择：${if (newDate.isBlank()) "未选择" else newDate} ${if (newTime.isBlank()) "" else newTime}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val now = java.time.LocalDate.now()
                                    android.app.DatePickerDialog(
                                        ctx,
                                        { _, year, monthOfYear, dayOfMonth ->
                                            newDate = "%04d-%02d-%02d".format(year, monthOfYear + 1, dayOfMonth)
                                        },
                                        now.year,
                                        now.monthValue - 1,
                                        now.dayOfMonth
                                    ).show()
                                }) { Text("选择日期") }
                                Button(onClick = {
                                    val nowT = java.time.LocalTime.now()
                                    android.app.TimePickerDialog(
                                        ctx,
                                        { _, hourOfDay, minute ->
                                            newTime = "%02d:%02d".format(hourOfDay, minute)
                                        },
                                        nowT.hour,
                                        nowT.minute,
                                        true
                                    ).show()
                                }) { Text("选择时间") }
                            }
                        }
                    })
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private data class AppointmentItem(
    val consultantId: String,
    val name: String,
    val mode: String,
    val time: String,
    val status: String,
    val id: Long = -1
)

@Composable
private fun AppointmentCard(it: AppointmentItem, onClick: () -> Unit, onCancel: () -> Unit, onReschedule: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(it.name, fontWeight = FontWeight.SemiBold)
                Text("时间：${it.time}", color = Color(0xFF666666))
                Text("形式：${it.mode}", color = Color(0xFF666666))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusTag(it.status)
                if (it.status == "待咨询") {
                    Button(onClick = onCancel, content = { Text("取消") })
                    Button(onClick = onReschedule, content = { Text("改期") })
                }
            }
        }
    }
}

@Composable
private fun StatusTag(status: String) {
    val color = when (status) {
        "待咨询" -> Color(0xFF2F54EB)
        "已完成" -> Color(0xFF52C41A)
        "已取消" -> Color(0xFFFA8C16)
        else -> Color(0xFF999999)
    }
    AssistChip(onClick = {}, label = { Text(status, color = Color.White) }, colors = AssistChipDefaults.assistChipColors(containerColor = color))
}
