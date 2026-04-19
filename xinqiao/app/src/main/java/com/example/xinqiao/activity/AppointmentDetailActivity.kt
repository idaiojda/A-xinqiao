package com.example.xinqiao.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import com.example.xinqiao.consultation.pro.AppointmentDetailViewModel
import com.example.xinqiao.consultation.pro.SlotTime

class AppointmentDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val consultantId = intent.getStringExtra("consultantId") ?: ""
        val name = intent.getStringExtra("name") ?: "咨询师"
        val mode = intent.getStringExtra("mode") ?: "文字咨询"
        val price = intent.getIntExtra("price", 299)
        val priceText = intent.getIntExtra("priceText", 0)
        val priceVoice = intent.getIntExtra("priceVoice", 0)
        val priceVideo = intent.getIntExtra("priceVideo", 0)
        val duration = intent.getIntExtra("duration", 60)
        setContent {
            AppointmentDetailScreen(
                consultantId = consultantId,
                name = name,
                defaultMode = mode,
                price = price,
                priceText = priceText,
                priceVoice = priceVoice,
                priceVideo = priceVideo,
                duration = duration,
                onBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentDetailScreen(
    consultantId: String,
    name: String,
    defaultMode: String,
    price: Int,
    priceText: Int,
    priceVoice: Int,
    priceVideo: Int,
    duration: Int,
    onBack: () -> Unit
) {
    val themeColor = Color(0xFF5B7FFF)
    val ctx = LocalContext.current
    val vm: AppointmentDetailViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.init(consultantId, name, defaultMode, price, priceText, priceVoice, priceVideo, duration)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshLoginStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF333333),
                    navigationIconContentColor = Color(0xFF333333),
                ),
                title = { Text("预约详情", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } }
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                // 如果未选择时间，显示提示
                if (ui.selectedTime == null && !ui.loginRequired && ui.profileComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3E0))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "请选择预约时段",
                            fontSize = 13.sp,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                BottomBar(
                    price = ui.price,
                    enabled = !ui.loginRequired && ui.profileComplete && ui.selectedTime != null && !ui.submitting,
                    submitting = ui.submitting,
                    themeColor = themeColor,
                    onClick = {
                        vm.submit { ok, msg ->
                            if (ok) {
                                Toast.makeText(ctx, "预约成功", Toast.LENGTH_SHORT).show()
                                ctx.startActivity(Intent(ctx, MyAppointmentsActivity::class.java))
                            } else {
                                Toast.makeText(ctx, msg ?: "预约失败", Toast.LENGTH_SHORT).show()
                                if (!msg.isNullOrBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HeaderCard使用key强制重组
            key(ui.selectedMode) {
                HeaderCard(name = ui.consultantName, mode = ui.selectedMode)
            }
            ModeSection(current = ui.selectedMode, onSelect = vm::selectMode)
            DateSection(dates = ui.dates, selected = ui.selectedDate, onSelect = vm::selectDate)
            TimeSection(slots = ui.slots, selected = ui.selectedTime, loading = ui.loadingSlots, onSelect = vm::selectTime)
            PersonalInfoCard(
                nickname = ui.nickname,
                maskedPhone = ui.maskedPhone,
                missingFields = ui.missingFields,
                profileComplete = ui.profileComplete,
                loginRequired = ui.loginRequired,
                onGoLogin = { ctx.startActivity(Intent(ctx, LoginActivity::class.java)) },
                onGoEdit = { ctx.startActivity(Intent(ctx, UserInfoActivity::class.java)) }
            )
            RemarkCard(text = ui.remark, onChange = vm::updateRemark)
        }
        if (ui.error != null) {
            androidx.compose.runtime.LaunchedEffect(ui.error) {
                snackbarHostState.showSnackbar(ui.error ?: "加载失败")
            }
        }
    }
}

@Composable
private fun HeaderCard(name: String, mode: String) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形头像占位
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFFE6F0FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.toString() ?: "g",
                    color = Color(0xFF5B7FFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )
                // 强制使用remember确保更新
                val displayMode = remember(mode) { mode }
                Text(
                    text = "实付方式：$displayMode",
                    fontSize = 13.sp,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

@Composable
private fun ModeSection(current: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "咨询方式",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333)
        )
        val modes = listOf("文字咨询", "语音咨询", "视频咨询")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            modes.forEach { m ->
                val selected = current == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(if (selected) Color(0xFF5B7FFF) else Color.White)
                        .clickable { onSelect(m) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        m,
                        color = if (selected) Color.White else Color(0xFF666666),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSection(dates: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "选择日期",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 显示所有日期，用户可以横向滚动查看
            dates.forEach { d ->
                val sel = selected == d
                val parts = d.split("-")
                val month = if (parts.size >= 2) parts[1] else ""
                val day = if (parts.size >= 3) parts[2] else d
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(if (sel) Color(0xFF5B7FFF) else Color.White)
                        .clickable { onSelect(d) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${month}月",
                            fontSize = 11.sp,
                            color = if (sel) Color.White else Color(0xFF999999)
                        )
                        Text(
                            day,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sel) Color.White else Color(0xFF333333)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSection(slots: List<SlotTime>, selected: SlotTime?, loading: Boolean, onSelect: (SlotTime) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "选择时段",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333)
        )
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        
        // 只显示可预约的时间槽
        val availableSlots = slots.filter { it.available }
        
        if (availableSlots.isEmpty() && !loading) {
            Text(
                "当前日期暂无可预约时段",
                fontSize = 14.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                availableSlots.forEach { slot ->
                    val sel = selected?.start == slot.start
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                            .background(if (sel) Color(0xFF5B7FFF) else Color.White)
                            .clickable { onSelect(slot) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = slot.start,
                            color = if (sel) Color.White else Color(0xFF666666),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalInfoCard(
    nickname: String,
    maskedPhone: String,
    missingFields: String,
    profileComplete: Boolean,
    loginRequired: Boolean,
    onGoLogin: () -> Unit,
    onGoEdit: () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "个人信息",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333)
            )
            
            if (loginRequired) {
                Text("请先登录以继续预约", color = Color(0xFFFF4D4F), fontSize = 14.sp)
                Button(
                    onClick = onGoLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B7FFF))
                ) {
                    Text("去登录")
                }
            } else if (!profileComplete) {
                val hint = if (missingFields.isNotBlank()) missingFields else "昵称、手机号"
                Text("请完善个人资料（$hint）", color = Color(0xFFFF4D4F), fontSize = 14.sp)
                Button(
                    onClick = onGoEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B7FFF))
                ) {
                    Text("去完善")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material.Icon(
                            painter = androidx.compose.ui.res.painterResource(com.example.xinqiao.R.drawable.ic_person),
                            contentDescription = null,
                            tint = Color(0xFF999999),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("预约人：", fontSize = 14.sp, color = Color(0xFF999999))
                        Text(nickname, fontSize = 14.sp, color = Color(0xFF333333))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material.Icon(
                            painter = androidx.compose.ui.res.painterResource(com.example.xinqiao.R.drawable.ic_phone),
                            contentDescription = null,
                            tint = Color(0xFF999999),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("联系方式：", fontSize = 14.sp, color = Color(0xFF999999))
                        Text(maskedPhone, fontSize = 14.sp, color = Color(0xFF333333))
                    }
                }
            }
        }
    }
}

@Composable
private fun RemarkCard(text: String, onChange: (String) -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = onChange,
                label = { Text("备注需求（可选）") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5B7FFF),
                    unfocusedBorderColor = Color(0xFFDDDDDD)
                )
            )
        }
    }
}

@Composable
private fun BottomBar(price: Int, enabled: Boolean, submitting: Boolean, themeColor: Color, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "总计",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "¥$price",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B)
                    )
                }
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .height(44.dp)
                    .width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (enabled) themeColor else Color(0xFFCCCCCC),
                    disabledContainerColor = Color(0xFFCCCCCC)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "立即支付",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) Color.White else Color(0xFF999999)
                    )
                }
            }
        }
    }
}
