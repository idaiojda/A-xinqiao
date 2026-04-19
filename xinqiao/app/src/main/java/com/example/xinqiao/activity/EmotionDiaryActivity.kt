package com.example.xinqiao.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xinqiao.repository.EmotionDiaryApiRepository
import com.example.xinqiao.room.entities.EmotionDiaryEntity
import com.example.xinqiao.util.AnalysisUtils
import com.example.xinqiao.util.crypto.CryptoUtil
import com.example.xinqiao.network.RetrofitClient
import com.example.xinqiao.bean.EmotionEntry
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun calculateCurrentStreak(diaries: List<EmotionDiaryEntity>): Int {
    if (diaries.isEmpty()) return 0
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = Calendar.getInstance()
    val recordedDates = diaries.map { it.date }.toSet()
    
    var streak = 0
    var currentDate = today.clone() as Calendar
    
    while (true) {
        val dateStr = sdf.format(currentDate.time)
        if (recordedDates.contains(dateStr)) {
            streak++
            currentDate.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    
    return streak
}

fun calculateCurrentStreakFromEntries(entries: List<EmotionEntry>): Int {
    if (entries.isEmpty()) return 0
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = Calendar.getInstance()
    val recordedDates = entries.map { it.date }.toSet()
    
    var streak = 0
    var currentDate = today.clone() as Calendar
    
    while (true) {
        val dateStr = sdf.format(currentDate.time)
        if (recordedDates.contains(dateStr)) {
            streak++
            currentDate.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    
    return streak
}

@Composable
fun getMoodEmoji(mood: Int): String {
    return when (mood) {
        in 1..2 -> "😢" // Very sad
        in 3..4 -> "😔" // Sad
        5 -> "😐" // Neutral
        in 6..7 -> "😊" // Happy
        in 8..9 -> "😄" // Very happy
        10 -> "🤗" // Extremely happy
        else -> "😐"
    }
}

@Composable
fun ChallengeProgressBar(
    currentDays: Int,
    totalDays: Int = 21,
    modifier: Modifier = Modifier,
    showIndicator: Boolean = true
) {
    val progress = if (totalDays > 0) currentDays.toFloat() / totalDays.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Progress track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            // Progress fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF9C27B0), // Purple start
                                Color(0xFFE91E63)  // Pink end
                            )
                        )
                    )
                    .animateContentSize()
            )
            
            // Progress indicator dot
            if (showIndicator && currentDays > 0) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color(0xFF9C27B0), CircleShape)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-6).dp)
                        .scale(
                            animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                )
                            ).value
                        )
                )
            }
        }
        
        // Progress text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已坚持 ${currentDays}/${totalDays} 天",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            if (currentDays >= totalDays) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "完成！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        if (currentDays < totalDays && currentDays > 0) {
            Text(
                text = "继续记录以获得徽章 🎖️",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun CompletionCelebrationCard(
    onRestartChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Celebration emoji with animation
                Text(
                    text = "🎉",
                    fontSize = 64.sp,
                    modifier = Modifier.scale(
                        animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessVeryLow
                            )
                        ).value
                    )
                )
                
                Text(
                    text = "你做到了！",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF4CAF50)
                )
                
                Text(
                    text = "恭喜你完成了21天情绪记录挑战！",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "你已经养成了记录情绪的好习惯，继续保持！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onRestartChallenge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始新的挑战")
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📝",
                fontSize = 64.sp,
                modifier = Modifier.scale(
                    animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ).value
                )
            )
            Text(
                text = "还没有心情记录",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "点击右下角的 + 按钮开始记录你的心情吧！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedAddDiarySheet(
    mood: Float,
    note: String,
    onMoodChange: (Float) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                Text(
                    text = "记录心情",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Enhanced mood selector
            MoodSelector(
                currentMood = mood,
                onMoodChange = onMoodChange
            )
            
            // Note input with enhanced styling
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { 
                    Text("记录今天的心情… 💭")
                },
                label = { Text("心情备注") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = getMoodColor(mood.toInt()),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                maxLines = 5
            )
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("取消")
                }
                
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = getMoodColor(mood.toInt())
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存心情")
                }
            }
        }
    }
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
        else -> Color.Gray
    }
}

