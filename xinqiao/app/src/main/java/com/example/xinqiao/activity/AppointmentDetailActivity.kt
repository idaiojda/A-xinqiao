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
        val duration = intent.getIntExtra("duration", 60)
        setContent {
            AppointmentDetailScreen(
                consultantId = consultantId,
                name = name,
                defaultMode = mode,
                price = price,
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
    duration: Int,
    onBack: () -> Unit
) {
    val themeColor = Color(0xFF2F54EB)
    val ctx = LocalContext.current
    val vm: AppointmentDetailViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.init(consultantId, name, defaultMode, price, duration)
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
                    containerColor = themeColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
                title = { Text("预约详情", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } }
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderSection(name = ui.consultantName, mode = ui.defaultMode)
            ModeSelector(current = ui.selectedMode, onSelect = vm::selectMode)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DateSelector(dates = ui.dates, selected = ui.selectedDate, onSelect = vm::selectDate)
                Button(onClick = {
                    val now = java.time.LocalDate.now()
                    android.app.DatePickerDialog(
                        ctx,
                        { _, year, monthOfYear, dayOfMonth ->
                            val chosen = "%04d-%02d-%02d".format(year, monthOfYear + 1, dayOfMonth)
                            vm.selectDate(chosen)
                        },
                        now.year,
                        now.monthValue - 1,
                        now.dayOfMonth
                    ).show()
                }) { Text("选择日期") }
                Button(onClick = { vm.reloadSlots() }) { Text("刷新时段") }
            }
            SlotsRow(slots = ui.slots, selected = ui.selectedTime, loading = ui.loadingSlots, onSelect = vm::selectTime)
            PriceSection(price = ui.price)
            RemarkSection(text = ui.remark, onChange = vm::updateRemark)
            ProfileSection(
                nickname = ui.nickname,
                maskedPhone = ui.maskedPhone,
                missingFields = ui.missingFields,
                profileComplete = ui.profileComplete,
                loginRequired = ui.loginRequired,
                onGoLogin = { ctx.startActivity(Intent(ctx, LoginActivity::class.java)) },
                onGoEdit = { ctx.startActivity(Intent(ctx, UserInfoActivity::class.java)) }
            )
            ConfirmButton(
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
        if (ui.error != null) {
            androidx.compose.runtime.LaunchedEffect(ui.error) {
                snackbarHostState.showSnackbar(ui.error ?: "加载失败")
            }
        }
    }
}

@Composable
private fun HeaderSection(name: String, mode: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text = "默认咨询形式：$mode", color = Color(0xFF666666), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ModeSelector(current: String, onSelect: (String) -> Unit) {
    val modes = listOf("文字咨询", "语音咨询", "视频咨询")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { m ->
            val selected = current == m
            FilterChip(
                selected = selected,
                onClick = { onSelect(m) },
                label = { Text(m) }
            )
        }
    }
}

@Composable
private fun DateSelector(dates: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dates.forEach { d ->
            val sel = selected == d
            AssistChip(
                onClick = { onSelect(d) },
                label = { Text(d) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (sel) Color(0xFFE6F4FF) else Color(0xFFF5F5F5),
                    labelColor = if (sel) Color(0xFF1677FF) else Color(0xFF333333)
                )
            )
        }
    }
}

@Composable
private fun SlotsRow(slots: List<SlotTime>, selected: SlotTime?, loading: Boolean, onSelect: (SlotTime) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("选择时间段")
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slots.forEach { slot ->
                val sel = selected?.start == slot.start
                val bg = when {
                    !slot.available -> Color(0xFFF0F0F0)
                    sel -> Color(0xFFE6F4FF)
                    else -> Color(0xFFF5F5F5)
                }
                val labelColor = when {
                    !slot.available -> Color(0xFF999999)
                    sel -> Color(0xFF1677FF)
                    else -> Color(0xFF333333)
                }
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(bg)
                        .clickable(enabled = slot.available) { onSelect(slot) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = slot.start, color = labelColor, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun PriceSection(price: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("价格：¥$price", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RemarkSection(text: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text("备注（可选）") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ProfileSection(
    nickname: String,
    maskedPhone: String,
    missingFields: String,
    profileComplete: Boolean,
    loginRequired: Boolean,
    onGoLogin: () -> Unit,
    onGoEdit: () -> Unit
) {
    val warnColor = Color(0xFFCC0000)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (loginRequired) {
            Text("请先登录以继续预约", color = warnColor)
            Button(onClick = onGoLogin) { Text("去登录") }
        } else if (!profileComplete) {
            val hint = if (missingFields.isNotBlank()) missingFields else "昵称、手机号"
            Text("请完善个人资料（$hint）", color = warnColor)
            Button(onClick = onGoEdit) { Text("去完善") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("昵称：$nickname")
                Text("手机号：$maskedPhone")
            }
        }
    }
}

@Composable
private fun ConfirmButton(enabled: Boolean, submitting: Boolean, themeColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        if (submitting) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text("确认预约", color = Color.White)
    }
}
