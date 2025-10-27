package com.example.xinqiao.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class ConsultantDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cid = intent.getStringExtra("consultantId") ?: ""
        setContent { ConsultantDetailScreen(cid, onBack = { finish() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsultantDetailScreen(consultantId: String, onBack: () -> Unit) {
    val themeColor = Color(0xFF2F54EB)
    Scaffold(topBar = {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
            containerColor = themeColor,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
        ), title = { Text("咨询师详情", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } })
    }) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(24.dp)) {
            Text("咨询师ID：$consultantId")
            Text("详细资质与案例占位，后续接入真实数据")
        }
    }
}