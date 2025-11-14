package com.example.xinqiao.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.xinqiao.R
import com.example.xinqiao.bean.TestReportItem
import com.example.xinqiao.repository.MedicalRecordRepository
import com.example.xinqiao.room.entities.TestReportEntity
import com.example.xinqiao.utils.AnalysisUtils
import com.example.xinqiao.util.CryptoUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class TestReportListFragmentNew : Fragment() {
    
    private lateinit var repo: MedicalRecordRepository
    private var userName: String = ""
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        repo = MedicalRecordRepository(requireContext())
        userName = AnalysisUtils.readLoginUserName(requireContext()) ?: ""
        
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    TestReportsScreen()
                }
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TestReportsScreen() {
        var testReports by remember { mutableStateOf<List<TestReportItem>>(emptyList()) }
        var filteredReports by remember { mutableStateOf<List<TestReportItem>>(emptyList()) }
        var sortByScore by remember { mutableStateOf(false) }
        var dateRange by remember { mutableStateOf<DateRange?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var showCharts by remember { mutableStateOf(true) }
        var showDatePicker by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(true) }
        
        // Load data
        LaunchedEffect(Unit) {
            loadTestReports { reports ->
                testReports = reports
                filteredReports = reports
                isLoading = false
            }
        }
        
        // Apply filters and sorting
        LaunchedEffect(sortByScore, dateRange, searchQuery) {
            filteredReports = applyFiltersAndSort(testReports, sortByScore, dateRange, searchQuery)
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            Column {
                // Top App Bar
                TopAppBar(
                    title = { 
                        Text(
                            "测评报告",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    actions = {
                        IconButton(onClick = { showCharts = !showCharts }) {
                            Icon(
                                imageVector = if (showCharts) Icons.Default.BarChart else Icons.Default.HideImage,
                                contentDescription = if (showCharts) "隐藏图表" else "显示图表"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
                
                when {
                    isLoading -> {
                        LoadingTestReports()
                    }
                    testReports.isEmpty() -> {
                        EmptyTestReports()
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Charts Section
                            if (showCharts) {
                                ChartsSection(filteredReports)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            // Search and Filter Section
                            SearchAndFilterSection(
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                sortByScore = sortByScore,
                                onSortChange = { sortByScore = it },
                                dateRange = dateRange,
                                onDateRangeChange = { dateRange = it },
                                onDatePickerShow = { showDatePicker = true }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Results Summary
                            ResultsSummary(filteredReports.size, testReports.size)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Test Report List
                            TestReportList(filteredReports)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
            
            // Date Picker Dialog
            if (showDatePicker) {
                DateRangePickerDialog(
                    currentRange = dateRange,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { newRange ->
                        dateRange = newRange
                        showDatePicker = false
                    }
                )
            }
        }
    }
    
    @Composable
    fun ChartsSection(reports: List<TestReportItem>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Score Trend Chart
            ScoreTrendChart(reports)
            
            // Risk Distribution Chart
            RiskDistributionChart(reports)
        }
    }
    
    @Composable
    fun ScoreTrendChart(reports: List<TestReportItem>) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF3F51B5).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF3F51B5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "分数趋势",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Text(
                        text = "${reports.size} 次测评",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (reports.size < 2) {
                    EmptyChart("需要至少2次测评数据")
                } else {
                    SimpleScoreTrend(reports)
                }
            }
        }
    }
    
    @Composable
    fun RiskDistributionChart(reports: List<TestReportItem>) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFFF6B6B).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "风险分布",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (reports.isEmpty()) {
                    EmptyChart("暂无测评数据")
                } else {
                    SimpleRiskDistribution(reports)
                }
            }
        }
    }
    
    @Composable
    fun EmptyChart(message: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
    
    @Composable
    fun SimpleScoreTrend(reports: List<TestReportItem>) {
        val groupedByDate = reports.groupBy { it.date }
            .mapValues { entry -> entry.value.map { it.score }.average() }
            .toList()
            .sortedBy { it.first }
            .takeLast(10) // Show last 10 data points
        
        if (groupedByDate.isEmpty()) {
            EmptyChart("需要至少2次测评数据")
            return
        }
        
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                groupedByDate.forEachIndexed { index, (date, avgScore) ->
                    val normalizedScore = (avgScore / 100.0).coerceIn(0.0, 1.0)
                    val barHeight by animateFloatAsState(
                        targetValue = (normalizedScore * 100).toFloat(),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "score_bar_$index"
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(barHeight.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF3F51B5),
                                            Color(0xFF3F51B5).copy(alpha = 0.6f)
                                        )
                                    ),
                                    RoundedCornerShape(10.dp, 10.dp, 4.dp, 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = date.takeLast(5),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "平均分: ${"%.1f".format(reports.map { it.score }.average())}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
    
    @Composable
    fun SimpleRiskDistribution(reports: List<TestReportItem>) {
        val riskCounts = reports.groupingBy { it.riskLevel }.eachCount()
        val total = reports.size
        
        val riskLevels = listOf(
            "low" to "低风险" to Color(0xFF10B981),
            "medium" to "中风险" to Color(0xFFF59E0B),
            "high" to "高风险" to Color(0xFFEF4444)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            riskLevels.forEach { (levelInfo, color) ->
                val (levelKey, levelName) = levelInfo
                val count = riskCounts[levelKey] ?: 0
                val percentage = if (total > 0) (count * 100 / total) else 0
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, CircleShape)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = levelName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "$count ($percentage%)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(2.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage / 100f)
                            .height(4.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
    
    @Composable
    fun SearchAndFilterSection(
        searchQuery: String,
        onSearchChange: (String) -> Unit,
        sortByScore: Boolean,
        onSortChange: (Boolean) -> Unit,
        dateRange: DateRange?,
        onDateRangeChange: (DateRange?) -> Unit,
        onDatePickerShow: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Sort Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "排序方式",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !sortByScore,
                            onClick = { onSortChange(false) },
                            label = { Text("按日期") }
                        )
                        
                        FilterChip(
                            selected = sortByScore,
                            onClick = { onSortChange(true) },
                            label = { Text("按分数") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Date Range Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "时间范围",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (dateRange != null) {
                            Text(
                                text = "${dateRange.startDate} 至 ${dateRange.endDate}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            
                            IconButton(
                                onClick = { onDateRangeChange(null) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "清除日期范围",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        Button(
                            onClick = onDatePickerShow,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (dateRange == null) "选择日期" else "修改日期",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchBar(
        query: String,
        onQueryChange: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = modifier
                .heightIn(min = 52.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                ),
            placeholder = {
                Text(
                    "搜索测评报告...",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除搜索",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onQueryChange(query) }
            )
        )
    }
    
    @Composable
    fun ResultsSummary(filteredCount: Int, totalCount: Int) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "筛选结果",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                
                Text(
                    text = "$filteredCount / $totalCount 份报告",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
    
    @Composable
    fun TestReportList(reports: List<TestReportItem>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            reports.forEachIndexed { index, report ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInHorizontally(
                        initialOffsetX = { it * (index % 2 + 1) },
                        animationSpec = tween(300, delayMillis = index * 50)
                    )
                ) {
                    TestReportCard(report)
                }
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TestReportCard(report: TestReportItem) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                ) { /* Handle card click */ },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isPressed) 6.dp else 2.dp,
                pressedElevation = 8.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    getScoreColor(report.score),
                                    getScoreColor(report.score).copy(alpha = 0.6f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${report.score.toInt()}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "分",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = report.type,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Risk Level Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = getRiskColor(report.riskLevel).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = getRiskLabel(report.riskLevel),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = getRiskColor(report.riskLevel),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Test details preview
                    Text(
                        text = report.details?.take(100) ?: "暂无详细说明",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bottom info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = report.date,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                        
                        // Score interpretation
                        Text(
                            text = getScoreInterpretation(report.score),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = getScoreColor(report.score),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Chevron
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "查看详情",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    @Composable
    fun DateRangePickerDialog(
        currentRange: DateRange?,
        onDismiss: () -> Unit,
        onConfirm: (DateRange) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择日期范围") },
            text = {
                Column {
                    Text(
                        "请选择要查看的测评报告时间范围",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick date range options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateRangeChip("最近7天", 7, currentRange, onConfirm)
                        DateRangeChip("最近30天", 30, currentRange, onConfirm)
                        DateRangeChip("最近90天", 90, currentRange, onConfirm)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
    
    @Composable
    fun DateRangeChip(
        label: String,
        days: Int,
        currentRange: DateRange?,
        onConfirm: (DateRange) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -days + 1)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        FilterChip(
            selected = currentRange?.let { it.startDate == startDate && it.endDate == endDate } ?: false,
            onClick = { onConfirm(DateRange(startDate, endDate)) },
            label = { Text(label) }
        )
    }
    
    @Composable
    fun LoadingTestReports() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "加载测评报告中...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
    
    @Composable
    fun EmptyTestReports() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无测评报告",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = "完成心理测评后，报告将显示在这里",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    private fun loadTestReports(callback: (List<TestReportItem>) -> Unit) {
        lifecycleScope.launch {
            try {
                val reports = withContext(Dispatchers.IO) {
                    // This would use the actual MedicalRecordRepository
                    // For now, we'll generate some sample data
                    generateSampleTestReports()
                }
                
                withContext(Dispatchers.Main) {
                    callback(reports)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(emptyList())
                }
            }
        }
    }
    
    private fun generateSampleTestReports(): List<TestReportItem> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val testTypes = listOf(
            "抑郁自评量表(SDS)",
            "焦虑自评量表(SAS)",
            "睡眠质量指数(PSQI)",
            "压力知觉量表(PSS)",
            "生活满意度量表(SWLS)"
        )
        
        return List(20) { index ->
            calendar.add(Calendar.DAY_OF_YEAR, -(index * 3))
            val date = dateFormat.format(calendar.time)
            val score = (20..95).random().toFloat()
            val riskLevel = when {
                score >= 70 -> "high"
                score >= 40 -> "medium"
                else -> "low"
            }
            
            TestReportItem().apply {
                id = index.toLong()
                reportId = "report_${1000 + index}"
                type = testTypes[index % testTypes.size]
                this.score = score
                this.riskLevel = riskLevel
                this.date = date
                details = "本次测评显示您的${type}得分为${score.toInt()}分，属于${getRiskLabel(riskLevel)}范围。建议根据测评结果进行相应的心理调适。"
            }
        }
    }
    
    private fun applyFiltersAndSort(
        reports: List<TestReportItem>,
        sortByScore: Boolean,
        dateRange: DateRange?,
        searchQuery: String
    ): List<TestReportItem> {
        var filtered = reports
        
        // Date range filter
        dateRange?.let { range ->
            filtered = filtered.filter { report ->
                report.date >= range.startDate && report.date <= range.endDate
            }
        }
        
        // Search filter
        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.trim().lowercase()
            val tokens = q.split(Regex("\\s+")).filter { it.isNotEmpty() }
            filtered = filtered.filter { report ->
                val haystack = listOfNotNull(report.type, report.details, report.reportId)
                    .joinToString(" ").lowercase()
                tokens.any { t -> haystack.contains(t) }
            }
        }
        
        // Sort
        return if (sortByScore) {
            filtered.sortedByDescending { it.score }
        } else {
            filtered.sortedByDescending { it.date }
        }
    }
    
    private fun getScoreColor(score: Float): Color {
        return when {
            score >= 80 -> Color(0xFF10B981) // Green - Excellent
            score >= 60 -> Color(0xFF3B82F6) // Blue - Good
            score >= 40 -> Color(0xFFF59E0B) // Orange - Fair
            else -> Color(0xFFEF4444) // Red - Poor
        }
    }
    
    private fun getRiskColor(riskLevel: String): Color {
        return when (riskLevel) {
            "low" -> Color(0xFF10B981) // Green
            "medium" -> Color(0xFFF59E0B) // Orange
            "high" -> Color(0xFFEF4444) // Red
            else -> Color(0xFF6B7280) // Gray
        }
    }
    
    private fun getRiskLabel(riskLevel: String): String {
        return when (riskLevel) {
            "low" -> "低风险"
            "medium" -> "中风险"
            "high" -> "高风险"
            else -> "未知"
        }
    }
    
    private fun getScoreInterpretation(score: Float): String {
        return when {
            score >= 80 -> "优秀"
            score >= 60 -> "良好"
            score >= 40 -> "一般"
            else -> "需关注"
        }
    }
    
    data class DateRange(
        val startDate: String,
        val endDate: String
    )
}
