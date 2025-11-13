package com.example.xinqiao.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.xinqiao.bean.EmotionEntry
import com.example.xinqiao.repository.MedicalRecordRepository
import com.example.xinqiao.room.entities.EmotionDiaryEntity
import com.example.xinqiao.utils.AnalysisUtils
import com.example.xinqiao.util.CryptoUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EmotionDiaryFragmentNew : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    EmotionDiaryScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionDiaryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { MedicalRecordRepository(context) }
    val userName = remember { AnalysisUtils.readLoginUserName(context) }
    
    var entries by remember { mutableStateOf<List<EmotionEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Date range state
    var startDate by remember { mutableStateOf(getDefaultStartDate()) }
    var endDate by remember { mutableStateOf(getDefaultEndDate()) }
    
    // Chart data
    var chartData by remember { mutableStateOf<List<EmotionDataPoint>>(emptyList()) }
    
    // Animation states
    var headerVisible by remember { mutableStateOf(false) }
    var chartVisible by remember { mutableStateOf(false) }
    var listVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        headerVisible = true
        delay(200)
        chartVisible = true
        delay(300)
        listVisible = true
    }
    
    LaunchedEffect(userName, startDate, endDate) {
        scope.launch {
            loading = true
            try {
                val diaries = withContext(Dispatchers.IO) {
                    repo.getEmotionDiariesByDateRange(userName, startDate, endDate)
                }
                entries = diaries.map { diary ->
                    EmotionEntry(
                        diary.id,
                        diary.date,
                        diary.mood,
                        if (diary.noteEncrypted.isNullOrEmpty()) "" else CryptoUtil.decrypt(diary.noteEncrypted) ?: ""
                    )
                }.sortedByDescending { it.date }
                chartData = prepareChartData(entries)
                loading = false
            } catch (e: Exception) {
                loading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialOffsetY = { -it / 2 }
                )
            ) {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "情绪日记",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.Mood,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            var fabVisible by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                fabVisible = true
            }
            
            AnimatedVisibility(
                visible = fabVisible,
                enter = scaleIn(animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { 
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(
                                    animateFloatAsState(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        )
                                    ).value
                                )
                        )
                    },
                    text = { 
                        Text(
                            "记录心情",
                            modifier = Modifier.animateContentSize()
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            // Date Range Selector with animation
            AnimatedVisibility(
                visible = chartVisible,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) + slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetX = { -it / 3 }
                )
            ) {
                DateRangeSelector(
                    startDate = startDate,
                    endDate = endDate,
                    onStartDateChange = { startDate = it },
                    onEndDateChange = { endDate = it }
                )
            }
            
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            "正在加载心情数据...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else if (entries.isEmpty()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    EmptyState()
                }
            } else {
                // Emotion Trend Chart with enhanced animation
                AnimatedVisibility(
                    visible = chartVisible,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) {
                    EmotionTrendChart(
                        data = chartData,
                        dateRange = "$startDate 至 $endDate",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                // Emotion Entries List with staggered animation
                AnimatedVisibility(
                    visible = listVisible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 400))
                ) {
                    EmotionEntriesList(
                        entries = entries,
                        onDelete = { entry ->
                            scope.launch {
                                try {
                                    val diaries = withContext(Dispatchers.IO) {
                                        repo.deleteEmotionDiaryById(entry.id, userName)
                                        repo.getEmotionDiariesByDateRange(userName, startDate, endDate)
                                    }
                                    entries = diaries.map { diary ->
                                        EmotionEntry(
                                            diary.id,
                                            diary.date,
                                            diary.mood,
                                            if (diary.noteEncrypted.isNullOrEmpty()) "" else CryptoUtil.decrypt(diary.noteEncrypted) ?: ""
                                        )
                                    }.sortedByDescending { it.date }
                                    chartData = prepareChartData(entries)
                                } catch (e: Exception) {
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddEmotionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { mood, note ->
                scope.launch {
                    try {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val diaries = withContext(Dispatchers.IO) {
                            repo.addEmotionDiary(userName, date, mood, note)
                            repo.getEmotionDiariesByDateRange(userName, startDate, endDate)
                        }
                        entries = diaries.map { diary ->
                            EmotionEntry(
                                diary.id,
                                diary.date,
                                diary.mood,
                                if (diary.noteEncrypted.isNullOrEmpty()) "" else CryptoUtil.decrypt(diary.noteEncrypted) ?: ""
                            )
                        }.sortedByDescending { it.date }
                        chartData = prepareChartData(entries)
                        showAddDialog = false
                    } catch (e: Exception) {
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    startDate: String,
    endDate: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start Date
            OutlinedCard(
                onClick = { /* Date picker logic */ },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "开始日期",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        startDate,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // End Date
            OutlinedCard(
                onClick = { /* Date picker logic */ },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "结束日期",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        endDate,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Filter Button
            FilledIconButton(
                onClick = { /* Filter logic */ },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "筛选")
            }
        }
    }
}

@Composable
fun EmotionTrendChart(
    data: List<EmotionDataPoint>,
    dateRange: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Chart Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "情绪趋势",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = { },
                    label = { Text(dateRange) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mock Chart (you can integrate with MPAndroidChart or other charting library)
            if (data.isEmpty()) {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            "暂无情绪数据",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "点击右下角按钮记录第一条心情",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Simple emotion trend visualization
                EmotionTrendVisualization(data = data)
            }
        }
    }
}

@Composable
fun EmotionTrendVisualization(data: List<EmotionDataPoint>) {
    val maxMood = 10f
    val minMood = 1f
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                ),
                RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        // Y-axis labels with mood emojis
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("😄", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("10", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("😐", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("5", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("😢", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("1", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Animated trend line
        if (data.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.take(7).forEachIndexed { index, point ->
                    val heightFraction = (point.mood - minMood) / (maxMood - minMood)
                    val animatedHeight by animateFloatAsState(
                        targetValue = heightFraction,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "bar_height_$index"
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.animateContentSize()
                    ) {
                        // Floating emoji based on mood
                        var emojiVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            emojiVisible = true
                        }
                        
                        AnimatedVisibility(
                            visible = emojiVisible,
                            enter = scaleIn(animationSpec = tween(300, delayMillis = index * 100)) + fadeIn()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(
                                        animateFloatAsState(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioHighBouncy
                                            )
                                        ).value
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    getMoodEmoji(point.mood.toInt()),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Animated mood bar with gradient - Adjusted height for new container
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height((animatedHeight * 100).dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            getMoodColor(point.mood.toInt()),
                                            getMoodColor(point.mood.toInt()).copy(alpha = 0.6f)
                                        )
                                    ),
                                    RoundedCornerShape(14.dp, 14.dp, 4.dp, 4.dp)
                                )
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(14.dp, 14.dp, 4.dp, 4.dp),
                                    spotColor = getMoodColor(point.mood.toInt()).copy(alpha = 0.3f)
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Mood indicator dot with pulse animation
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    getMoodColor(point.mood.toInt()),
                                    CircleShape
                                )
                                .scale(
                                    animateFloatAsState(
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    ).value
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Date with enhanced styling - Fixed clipping issue
                        Card(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = point.date.takeLast(5),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Trend summary with emoji
        if (data.isNotEmpty()) {
            val avgMood = data.map { it.mood }.average()
            val trendEmoji = when {
                avgMood >= 8 -> "🌟"
                avgMood >= 6 -> "😊"
                avgMood >= 4 -> "😐"
                else -> "💪"
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "平均心情指数: ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "${avgMood.toInt()}/10 ",
                    style = MaterialTheme.typography.labelMedium,
                    color = getMoodColor(avgMood.toInt()),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    trendEmoji,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun EmotionEntriesList(
    entries: List<EmotionEntry>,
    onDelete: (EmotionEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(entries) { entry ->
            EmotionEntryCard(
                entry = entry,
                onDelete = { onDelete(entry) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionEntryCard(
    entry: EmotionEntry,
    onDelete: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it / 2 }
        ),
        exit = fadeOut() + slideOutVertically()
    ) {
        val moodColor = getMoodColor(entry.mood)
        val moodEmoji = getMoodEmoji(entry.mood)
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(60.dp)
                        .background(moodColor, RoundedCornerShape(2.dp))
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Mood circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(moodColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = moodEmoji,
                        fontSize = 28.sp
                    )
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
                            text = entry.date,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        AssistChip(
                            onClick = { },
                            label = { Text("${entry.mood}/10") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = moodColor.copy(alpha = 0.1f),
                                labelColor = moodColor
                            ),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(moodColor, CircleShape)
                                )
                            }
                        )
                    }
                    
                    if (entry.note.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = entry.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            maxLines = 2
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmotionDialog(
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    var mood by remember { mutableStateOf(5) }
    var note by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.9f)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "记录心情",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                
                // Mood selector
                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            getMoodColor(mood).copy(alpha = 0.1f),
                            RoundedCornerShape(60.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = getMoodColor(mood).copy(alpha = 0.1f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                getMoodEmoji(mood),
                                fontSize = 48.sp
                            )
                            Text(
                                "$mood/10",
                                style = MaterialTheme.typography.titleLarge,
                                color = getMoodColor(mood),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Mood slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
                        value = mood.toFloat(),
                        onValueChange = { mood = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = getMoodColor(mood),
                            activeTrackColor = getMoodColor(mood),
                            inactiveTrackColor = getMoodColor(mood).copy(alpha = 0.3f)
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1", style = MaterialTheme.typography.labelSmall)
                        Text("10", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                // Note input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("记录今天的心情... 💭") },
                    label = { Text("心情备注") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = getMoodColor(mood),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    maxLines = 3,
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                )
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(mood, note) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = getMoodColor(mood)
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        animationProgress = 1f
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.scale(
                animateFloatAsState(
                    targetValue = animationProgress,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ).value
            )
        ) {
            // Animated emoji background
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle with gradient
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            ),
                            CircleShape
                        )
                        .scale(
                            animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000),
                                    repeatMode = RepeatMode.Reverse
                                )
                            ).value
                        )
                )
                
                // Floating emojis
                Row {
                    listOf("😊", "💭", "🌈", "✨", "💝").forEachIndexed { index, emoji ->
                        var emojiVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(index * 200L)
                            emojiVisible = true
                        }
                        
                        AnimatedVisibility(
                            visible = emojiVisible,
                            enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioHighBouncy
                                )
                            )
                        ) {
                            Text(
                                emoji,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
                
                // Main emoji
                Text(
                    "📝",
                    fontSize = 48.sp,
                    modifier = Modifier
                        .scale(
                            animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessVeryLow
                                )
                            ).value
                        )
                )
            }
            
            // Title with gradient text
            Text(
                "开始记录你的心情之旅",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Description with enhanced styling
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Mood,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "记录每日情绪变化",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "追踪情绪趋势",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "了解情绪模式",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Call to action with emoji
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        "💡",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        "点击右下角按钮开始记录第一条心情",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Helper functions
private fun getDefaultStartDate(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -6)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
}

private fun getDefaultEndDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

private fun prepareChartData(entries: List<EmotionEntry>): List<EmotionDataPoint> {
    return entries.groupBy { it.date }
        .map { (date, dayEntries) ->
            EmotionDataPoint(
                date = date,
                mood = dayEntries.map { it.mood }.average().toFloat(),
                note = dayEntries.firstOrNull()?.note ?: ""
            )
        }
        .sortedBy { it.date }
}

@Composable
fun getMoodColor(mood: Int): Color {
    return when (mood) {
        in 1..2 -> Color(0xFFEF5350) // Red
        in 3..4 -> Color(0xFFAB47BC) // Purple
        5 -> Color(0xFF78909C) // Gray
        in 6..7 -> Color(0xFF66BB6A) // Green
        in 8..9 -> Color(0xFF42A5F5) // Blue
        10 -> Color(0xFFFFCA28) // Yellow
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun getMoodEmoji(mood: Int): String {
    return when (mood) {
        in 1..2 -> "😢"
        in 3..4 -> "😔"
        5 -> "😐"
        in 6..7 -> "😊"
        in 8..9 -> "😄"
        10 -> "🤗"
        else -> "😐"
    }
}

data class EmotionDataPoint(
    val date: String,
    val mood: Float,
    val note: String
)
