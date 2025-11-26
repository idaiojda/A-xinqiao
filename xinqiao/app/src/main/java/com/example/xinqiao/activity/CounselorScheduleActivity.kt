package com.example.xinqiao.activity

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xinqiao.counselor.CounselorScheduleRepository
import kotlinx.coroutines.launch

class CounselorScheduleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CounselorScheduleScreen(onBack = { finish() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounselorScheduleScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { CounselorScheduleRepository(ctx) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var rules by remember { mutableStateOf(listOf<com.example.xinqiao.counselor.ScheduleRuleDto>()) }
    var slots by remember { mutableStateOf(listOf<com.example.xinqiao.counselor.ScheduleSlotDto>()) }
    var genCount by remember { mutableStateOf(0) }
    var startDate by remember { mutableStateOf("2025-11-01") }
    var endDate by remember { mutableStateOf("2025-11-30") }
    var startTime by remember { mutableStateOf("09:00:00") }
    var endTime by remember { mutableStateOf("12:00:00") }
    var weekdays by remember { mutableStateOf("1,3,5") }
    var frequency by remember { mutableStateOf("WEEKLY") }
    var selectedRule by remember { mutableStateOf<com.example.xinqiao.counselor.ScheduleRuleDto?>(null) }
    var exceptionDate by remember { mutableStateOf("") }
    var exceptions by remember { mutableStateOf(listOf<com.example.xinqiao.counselor.ScheduleExceptionDto>()) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("排班管理") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "") } })
    }, snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("开始日期") })
                Button(onClick = {
                    val now = java.time.LocalDate.now()
                    android.app.DatePickerDialog(
                        ctx,
                        { _, year, monthOfYear, dayOfMonth -> startDate = "%04d-%02d-%02d".format(year, monthOfYear + 1, dayOfMonth) },
                        now.year, now.monthValue - 1, now.dayOfMonth
                    ).show()
                }) { Text("选择开始") }
                OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("结束日期") })
                Button(onClick = {
                    val now = java.time.LocalDate.now()
                    android.app.DatePickerDialog(
                        ctx,
                        { _, year, monthOfYear, dayOfMonth -> endDate = "%04d-%02d-%02d".format(year, monthOfYear + 1, dayOfMonth) },
                        now.year, now.monthValue - 1, now.dayOfMonth
                    ).show()
                }) { Text("选择结束") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("开始时间") })
                Button(onClick = {
                    val nowT = java.time.LocalTime.now()
                    android.app.TimePickerDialog(ctx, { _, h, m -> startTime = "%02d:%02d:%02d".format(h, m, 0) }, nowT.hour, nowT.minute, true).show()
                }) { Text("选择开始") }
                OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("结束时间") })
                Button(onClick = {
                    val nowT = java.time.LocalTime.now()
                    android.app.TimePickerDialog(ctx, { _, h, m -> endTime = "%02d:%02d:%02d".format(h, m, 0) }, nowT.hour, nowT.minute, true).show()
                }) { Text("选择结束") }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { frequency = "DAILY" }, label = { Text("每日") }, colors = AssistChipDefaults.assistChipColors(containerColor = if (frequency == "DAILY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface))
                    AssistChip(onClick = { frequency = "WEEKLY" }, label = { Text("每周") }, colors = AssistChipDefaults.assistChipColors(containerColor = if (frequency == "WEEKLY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface))
                }
                if (frequency == "WEEKLY") {
                    OutlinedTextField(value = weekdays, onValueChange = { weekdays = it }, label = { Text("周几") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        if (startDate.isBlank() || endDate.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                            snackbarHostState.showSnackbar("请完整选择日期与时间")
                        } else {
                            val w = if (frequency == "WEEKLY") weekdays.split(",").mapNotNull { it.trim().toIntOrNull() } else emptyList()
                            repo.createRule(frequency, startDate, endDate, startTime, endTime, w)
                            rules = repo.listRules().getOrDefault(emptyList())
                        }
                    }
                }) { Text("创建规则") }
                Button(onClick = { scope.launch { genCount = repo.generate(startDate, endDate).getOrDefault(0); slots = repo.listSlots(null, null).getOrDefault(emptyList()) } }) { Text("生成时段") }
                Button(onClick = { scope.launch { rules = repo.listRules().getOrDefault(emptyList()) } }) { Text("刷新规则") }
                Button(onClick = { scope.launch { slots = repo.listSlots(null, null).getOrDefault(emptyList()) } }) { Text("刷新时段") }
            }
            if (genCount > 0) Text("生成数量：$genCount", style = MaterialTheme.typography.bodyMedium)
            Text("规则", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                items(rules) { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("#${r.id} ${r.frequency} ${r.startDate}~${r.endDate} ${r.startTime}-${r.endTime} ${r.weekdays.joinToString(",")}")
                        Button(onClick = { selectedRule = r; scope.launch { exceptions = repo.listExceptions(r.id).getOrDefault(emptyList()) } }) { Text("例外") }
                        Button(onClick = { scope.launch { repo.deleteRule(r.id); rules = repo.listRules().getOrDefault(emptyList()); if (selectedRule?.id == r.id) { selectedRule = null; exceptions = emptyList() } } }) { Text("删除") }
                    }
                }
            }
            if (selectedRule != null) {
                Text("例外日期", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = exceptionDate, onValueChange = { exceptionDate = it }, label = { Text("YYYY-MM-DD") })
                    Button(onClick = { scope.launch { if (exceptionDate.isNotBlank()) { repo.addException(selectedRule!!.id, exceptionDate); exceptions = repo.listExceptions(selectedRule!!.id).getOrDefault(emptyList()); exceptionDate = "" } } }) { Text("添加例外") }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    items(exceptions) { e ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("#${e.id} ${e.date}")
                            Button(onClick = { scope.launch { repo.deleteException(e.id); exceptions = repo.listExceptions(selectedRule!!.id).getOrDefault(emptyList()) } }) { Text("删除") }
                        }
                    }
                }
            }
            Text("时段", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(slots) { s ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("#${s.id} ${s.startTime} - ${s.endTime} ${if (s.available) "可约" else "关闭"}")
                        if (s.available) Button(onClick = { scope.launch { repo.closeSlot(s.id); slots = repo.listSlots(null, null).getOrDefault(emptyList()) } }) { Text("关闭") }
                        if (!s.available) Button(onClick = { scope.launch { repo.openSlot(s.id); slots = repo.listSlots(null, null).getOrDefault(emptyList()) } }) { Text("开放") }
                    }
                }
            }
        }
    }
}