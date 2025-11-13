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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.xinqiao.R
import com.example.xinqiao.repository.MedicalRecordRepository
import com.example.xinqiao.room.entities.EmotionDiaryEntity
import com.example.xinqiao.utils.AnalysisUtils
import com.github.mikephil.charting.charts.LineChart
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class MedicalRecordOverviewFragmentNew : Fragment() {
    
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
                    MedicalOverviewScreen()
                }
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MedicalOverviewScreen() {
        var selectedRange by remember { mutableStateOf(30) }
        var stats by remember { mutableStateOf<OverviewStats?>(null) }
        var emotionData by remember { mutableStateOf<List<EmotionDiaryEntity>>(emptyList()) }
        var hotTests by remember { mutableStateOf<List<HotTestItem>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        
        // Load data
        LaunchedEffect(Unit) {
            loadOverviewData { loadedStats, loadedHotTests, loadedEmotionData ->
                stats = loadedStats
                hotTests = loadedHotTests
                emotionData = loadedEmotionData
                isLoading = false
            }
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
            when {
                isLoading -> {
                    LoadingOverview()
                }
                stats == null -> {
                    EmptyOverview()
                }
                else -> {
                    AnimatedVisibility(
                        visible = !isLoading,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Header with welcome message
                            WelcomeHeader(stats!!.userName)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Statistics Cards with animations
                            AnimatedStatistics(stats!!)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Hot Tests Section
                            HotTestsSection(hotTests)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Emotion Trend Chart
                            EmotionTrendSection(
                                emotionData = emotionData,
                                selectedRange = selectedRange,
                                onRangeChange = { newRange ->
                                    selectedRange = newRange
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Quick Actions
                            QuickActionsSection()
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun WelcomeHeader(userName: String) {
        val animatedAlpha by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(1000, delayMillis = 200)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(animatedAlpha),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "你好，$userName",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "今天感觉怎么样？来看看你的健康档案吧",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
    
    @Composable
    fun AnimatedStatistics(stats: OverviewStats) {
        val animatedCompleted by animateIntAsState(
            targetValue = stats.completedCount,
            animationSpec = tween(1500, delayMillis = 300)
        )
        val animatedPending by animateIntAsState(
            targetValue = stats.pendingCount,
            animationSpec = tween(1500, delayMillis = 400)
        )
        val animatedUnfinished by animateIntAsState(
            targetValue = stats.unfinishedCount,
            animationSpec = tween(1500, delayMillis = 500)
        )
        val animatedSessions by animateIntAsState(
            targetValue = stats.sessionCount,
            animationSpec = tween(1500, delayMillis = 600)
        )
        val animatedMessages by animateIntAsState(
            targetValue = stats.messageCount,
            animationSpec = tween(1500, delayMillis = 700)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Assessment Statistics
            StatCard(
                title = "测评统计",
                icon = Icons.Default.Assignment,
                iconColor = Color(0xFF30B4FF),
                modifier = Modifier.animateContentSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("已完成", animatedCompleted, Color(0xFF10B981), "✅")
                    StatItem("待支付", animatedPending, Color(0xFFF59E0B), "⏳")
                    StatItem("未完成", animatedUnfinished, Color(0xFFEF4444), "❌")
                }
            }
            
            // Chat Statistics
            StatCard(
                title = "咨询统计",
                icon = Icons.Default.Chat,
                iconColor = Color(0xFF8B5CF6),
                modifier = Modifier.animateContentSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("会话数", animatedSessions, Color(0xFF8B5CF6), "💬")
                    StatItem("消息数", animatedMessages, Color(0xFF06B6D4), "📨")
                }
            }
        }
    }
    
    @Composable
    fun StatCard(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        iconColor: Color,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(iconColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                content()
            }
        }
    }
    
    @Composable
    fun RowScope.StatItem(
        label: String,
        value: Int,
        color: Color,
        emoji: String
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
    
    @Composable
    fun HotTestsSection(hotTests: List<HotTestItem>) {
        var expanded by remember { mutableStateOf(false) }
        
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
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "热门测评",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    if (hotTests.isNotEmpty()) {
                        TextButton(
                            onClick = { expanded = !expanded }
                        ) {
                            Text(
                                text = if (expanded) "收起" else "查看更多",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (hotTests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无热门测评数据",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                } else {
                    val displayItems = if (expanded) hotTests else hotTests.take(3)
                    
                    displayItems.forEachIndexed { index, item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInHorizontally(
                                initialOffsetX = { it * (index + 1) },
                                animationSpec = tween(300, delayMillis = index * 100)
                            )
                        ) {
                            HotTestItem(item, index + 1)
                            if (index < displayItems.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun HotTestItem(item: HotTestItem, rank: Int) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (rank) {
                    1 -> Color(0xFFFFD700).copy(alpha = 0.1f)
                    2 -> Color(0xFFC0C0C0).copy(alpha = 0.1f)
                    3 -> Color(0xFFCD7F32).copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            when (rank) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.count} 人已完成",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
    
    @Composable
    fun EmotionTrendSection(
        emotionData: List<EmotionDiaryEntity>,
        selectedRange: Int,
        onRangeChange: (Int) -> Unit
    ) {
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
                                .background(Color(0xFF30B4FF).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = Color(0xFF30B4FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "情绪趋势",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        listOf(7, 30).forEach { days ->
                            val isSelected = selectedRange == days
                            Button(
                                onClick = { onRangeChange(days) },
                                modifier = Modifier
                                    .height(28.dp)
                                    .padding(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                    contentColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    }
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "近${days}天",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (emotionData.isEmpty()) {
                    EmptyEmotionChart()
                } else {
                    EmotionTrendChart(emotionData, selectedRange)
                }
            }
        }
    }
    
    @Composable
    fun EmptyEmotionChart() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
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
                    imageVector = Icons.Default.Mood,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "暂无情绪记录",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = "开始记录你的情绪变化吧",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
    
    @Composable
    fun EmotionTrendChart(emotionData: List<EmotionDiaryEntity>, days: Int) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val now = java.util.Calendar.getInstance()
        val daysLabels = mutableListOf<String>()
        for (i in days - 1 downTo 0) {
            val c = java.util.Calendar.getInstance()
            c.timeInMillis = now.timeInMillis
            c.add(java.util.Calendar.DAY_OF_YEAR, -i)
            daysLabels.add(sdf.format(c.time))
        }
        val perDay = mutableMapOf<String, MutableList<Int>>()
        val windowSet = daysLabels.toSet()
        emotionData.forEach {
            if (windowSet.contains(it.date)) {
                perDay.getOrPut(it.date) { mutableListOf() }.add(it.mood.coerceIn(1, 10))
            }
        }
        val labels = mutableListOf<String>()
        val values = mutableListOf<Float>()
        if (days == 30) {
            val bucketSize = 3
            var idx = 0
            while (idx < daysLabels.size) {
                val bucketDays = daysLabels.subList(idx, kotlin.math.min(idx + bucketSize, daysLabels.size))
                val moods = mutableListOf<Int>()
                bucketDays.forEach { d ->
                    perDay[d]?.let { moods.addAll(it) }
                }
                if (moods.isNotEmpty()) {
                    val avg = moods.map { it.toFloat() }.average().toFloat()
                    val end = bucketDays.last().substring(5)
                    labels.add(end)
                    values.add(avg)
                }
                idx += bucketSize
            }
        } else {
            daysLabels.forEach { d ->
                val moods = perDay[d]
                if (moods != null && moods.isNotEmpty()) {
                    labels.add(d.substring(5))
                    values.add(moods.map { it.toFloat() }.average().toFloat())
                }
            }
        }
        if (values.isEmpty()) {
            EmptyEmotionChart()
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
                values.forEachIndexed { index, v ->
                    val barHeight by animateFloatAsState(
                        targetValue = (v.coerceIn(1f, 10f) / 10f) * 100f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "bar_height_$index"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(barHeight.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            getMoodColor(v.toInt()),
                                            getMoodColor(v.toInt()).copy(alpha = 0.6f)
                                        )
                                    ),
                                    RoundedCornerShape(12.dp, 12.dp, 4.dp, 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = labels.getOrElse(index) { "" },
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
                    text = "平均情绪值: ${"%.1f".format(values.average())}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
    
    @Composable
    fun QuickActionsSection() {
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "快捷操作",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        text = "找测评",
                        icon = Icons.Default.Search,
                        color = Color(0xFF30B4FF),
                        modifier = Modifier.weight(1f)
                    ) {
                        val vp = requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.view_pager)
                        vp?.setCurrentItem(2, true)
                    }
                    
                    QuickActionButton(
                        text = "咨询记录",
                        icon = Icons.Default.Chat,
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    ) {
                        val vp = requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.view_pager)
                        vp?.setCurrentItem(1, true)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                QuickActionButton(
                    text = "授权管理",
                    icon = Icons.Default.Security,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val intent = android.content.Intent(requireContext(), com.example.xinqiao.activity.AuthorizationActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
    
    @Composable
    fun QuickActionButton(
        text: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        color: Color,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        
        Button(
            onClick = onClick,
            modifier = modifier
                .scale(scale)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color.copy(alpha = 0.1f),
                contentColor = color
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp
            ),
            shape = RoundedCornerShape(12.dp),
            interactionSource = interactionSource
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
    
    @Composable
    fun LoadingOverview() {
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
                    text = "加载档案数据中...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
    
    @Composable
    fun EmptyOverview() {
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
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无档案数据",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = "开始记录你的健康数据吧",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    private fun loadOverviewData(
        callback: (OverviewStats, List<HotTestItem>, List<EmotionDiaryEntity>) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val stats = OverviewStats(
                    userName = userName,
                    completedCount = getCompletedCount(),
                    pendingCount = getPendingCount(),
                    unfinishedCount = getUnfinishedCount(),
                    sessionCount = getSessionCount(),
                    messageCount = getMessageCount()
                )
                
                val hotTests = getHotTests()
                val emotionData = getEmotionData()
                
                withContext(Dispatchers.Main) {
                    callback(stats, hotTests, emotionData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Return empty data on error
                withContext(Dispatchers.Main) {
                    callback(
                        OverviewStats(userName, 0, 0, 0, 0, 0),
                        emptyList(),
                        emptyList()
                    )
                }
            }
        }
    }
    
    private suspend fun getCompletedCount(): Int = withContext(Dispatchers.IO) {
        // Implementation would use TestRecordDao
        (0..15).random() // Placeholder
    }
    
    private suspend fun getPendingCount(): Int = withContext(Dispatchers.IO) {
        // Implementation would use TestRecordDao
        (0..5).random() // Placeholder
    }
    
    private suspend fun getUnfinishedCount(): Int = withContext(Dispatchers.IO) {
        // Implementation would use TestRecordDao
        (0..3).random() // Placeholder
    }
    
    private suspend fun getSessionCount(): Int = withContext(Dispatchers.IO) {
        // Implementation would use ChatSessionDao
        (5..25).random() // Placeholder
    }
    
    private suspend fun getMessageCount(): Int = withContext(Dispatchers.IO) {
        // Implementation would use ChatHistoryDao
        (20..100).random() // Placeholder
    }
    
    private suspend fun getHotTests(): List<HotTestItem> = withContext(Dispatchers.IO) {
        // Implementation would use TestRecordDao
        listOf(
            HotTestItem("抑郁自评量表(SDS)", 1250),
            HotTestItem("焦虑自评量表(SAS)", 980),
            HotTestItem("睡眠质量指数(PSQI)", 756),
            HotTestItem("压力知觉量表(PSS)", 642),
            HotTestItem("生活满意度量表(SWLS)", 534)
        )
    }
    
    private suspend fun getEmotionData(): List<EmotionDiaryEntity> = withContext(Dispatchers.IO) {
        // Implementation would use MedicalRecordRepository
        val calendar = Calendar.getInstance()
        return@withContext List(15) { index ->
            calendar.add(Calendar.DAY_OF_YEAR, -2)
            EmotionDiaryEntity().apply {
                this.id = index.toLong()
                this.userName = this@MedicalRecordOverviewFragmentNew.userName
                this.date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                this.mood = (5..10).random()
                this.noteEncrypted = "今日情绪记录"
            }
        }
    }
    
    private fun getMoodColor(mood: Int): Color {
        return when (mood) {
            in 1..3 -> Color(0xFFEF4444) // Red - Bad
            in 4..6 -> Color(0xFFF59E0B) // Orange - Neutral
            in 7..8 -> Color(0xFF10B981) // Green - Good
            else -> Color(0xFF3B82F6) // Blue - Excellent
        }
    }
    
    data class OverviewStats(
        val userName: String,
        val completedCount: Int,
        val pendingCount: Int,
        val unfinishedCount: Int,
        val sessionCount: Int,
        val messageCount: Int
    )
    
    data class HotTestItem(
        val title: String,
        val count: Int
    )
}
