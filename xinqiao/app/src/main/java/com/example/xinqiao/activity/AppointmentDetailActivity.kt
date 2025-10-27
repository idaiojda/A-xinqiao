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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class AppointmentDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val consultantId = intent.getStringExtra("consultantId") ?: ""
        val name = intent.getStringExtra("name") ?: "咨询师"
        val mode = intent.getStringExtra("mode") ?: "文字咨询"
        setContent { AppointmentDetailScreen(consultantId, name, mode, onBack = { finish() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentDetailScreen(consultantId: String, name: String, mode: String, onBack: () -> Unit) {
    val themeColor = Color(0xFF2F54EB)
    val ctx = LocalContext.current
    var needProfile by remember { mutableStateOf(!isProfileComplete(ctx)) }
    Scaffold(topBar = {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
            containerColor = themeColor,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
        ), title = { Text("预约详情", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } })
    }) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(24.dp)) {
            Text("咨询师：$name ($consultantId)")
            Text("默认咨询形式：$mode")
            Spacer(modifier = Modifier.height(12.dp))
            if (needProfile) {
                Text("请完善个人信息后再预约", color = Color(0xFFCC0000))
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    ctx.startActivity(Intent(ctx, UserInfoActivity::class.java))
                }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                    Text("去完善", color = Color.White)
                }
            } else {
                Button(onClick = { /* TODO: 提交预约 */ }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                    Text("确认预约", color = Color.White)
                }
            }
        }
    }
}

private fun isProfileComplete(ctx: android.content.Context): Boolean {
    // 简化校验：从 SharedPreferences 读取标记，真实项目应查询 Room 中的用户信息
    val sp = ctx.getSharedPreferences("user_profile", android.content.Context.MODE_PRIVATE)
    return sp.getBoolean("complete", false)
}