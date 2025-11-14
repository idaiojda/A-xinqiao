package com.example.xinqiao.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.xinqiao.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay

class MedicalRecordActivityModern : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MedicalRecordScreen()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MedicalRecordScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
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
        Column {
            // Enhanced Animated Header
            AnimatedHeader(
                title = "我的诊疗档案",
                subtitle = "健康管理 · 专业贴心",
                onBackClick = { (context as? FragmentActivity)?.finish() },
                onSettingsClick = { /* Settings functionality */ }
            )
            
            // Modern Tab Layout
            ModernTabLayout(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                tabs = listOf("概览", "咨询记录", "测评报告", "情绪日记", "健康指标")
            )
            
            // Content Area with Animation
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetX = { it }
                    ) + fadeIn() with
                    slideOutHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        targetOffsetX = { -it }
                    ) + fadeOut()
                }
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> OverviewContent()
                    1 -> ConsultationContent()
                    2 -> AssessmentContent()
                    3 -> EmotionContent()
                    4 -> HealthMetricsContent()
                    else -> OverviewContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedHeader(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            )
            .padding(top = 24.dp, bottom = 16.dp)
            .alpha(animatedAlpha)
            .scale(animatedScale)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Back Button
            AnimatedBackButton(onClick = onBackClick)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Animated Title and Subtitle
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedTitle(text = title)
                AnimatedSubtitle(text = subtitle)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Animated Settings Button
            AnimatedSettingsButton(onClick = onSettingsClick)
        }
    }
}

@Composable
fun AnimatedBackButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = {
                    isPressed = true
                    onClick()
                }
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "返回",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AnimatedTitle(text: String) {
    val animatedOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        letterSpacing = 0.02.sp,
        modifier = Modifier.offset(y = animatedOffset.dp)
    )
}

@Composable
fun AnimatedSubtitle(text: String) {
    var subtitleStart by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        subtitleStart = true
    }
    val animatedOffset by animateFloatAsState(
        targetValue = if (subtitleStart) 0f else -6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Text(
        text = text,
        fontSize = 12.sp,
        color = Color.White.copy(alpha = 0.8f),
        letterSpacing = 0.05.sp,
        modifier = Modifier.offset(y = animatedOffset.dp)
    )
}

@Composable
fun AnimatedSettingsButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = {
                    isPressed = true
                    onClick()
                }
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "设置",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ModernTabLayout(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = Color(0xFF6B7280),
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, tabName ->
                Tab(
                    selected = index == selectedTab,
                    onClick = { onTabSelected(index) },
                    selectedContentColor = Color(0xFF667eea),
                    unselectedContentColor = Color(0xFF6B7280),
                    text = {
                        Text(
                            text = tabName,
                            fontSize = 13.sp,
                            fontWeight = if (index == selectedTab) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ModernTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {}

// Placeholder content functions
@Composable
fun OverviewContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("概览内容", color = Color(0xFF6B7280))
    }
}

@Composable
fun ConsultationContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("咨询记录内容", color = Color(0xFF6B7280))
    }
}

@Composable
fun AssessmentContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("测评报告内容", color = Color(0xFF6B7280))
    }
}

@Composable
fun EmotionContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("情绪日记内容", color = Color(0xFF6B7280))
    }
}

@Composable
fun HealthMetricsContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("健康指标内容", color = Color(0xFF6B7280))
    }
}
