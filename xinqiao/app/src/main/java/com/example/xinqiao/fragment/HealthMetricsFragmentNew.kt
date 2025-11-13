package com.example.xinqiao.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.fragment.app.Fragment
import com.example.xinqiao.bean.HealthMetricEntry
import com.example.xinqiao.util.MedicalRecordStorage
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.abs

class HealthMetricsFragmentNew : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    HealthMetricsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HealthMetricsScreen() {
    val viewModel = remember { HealthMetricsViewModel() }
    val metrics by viewModel.metrics.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val editingMetric by viewModel.editingMetric.collectAsState()
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadMetrics(context)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFE2E8F0)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with animated title
            AnimatedHeader()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Metrics distribution chart
            AnimatedMetricsChart(metrics)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Add metric button
            AnimatedAddButton {
                viewModel.showAddDialog()
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Metrics list
            AnimatedMetricsList(
                metrics = metrics,
                onEdit = { metric ->
                    viewModel.showEditDialog(metric)
                },
                onDelete = { metric ->
                    scope.launch {
                        viewModel.deleteMetric(context, metric)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
        
        // Floating action button
        AnimatedFab {
            viewModel.showAddDialog()
        }
    }
    
    // Dialogs
    AnimatedVisibility(
        visible = showAddDialog,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        AddMetricDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { type, value, unit ->
                scope.launch {
                    viewModel.addMetric(context, type, value, unit)
                }
            }
        )
    }
    
    AnimatedVisibility(
        visible = showEditDialog && editingMetric != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        EditMetricDialog(
            metric = editingMetric,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { type, value, unit ->
                scope.launch {
                    viewModel.updateMetric(context, editingMetric!!, type, value, unit)
                }
            }
        )
    }
}

@Composable
fun AnimatedHeader() {
    val animatedOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .offset(y = animatedOffset.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.MonitorHeart,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "健康指标",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            Text(
                text = "记录和追踪您的健康数据",
                fontSize = 16.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AnimatedMetricsChart(metrics: List<HealthMetricEntry>) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .alpha(animatedAlpha)
            .scale(animatedAlpha),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "指标类型分布",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "总计 ${metrics.size} 条",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
            
            if (metrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "暂无健康指标数据",
                            color = Color(0xFF9CA3AF),
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // Custom pie chart visualization
                MetricsDistributionChart(metrics)
            }
        }
    }
}

@Composable
fun MetricsDistributionChart(metrics: List<HealthMetricEntry>) {
    val typeCounts = metrics.groupBy { it.type }.mapValues { it.value.size }
    val total = metrics.size.toFloat()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            typeCounts.forEach { (type, count) ->
                val percentage = (count / total * 100).roundToInt()
                AnimatedMetricBar(
                    type = type,
                    percentage = percentage,
                    color = getMetricColor(type)
                )
            }
        }
    }
    
    // Legend
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        typeCounts.forEach { (type, count) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(getMetricColor(type), CircleShape)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = "$type (${count})",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
fun AnimatedMetricBar(type: String, percentage: Int, color: Color) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(120.dp)
                .background(
                    color.copy(alpha = 0.2f),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedPercentage / 100f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color,
                                color.copy(alpha = 0.7f)
                            )
                        ),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .align(Alignment.BottomCenter)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "$percentage%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151)
        )
        
        Text(
            text = type,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            maxLines = 1
        )
    }
}

