package com.example.xinqiao.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xinqiao.counselor.CounselorScheduleRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CounselorScheduleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { 
            MaterialTheme {
                CounselorScheduleScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounselorScheduleScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { CounselorScheduleRepository(ctx) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var rules by remember { mutableStateOf(listOf<com.example.xinqiao.counselor.ScheduleRuleDto>()) }
    var slots by remember { mutableStateOf(listOf<com.example.xinqiao.counselor.ScheduleSlotDto>()) }
    var startDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_DATE)) }
    var startTime by remember { mutableStateOf("09:00:00") }
    var endTime by remember { mutableStateOf("18:00:00") }
    var showInstructions by remember { mutableStateOf(true) }
    
    // 加载数据
    LaunchedEffect(Unit) {
        rules = repo.listRules().getOrDefault(emptyList())
        slots = repo.listSlots(null, null).getOrDefault(emptyList())
        // 如果已有数据，不显示说明
        if (rules.isNotEmpty() || slots.isNotEmpty()) {
            showInstructions = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("排班管理", fontSize = 20.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // 使用说明（首次使用时显示）
            if (showInstructions) {
                item {
                    InstructionsCard(onDismiss = { showInstructions = false })
                }
            }
            
            // 快速设置工作时间
            item {
                QuickScheduleCard(
                    startDate = startDate,
                    endDate = endDate,
                    startTime = startTime,
                    endTime = endTime,
                    onStartDateClick = {
                        val date = LocalDate.parse(startDate)
                        android.app.DatePickerDialog(
                            ctx,
                            { _, year, month, day ->
                                startDate = "%04d-%02d-%02d".format(year, month + 1, day)
                            },
                            date.year, date.monthValue - 1, date.dayOfMonth
                        ).show()
                    },
                    onEndDateClick = {
                        val date = LocalDate.parse(endDate)
                        android.app.DatePickerDialog(
                            ctx,
                            { _, year, month, day ->
                                endDate = "%04d-%02d-%02d".format(year, month + 1, day)
                            },
                            date.year, date.monthValue - 1, date.dayOfMonth
                        ).show()
                    },
                    onStartTimeClick = {
                        val time = LocalTime.parse(startTime)
                        android.app.TimePickerDialog(
                            ctx,
                            { _, hour, minute ->
                                startTime = "%02d:%02d:00".format(hour, minute)
                            },
                            time.hour, time.minute, true
                        ).show()
                    },
                    onEndTimeClick = {
                        val time = LocalTime.parse(endTime)
                        android.app.TimePickerDialog(
                            ctx,
                            { _, hour, minute ->
                                endTime = "%02d:%02d:00".format(hour, minute)
                            },
                            time.hour, time.minute, true
                        ).show()
                    },
                    onQuickSetup = {
                        scope.launch {
                            try {
                                // 1. 创建规则
                                repo.createRule(
                                    frequency = "DAILY",
                                    startDate = startDate,
                                    endDate = endDate,
                                    startTime = startTime,
                                    endTime = endTime,
                                    weekdays = emptyList()
                                )
                                // 2. 生成时段
                                val count = repo.generate(startDate, endDate).getOrDefault(0)
                                // 3. 刷新数据
                                rules = repo.listRules().getOrDefault(emptyList())
                                slots = repo.listSlots(null, null).getOrDefault(emptyList())
                                snackbarHostState.showSnackbar("成功设置排班！生成了 $count 个时段")
                                showInstructions = false
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("设置失败: ${e.message}")
                            }
                        }
                    }
                )
            }
            
            // 我的排班规则
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "我的排班规则",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        scope.launch {
                            rules = repo.listRules().getOrDefault(emptyList())
                            slots = repo.listSlots(null, null).getOrDefault(emptyList())
                        }
                    }) {
                        Text("刷新")
                    }
                }
            }
            
            if (rules.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "还没有设置排班规则\n请使用上方的快速设置来创建您的工作时间"
                    )
                }
            } else {
                items(rules) { rule ->
                    ImprovedRuleCard(
                        rule = rule,
                        onDelete = {
                            scope.launch {
                                repo.deleteRule(rule.id)
                                rules = repo.listRules().getOrDefault(emptyList())
                                slots = repo.listSlots(null, null).getOrDefault(emptyList())
                                snackbarHostState.showSnackbar("已删除规则")
                            }
                        }
                    )
                }
            }
            
            // 可预约时段
            item {
                Text(
                    text = "可预约时段 (${slots.count { it.available }} 个可用)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            if (slots.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "还没有生成时段\n创建规则后，点击上方的\"一键设置排班\"按钮生成时段"
                    )
                }
            } else {
                // 按日期分组显示时段
                val slotsByDate = slots.groupBy { 
                    it.startTime.substring(0, 10) 
                }
                slotsByDate.forEach { (date, dateSlots) ->
                    item {
                        DateGroupHeader(date = date, count = dateSlots.size)
                    }
                    items(dateSlots) { slot ->
                        ImprovedSlotCard(
                            slot = slot,
                            onToggle = {
                                scope.launch {
                                    if (slot.available) {
                                        repo.closeSlot(slot.id)
                                    } else {
                                        repo.openSlot(slot.id)
                                    }
                                    slots = repo.listSlots(null, null).getOrDefault(emptyList())
                                }
                            }
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun InstructionsCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 如何设置排班？",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onDismiss) {
                    Text("知道了", fontSize = 12.sp)
                }
            }
            
            Text(
                text = "1️⃣ 选择工作日期范围（例如：本周或下周）",
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Text(
                text = "2️⃣ 设置每天的工作时间（例如：9:00 - 18:00）",
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Text(
                text = "3️⃣ 点击\"一键设置排班\"按钮，系统会自动生成可预约时段",
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Text(
                text = "4️⃣ 用户就可以在这些时段预约您的咨询服务了",
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun QuickScheduleCard(
    startDate: String,
    endDate: String,
    startTime: String,
    endTime: String,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onQuickSetup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚡ 快速设置工作时间",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            // 日期范围
            Text(
                text = "工作日期",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onStartDateClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("开始", fontSize = 12.sp)
                        Text(startDate, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "至",
                    modifier = Modifier.align(Alignment.CenterVertically),
                    fontSize = 16.sp
                )
                OutlinedButton(
                    onClick = onEndDateClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("结束", fontSize = 12.sp)
                        Text(endDate, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // 工作时间
            Text(
                text = "每天工作时间",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onStartTimeClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("上班", fontSize = 12.sp)
                        Text(startTime.substring(0, 5), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "至",
                    modifier = Modifier.align(Alignment.CenterVertically),
                    fontSize = 16.sp
                )
                OutlinedButton(
                    onClick = onEndTimeClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("下班", fontSize = 12.sp)
                        Text(endTime.substring(0, 5), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // 一键设置按钮
            Button(
                onClick = onQuickSetup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    text = "✨ 一键设置排班",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ImprovedRuleCard(
    rule: com.example.xinqiao.counselor.ScheduleRuleDto,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📋 ${rule.startDate} 至 ${rule.endDate}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⏰ 每天 ${rule.startTime.substring(0, 5)} - ${rule.endTime.substring(0, 5)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                if (rule.weekdays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📅 仅周 ${rule.weekdays.joinToString(", ")}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除规则",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DateGroupHeader(date: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📅 $date",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count 个时段",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ImprovedSlotCard(
    slot: com.example.xinqiao.counselor.ScheduleSlotDto,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (slot.available) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (slot.available) "✅" else "🔒",
                    fontSize = 24.sp
                )
                Column {
                    Text(
                        text = "${slot.startTime.substring(11, 16)} - ${slot.endTime.substring(11, 16)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (slot.available) "用户可以预约" else "已关闭预约",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (slot.available)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    if (slot.available) "关闭" else "开放",
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}