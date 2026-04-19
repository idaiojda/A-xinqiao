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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    val themeColor = Color(0xFF5B7FFF)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("全部", "待审核", "已批准", "已完成", "已取消")
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF333333),
                        navigationIconContentColor = Color(0xFF333333),
                    ),
                    title = { Text("我的预约", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = { 
                        IconButton(onClick = onBack) { 
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回") 
                        } 
                    }
                )
                // Tab栏
                androidx.compose.material3.TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = themeColor,
                    indicator = { tabPositions ->
                        androidx.compose.material3.TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = themeColor
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title,
                                    color = if (selectedTab == index) themeColor else Color(0xFF666666)
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val repo = remember { com.example.xinqiao.user.UserAppointmentRepository(ctx) }
        var allItems by remember { mutableStateOf(listOf<AppointmentItem>()) }
        
        LaunchedEffect(Unit) {
            android.util.Log.d("MyAppointments", "开始加载预约列表")
            val res = repo.listMine()
            android.util.Log.d("MyAppointments", "API调用结果: ${res.isSuccess}")
            res.onSuccess { list ->
                android.util.Log.d("MyAppointments", "获取到 ${list.size} 条预约记录")
                list.forEachIndexed { index, item ->
                    android.util.Log.d("MyAppointments", "预约[$index]: id=${item.id}, counselor=${item.counselor}, status=${item.status}, startTime=${item.startTime}")
                }
                allItems = list.map { AppointmentItem(
                    it.counselor, 
                    it.counselor, 
                    it.mode ?: "text",  // 使用API返回的mode字段
                    it.startTime.replace('T', ' '), 
                    when (it.status) {
                        "PENDING" -> "待审核"
                        "APPROVED" -> "已批准"
                        "REJECTED" -> "已驳回"
                        "COMPLETED" -> "已完成"
                        "CANCELLED" -> "已取消"
                        else -> it.status
                    }, 
                    it.id,
                    it.status  // 保存原始状态
                ) }
                android.util.Log.d("MyAppointments", "转换后的列表大小: ${allItems.size}")
            }.onFailure { error ->
                android.util.Log.e("MyAppointments", "加载预约失败", error)
            }
        }
        
        // 根据选中的tab过滤数据
        val items = remember(selectedTab, allItems) {
            when (selectedTab) {
                0 -> allItems // 全部
                1 -> allItems.filter { it.status == "待审核" } // 待审核
                2 -> allItems.filter { it.status == "已批准" } // 已批准
                3 -> allItems.filter { it.status == "已完成" } // 已完成
                4 -> allItems.filter { it.status == "已取消" } // 已取消
                else -> allItems
            }
        }
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)), 
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { it ->
                var showReschedule by remember { mutableStateOf(false) }
                var showCancelConfirm by remember { mutableStateOf(false) }
                var newDate by remember { mutableStateOf("") }
                var newTime by remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()
                
                AppointmentCard(it, onClick = {
                    com.example.xinqiao.ui.Navigation.toAppointmentDetail(ctx, it.consultantId, it.name, it.mode)
                }, onCancel = {
                    showCancelConfirm = true
                }, onReschedule = {
                    showReschedule = true
                })
                
                // 取消预约确认对话框
                if (showCancelConfirm) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showCancelConfirm = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (it.id > 0) {
                                        scope.launch {
                                            android.util.Log.d("MyAppointments", "开始取消预约: id=${it.id}")
                                            try {
                                                val result = com.example.xinqiao.user.UserAppointmentRepository(ctx).cancel(it.id)
                                                val (ok, msg) = result.getOrDefault(Pair(false, "取消失败"))
                                                android.util.Log.d("MyAppointments", "取消预约结果: ok=$ok, msg=$msg")
                                                
                                                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                                                
                                                if (ok) {
                                                    // 刷新列表
                                                    val res = com.example.xinqiao.user.UserAppointmentRepository(ctx).listMine()
                                                    res.onSuccess { list ->
                                                        allItems = list.map { a -> AppointmentItem(
                                                            a.counselor, 
                                                            a.counselor, 
                                                            a.mode ?: "text",
                                                            a.startTime.replace('T', ' '), 
                                                            when (a.status) {
                                                                "PENDING" -> "待审核"
                                                                "APPROVED" -> "已批准"
                                                                "REJECTED" -> "已驳回"
                                                                "COMPLETED" -> "已完成"
                                                                "CANCELLED" -> "已取消"
                                                                else -> a.status
                                                            }, 
                                                            a.id,
                                                            a.status  // 保存原始状态
                                                        ) }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("MyAppointments", "取消预约异常", e)
                                                android.widget.Toast.makeText(ctx, "取消预约失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                            showCancelConfirm = false
                                        }
                                    } else {
                                        android.widget.Toast.makeText(ctx, "无效的预约ID", android.widget.Toast.LENGTH_SHORT).show()
                                        showCancelConfirm = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF4444)
                                )
                            ) {
                                Text("确认取消")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showCancelConfirm = false }) {
                                Text("我再想想")
                            }
                        },
                        title = { Text("取消预约") },
                        text = {
                            Column {
                                Text("确定要取消这个预约吗？")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "咨询师：${it.name}\n时间：${it.time}\n状态：${it.status}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                                if (it.status != "待咨询") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "注意：只能取消待咨询状态的预约",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFF4444)
                                    )
                                }
                            }
                        }
                    )
                }
                
                // 改期对话框
                if (showReschedule) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showReschedule = false },
                        confirmButton = {
                            Button(onClick = {
                                if (it.id > 0 && newDate.isNotBlank() && newTime.isNotBlank()) {
                                    scope.launch {
                                        android.util.Log.d("MyAppointments", "开始改期: id=${it.id}, date=$newDate, time=$newTime")
                                        try {
                                            val result = com.example.xinqiao.user.UserAppointmentRepository(ctx).reschedule(it.id, newDate, newTime)
                                            val (ok, msg) = result.getOrDefault(Pair(false, "改期失败"))
                                            android.util.Log.d("MyAppointments", "改期结果: ok=$ok, msg=$msg")
                                            
                                            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                                            
                                            if (ok) {
                                                val res = com.example.xinqiao.user.UserAppointmentRepository(ctx).listMine()
                                                res.onSuccess { list ->
                                                    allItems = list.map { a -> AppointmentItem(
                                                        a.counselor, 
                                                        a.counselor, 
                                                        a.mode ?: "text",
                                                        a.startTime.replace('T', ' '), 
                                                        when (a.status) {
                                                            "PENDING" -> "待审核"
                                                            "APPROVED" -> "已批准"
                                                            "REJECTED" -> "已驳回"
                                                            "COMPLETED" -> "已完成"
                                                            "CANCELLED" -> "已取消"
                                                            else -> a.status
                                                        }, 
                                                        a.id,
                                                        a.status  // 保存原始状态
                                                    ) }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("MyAppointments", "改期异常", e)
                                            android.widget.Toast.makeText(ctx, "改期失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        showReschedule = false
                                    }
                                } else {
                                    android.widget.Toast.makeText(ctx, "请先选择日期与时间", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("确认改期") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showReschedule = false }) {
                                Text("取消")
                            }
                        },
                        title = { Text("改期") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("原预约时间：${it.time}", fontSize = 14.sp, color = Color(0xFF666666))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "新预约时间：${if (newDate.isBlank()) "未选择" else "$newDate ${if (newTime.isBlank()) "" else newTime}"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (newDate.isNotBlank() && newTime.isNotBlank()) Color(0xFF5B7FFF) else Color(0xFF999999)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
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
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (newDate.isBlank()) Color(0xFF5B7FFF) else Color(0xFF00C48C)
                                        )
                                    ) {
                                        Text(if (newDate.isBlank()) "选择日期" else "已选日期")
                                    }
                                    Button(
                                        onClick = {
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
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (newTime.isBlank()) Color(0xFF5B7FFF) else Color(0xFF00C48C)
                                        )
                                    ) {
                                        Text(if (newTime.isBlank()) "选择时间" else "已选时间")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class AppointmentItem(
    val consultantId: String,
    val name: String,
    val mode: String,
    val time: String,
    val status: String,  // 显示用的状态（中文）
    val id: Long = -1,
    val rawStatus: String = ""  // 原始状态（英文，用于判断是否已批准）
)

@Composable
private fun AppointmentCard(it: AppointmentItem, onClick: () -> Unit, onCancel: () -> Unit, onReschedule: () -> Unit) {
    val ctx = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp), 
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            // 顶部：ID和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 圆形ID标识
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE6F0FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = it.id.toString(),
                            color = Color(0xFF5B7FFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        it.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )
                }
                StatusTag(it.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 时间信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material.Icon(
                    painter = androidx.compose.ui.res.painterResource(com.example.xinqiao.R.drawable.ic_clock),
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "时间：${it.time}",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 咨询方式
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material.Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        when {
                            it.mode.contains("视频") -> com.example.xinqiao.R.drawable.ic_video
                            it.mode.contains("语音") -> com.example.xinqiao.R.drawable.ic_phone
                            else -> com.example.xinqiao.R.drawable.ic_message
                        }
                    ),
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "形式：${formatMode(it.mode)}",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            
            // 操作按钮
            // 待审核和已批准状态都可以取消和改期
            if (it.status == "待审核" || it.status == "已批准") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF999999)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
                    ) {
                        Text("取消预约", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onReschedule,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF5B7FFF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5B7FFF))
                    ) {
                        Text("改期", fontSize = 14.sp)
                    }
                    
                    // 只有当预约被批准（APPROVED）时才显示"进入咨询室"按钮
                    if (it.rawStatus == "APPROVED") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                com.example.xinqiao.ui.Navigation.toConsultationRoom(
                                    ctx = ctx,
                                    chatId = it.id.toString(),
                                    targetName = it.name,
                                    appointmentType = it.mode
                                )
                            },
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00C48C)
                            )
                        ) {
                            Text("进入咨询室", fontSize = 14.sp)
                        }
                    }
                }
            } else if (it.status == "已完成") {
                // 已完成状态不显示按钮或显示评价按钮
            }
        }
    }
}

@Composable
private fun StatusTag(status: String) {
    val (bgColor, textColor) = when (status) {
        "待审核" -> Color(0xFFFFF4E6) to Color(0xFFFF9800)  // 橙色
        "已批准" -> Color(0xFFE6F0FF) to Color(0xFF5B7FFF)  // 蓝色
        "已完成" -> Color(0xFFE6F7F0) to Color(0xFF00C48C)  // 绿色
        "已取消" -> Color(0xFFF5F5F5) to Color(0xFF999999)  // 灰色
        "已驳回" -> Color(0xFFFFEBEE) to Color(0xFFFF4444)  // 红色
        else -> Color(0xFFF5F5F5) to Color(0xFF999999)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            status, 
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// 将英文mode转换为中文显示
private fun formatMode(mode: String): String {
    return when (mode.lowercase()) {
        "text" -> "文字咨询"
        "voice" -> "语音咨询"
        "video" -> "视频咨询"
        else -> mode // 如果已经是中文或其他值，直接返回
    }
}