@Composable
fun AnimatedMoodCard(
    diary: EmotionDiaryEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
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
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        val moodColor = getMoodColor(diary.mood)
        val moodEmoji = getMoodEmoji(diary.mood)
        val decryptedNote = remember(diary.noteEncrypted) {
            if (diary.noteEncrypted.isNullOrEmpty()) "" 
            else CryptoUtil.decrypt(diary.noteEncrypted) ?: ""
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = moodColor.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mood emoji with animation
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(moodColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = moodEmoji,
                        fontSize = 24.sp,
                        modifier = Modifier.scale(
                            animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ).value
                        )
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "日期：${diary.date}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "心情：${diary.mood}/10",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp,
                            color = moodColor
                        )
                        Text(
                            text = decryptedNote.takeIf { it.isNotEmpty() } ?: "无备注",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                }
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MoodSelector(
    currentMood: Float,
    onMoodChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current mood display with emoji
        val currentMoodInt = currentMood.toInt()
        val moodEmoji = getMoodEmoji(currentMoodInt)
        val moodColor = getMoodColor(currentMoodInt)
        
        Card(
            modifier = Modifier
                .size(80.dp)
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = moodColor.copy(alpha = 0.2f)
            ),
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = moodEmoji,
                    fontSize = 40.sp,
                    modifier = Modifier.scale(
                        animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        ).value
                    )
                )
            }
        }
        
        // Mood score display
        Text(
            text = "心情分数：${currentMoodInt}/10",
            style = MaterialTheme.typography.titleMedium,
            color = moodColor
        )
        
        // Enhanced slider with mood previews
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Mood preview icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 1..10) {
                    if (i % 2 == 1) { // Show emoji for odd numbers
                        Text(
                            text = getMoodEmoji(i),
                            fontSize = 16.sp,
                            color = if (i == currentMoodInt) getMoodColor(i) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            
            Slider(
                value = currentMood,
                onValueChange = onMoodChange,
                valueRange = 1f..10f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = moodColor,
                    activeTrackColor = moodColor,
                    inactiveTrackColor = moodColor.copy(alpha = 0.3f)
                )
            )
        }
    }
}

class EmotionDiaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EmotionDiaryScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EmotionDiaryScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 初始化API Repository
    val apiRepo = remember { EmotionDiaryApiRepository(ctx) }
    LaunchedEffect(Unit) {
        RetrofitClient.initFromContext(ctx)
    }
    
    val user = remember { AnalysisUtils.readLoginUserName(ctx) }
    var entries by remember { mutableStateOf<List<EmotionEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var mood by remember { mutableStateOf(5f) }
    var note by remember { mutableStateOf("") }
    
    // 21-day challenge progress tracking
    var challengeDays by remember { mutableStateOf(0) }
    var currentStreak by remember { mutableStateOf(0) }

    LaunchedEffect(user) {
        loading = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    apiRepo.getAllDiaries(user)
                }
                
                if (result.isSuccess) {
                    entries = result.getOrNull() ?: emptyList()
                    
                    // Calculate 21-day challenge progress
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, -21)
                    val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    
                    val recentDiaries = entries.filter { entry ->
                        entry.date >= startDate
                    }
                    challengeDays = recentDiaries.distinctBy { it.date }.size
                    
                    // Calculate current streak
                    currentStreak = calculateCurrentStreakFromEntries(entries)
                }
                loading = false
            } catch (e: Exception) {
                android.util.Log.e("EmotionDiary", "Error loading diaries", e)
                loading = false
            }
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    var challengeCompleted by remember { mutableStateOf(false) }
    
    // Check if challenge is completed
    LaunchedEffect(challengeDays) {
        challengeCompleted = challengeDays >= 21
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "心情日记",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Mood summary chip
                        if (entries.isNotEmpty()) {
                            val avgMood = entries.map { it.mood }.average().toInt()
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text("平均：${avgMood}/10 ${getMoodEmoji(avgMood)}")
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = getMoodColor(avgMood).copy(alpha = 0.1f),
                                    labelColor = getMoodColor(avgMood)
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { (ctx as? android.app.Activity)?.finish() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            var fabExpanded by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                fabExpanded = true
            }
            
            AnimatedVisibility(
                visible = fabExpanded,
                enter = scaleIn(animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加心情日记",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 21-day challenge progress or completion card
            if (challengeCompleted) {
                CompletionCelebrationCard(
                    onRestartChallenge = {
                        // Reset challenge by clearing the completion state
                        // This will allow starting a new 21-day challenge
                        challengeCompleted = false
                        challengeDays = 0
                    }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "21天情绪记录挑战",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            if (currentStreak > 0) {
                                AssistChip(
                                    onClick = { },
                                    label = { 
                                        Text("连续 ${currentStreak} 天 🔥")
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color(0xFFFF5722).copy(alpha = 0.1f),
                                        labelColor = Color(0xFFFF5722)
                                    )
                                )
                            }
                        }
                        
                        ChallengeProgressBar(
                            currentDays = challengeDays,
                            totalDays = 21,
                            showIndicator = true
                        )
                    }
                }
            }
            
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (entries.isEmpty()) {
                EmptyStateMessage()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(entries) { index, entry ->
                        AnimatedMoodCardFromEntry(
                            entry = entry,
                            onDelete = {
                                scope.launch {
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            apiRepo.deleteDiary(entry.id)
                                        }
                                        if (result.isSuccess) {
                                            // 重新加载
                                            val refreshResult = withContext(Dispatchers.IO) {
                                                apiRepo.getAllDiaries(user)
                                            }
                                            if (refreshResult.isSuccess) {
                                                entries = refreshResult.getOrNull() ?: emptyList()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("EmotionDiary", "Error deleting diary", e)
                                    }
                                }
                            },
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        EnhancedAddDiarySheet(
            mood = mood,
            note = note,
            onMoodChange = { mood = it },
            onNoteChange = { note = it },
            onSave = {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.format(Date())
                android.util.Log.d("EmotionDiary", "Saving diary: user=$user, date=$date, mood=${mood.toInt()}")
                
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            apiRepo.createDiary(user, date, mood.toInt(), note)
                        }
                        
                        if (result.isSuccess) {
                            android.util.Log.d("EmotionDiary", "Diary saved successfully")
                            
                            // 重新加载
                            val refreshResult = withContext(Dispatchers.IO) {
                                apiRepo.getAllDiaries(user)
                            }
                            if (refreshResult.isSuccess) {
                                entries = refreshResult.getOrNull() ?: emptyList()
                                android.util.Log.d("EmotionDiary", "Reloaded diaries count: ${entries.size}")
                                
                                // Recalculate challenge progress
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.DAY_OF_YEAR, -21)
                                val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                                
                                val recentDiaries = entries.filter { entry ->
                                    entry.date >= startDate
                                }
                                android.util.Log.d("EmotionDiary", "Recent 21 days diaries: ${recentDiaries.size}, unique days: ${recentDiaries.distinctBy { it.date }.size}")
                                
                                challengeDays = recentDiaries.distinctBy { it.date }.size
                                currentStreak = calculateCurrentStreakFromEntries(entries)
                            }
                            
                            note = ""
                            mood = 5f
                            showAdd = false
                        }
                    } catch (e: Exception) { 
                        android.util.Log.e("EmotionDiary", "Error saving diary", e)
                        showAdd = false 
                    }
                }
            },
            onDismiss = { showAdd = false }
        )
    }
}


@Composable
fun AnimatedMoodCardFromEntry(
    entry: EmotionEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 将 EmotionEntry 转换为显示格式
    val moodEmoji = when (entry.mood) {
        in 1..2 -> "😢"
        in 3..4 -> "😔"
        5 -> "😐"
        in 6..7 -> "😊"
        in 8..9 -> "😄"
        10 -> "🤗"
        else -> "😐"
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = moodEmoji,
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 48.sp
                )
                
                Column {
                    Text(
                        text = "日期：${entry.date}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "心情：${entry.mood}/10",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.note.isNotBlank()) {
                        Text(
                            text = entry.note,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
