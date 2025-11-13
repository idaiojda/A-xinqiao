package com.example.xinqiao.community

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.xinqiao.repository.MedicalRecordRepository
import com.example.xinqiao.utils.AnalysisUtils
import android.content.Intent
import com.example.xinqiao.activity.EmotionDiaryActivity
import com.example.xinqiao.activity.EmotionDiaryPreviewActivity
import com.example.xinqiao.room.entities.EmotionDiaryEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EmotionChallengeCardNew() {
    val tokens = CommunityTokensInstance
    val ctx = LocalContext.current
    var recordedDays by remember { mutableStateOf(0) }
    val totalDays = 21
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val repo = MedicalRecordRepository(ctx)
                val userName = AnalysisUtils.readLoginUserName(ctx)
                android.util.Log.d("EmotionChallengeCard", "Loading data for user: $userName")
                
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val cal = Calendar.getInstance()
                val end = sdf.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, -totalDays + 1)
                val start = sdf.format(cal.time)
                
                android.util.Log.d("EmotionChallengeCard", "Date range: $start to $end")
                
                // Use withContext to ensure database operation runs on IO dispatcher
                val list = withContext(Dispatchers.IO) {
                    repo.getEmotionDiariesByDateRange(userName, start, end)
                }
                
                android.util.Log.d("EmotionChallengeCard", "Found ${list.size} diary entries in range")
                
                list.forEach { diary ->
                    android.util.Log.d("EmotionChallengeCard", "Entry: date=${diary.date}, mood=${diary.mood}")
                }
                
                val days = list.map { it.date }.toSet().size
                android.util.Log.d("EmotionChallengeCard", "Unique days recorded: $days")
                
                recordedDays = days.coerceAtMost(totalDays)
                android.util.Log.d("EmotionChallengeCard", "Final recorded days: $recordedDays")
            } catch (e: Exception) { 
                android.util.Log.e("EmotionChallengeCard", "Error loading data", e)
                recordedDays = 0 
            }
        }
    }
    Card(
        shape = RoundedCornerShape(tokens.corner.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.elevate.Card),
        colors = CardDefaults.cardColors(containerColor = tokens.color.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.L),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.L)
        ) {
            Text(
                text = "21天情绪记录挑战",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = tokens.type.CardTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.color.Neutral900
                )
            )
            val target = recordedDays.toFloat() / totalDays
            val progress by animateFloatAsState(targetValue = target, label = "progress")
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = tokens.color.Primary,
                trackColor = tokens.color.Neutral200
            )
            Text(
                text = "已坚持 ${recordedDays}/${totalDays} 天，继续记录以获得徽章",
                style = MaterialTheme.typography.bodySmall.copy(color = tokens.color.Neutral700)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        try {
                            ctx.startActivity(Intent(ctx, EmotionDiaryActivity::class.java))
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(tokens.corner.Button)
                ) {
                    Text("继续记录")
                }
            }
        }
    }
}