@Composable
fun AnimatedAddButton(onClick: () -> Unit) {
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .scale(animatedScale),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3B82F6)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "添加健康指标",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedMetricsList(
    metrics: List<HealthMetricEntry>,
    onEdit: (HealthMetricEntry) -> Unit,
    onDelete: (HealthMetricEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "指标记录",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "${metrics.size} 条记录",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (metrics.isEmpty()) {
            EmptyMetricsState()
        } else {
            metrics.forEachIndexed { index, metric ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { it * (index + 1) }
                    ),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    MetricCard(
                        metric = metric,
                        onEdit = { onEdit(metric) },
                        onDelete = { onDelete(metric) }
                    )
                }
                
                if (index < metrics.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyMetricsState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "还没有健康指标记录",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
            
            Text(
                text = "点击上方按钮添加您的第一个健康指标",
                fontSize = 14.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricCard(
    metric: HealthMetricEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Metric type icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        getMetricColor(metric.type).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getMetricIcon(metric.type),
                    contentDescription = null,
                    tint = getMetricColor(metric.type),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Metric info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = metric.type,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = getMetricColor(metric.type).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${metric.value} ${metric.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = getMetricColor(metric.type),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Text(
                    text = metric.date,
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Action buttons (shown when expanded)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = Color(0xFF6B7280)
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedFab(onClick: () -> Unit) {
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.scale(animatedScale),
            containerColor = Color(0xFF3B82F6),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加指标",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMetricDialog(
    onDismiss: () -> Unit,
    onSave: (String, Float, String) -> Unit
) {
    var selectedType by remember { mutableStateOf("心率") }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    
    val types = listOf("心率", "血压", "体温", "体重", "血糖")
    val typeUnits = mapOf(
        "心率" to "次/分",
        "血压" to "mmHg",
        "体温" to "°C",
        "体重" to "kg",
        "血糖" to "mmol/L"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("新增健康指标")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type selection
                Text(
                    text = "指标类型",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                types.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Value input
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("数值") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Unit input (auto-filled but editable)
                OutlinedTextField(
                    value = unit.ifEmpty { typeUnits[selectedType] ?: "" },
                    onValueChange = { unit = it },
                    label = { Text("单位") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalUnit = unit.ifEmpty { typeUnits[selectedType] ?: "" }
                    val valueFloat = value.toFloatOrNull() ?: 0f
                    onSave(selectedType, valueFloat, finalUnit)
                },
                enabled = value.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMetricDialog(
    metric: HealthMetricEntry?,
    onDismiss: () -> Unit,
    onSave: (String, Float, String) -> Unit
) {
    if (metric == null) return
    
    var selectedType by remember { mutableStateOf(metric.type) }
    var value by remember { mutableStateOf(metric.value.toString()) }
    var unit by remember { mutableStateOf(metric.unit) }
    
    val types = listOf("心率", "血压", "体温", "体重", "血糖")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("编辑健康指标")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type selection
                Text(
                    text = "指标类型",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                types.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Value input
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("数值") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Unit input
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("单位") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val valueFloat = value.toFloatOrNull() ?: 0f
                    onSave(selectedType, valueFloat, unit)
                },
                enabled = value.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

fun getMetricColor(type: String): Color {
    return when (type) {
        "心率" -> Color(0xFF3B82F6) // Blue
        "血压" -> Color(0xFFEF4444) // Red
        "体温" -> Color(0xFFF59E0B) // Amber
        "体重" -> Color(0xFF8B5CF6) // Purple
        "血糖" -> Color(0xFF10B981) // Green
        else -> {
            val colors = listOf(
                Color(0xFF06B6D4), // Cyan
                Color(0xFFF97316), // Orange
                Color(0xFF22C55E), // Emerald
                Color(0xFFA855F7), // Violet
                Color(0xFFEAB308)  // Yellow
            )
            colors[abs(type.hashCode()) % colors.size]
        }
    }
}

fun getMetricIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        "心率" -> Icons.Default.Favorite
        "血压" -> Icons.Default.WaterDrop
        "体温" -> Icons.Default.DeviceThermostat
        "体重" -> Icons.Default.Scale
        "血糖" -> Icons.Default.Science
        else -> Icons.Default.MonitorHeart
    }
}

class HealthMetricsViewModel {
    private val _metrics = MutableStateFlow<List<HealthMetricEntry>>(emptyList())
    val metrics: StateFlow<List<HealthMetricEntry>> = _metrics.asStateFlow()
    
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()
    
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()
    
    private val _editingMetric = MutableStateFlow<HealthMetricEntry?>(null)
    val editingMetric: StateFlow<HealthMetricEntry?> = _editingMetric.asStateFlow()
    
    suspend fun loadMetrics(context: android.content.Context) {
        withContext(Dispatchers.IO) {
            val loaded = MedicalRecordStorage.loadHealthMetricEntries(context)
            _metrics.value = loaded ?: emptyList()
        }
    }
    
    suspend fun addMetric(context: android.content.Context, type: String, value: Float, unit: String) {
        withContext(Dispatchers.IO) {
            val newMetric = HealthMetricEntry(
                System.currentTimeMillis(),
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                type,
                value,
                unit
            )
            
            val current = _metrics.value.toMutableList()
            current.add(0, newMetric)
            _metrics.value = current
            
            MedicalRecordStorage.saveHealthMetricEntries(context, current)
            _showAddDialog.value = false
        }
    }
    
    suspend fun updateMetric(context: android.content.Context, metric: HealthMetricEntry, type: String, value: Float, unit: String) {
        withContext(Dispatchers.IO) {
            val updatedMetric = HealthMetricEntry(
                metric.id,
                metric.date,
                type,
                value,
                unit
            )
            
            val current = _metrics.value.toMutableList()
            val index = current.indexOfFirst { it.id == metric.id }
            if (index != -1) {
                current[index] = updatedMetric
                _metrics.value = current
                MedicalRecordStorage.saveHealthMetricEntries(context, current)
            }
            
            _showEditDialog.value = false
            _editingMetric.value = null
        }
    }
    
    suspend fun deleteMetric(context: android.content.Context, metric: HealthMetricEntry) {
        withContext(Dispatchers.IO) {
            val current = _metrics.value.toMutableList()
            current.remove(metric)
            _metrics.value = current
            MedicalRecordStorage.saveHealthMetricEntries(context, current)
        }
    }
    
    fun showAddDialog() {
        _showAddDialog.value = true
    }
    
    fun hideAddDialog() {
        _showAddDialog.value = false
    }
    
    fun showEditDialog(metric: HealthMetricEntry) {
        _editingMetric.value = metric
        _showEditDialog.value = true
    }
    
    fun hideEditDialog() {
        _showEditDialog.value = false
        _editingMetric.value = null
    }
}