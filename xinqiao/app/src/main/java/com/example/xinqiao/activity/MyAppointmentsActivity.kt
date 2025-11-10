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
        val items = remember {
            listOf(
                AppointmentItem("A1001", "王心怡", "文字咨询", "2024-10-20 14:00", "待咨询"),
                AppointmentItem("A1002", "李可", "语音咨询", "2024-09-08 19:30", "已完成"),
                AppointmentItem("A1003", "周行", "视频咨询", "2024-08-11 10:00", "已取消")
            )
        }
        LazyColumn(modifier = Modifier
            .padding(padding)
            .fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(items) { it ->
                AppointmentCard(it, onClick = {
                    val intent = Intent(ctx, AppointmentDetailActivity::class.java)
                    intent.putExtra("consultantId", it.consultantId)
                    intent.putExtra("name", it.name)
                    intent.putExtra("mode", it.mode)
                    ctx.startActivity(intent)
                })
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
    val status: String
)

@Composable
private fun AppointmentCard(it: AppointmentItem, onClick: () -> Unit) {
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
            StatusTag(it.status)
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
