package com.example.xinqiao.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.xinqiao.R
import com.example.xinqiao.bean.ConsultationItem
import com.example.xinqiao.repository.MedicalRecordRepository
import com.example.xinqiao.room.entities.ConsultationEntity
import com.example.xinqiao.utils.AnalysisUtils
import com.example.xinqiao.util.CryptoUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class ConsultationRecordsFragmentNew : Fragment() {
    
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
                    ConsultationRecordsScreen()
                }
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ConsultationRecordsScreen() {
        var consultationItems by remember { mutableStateOf<List<ConsultationItem>>(emptyList()) }
        var filteredItems by remember { mutableStateOf<List<ConsultationItem>>(emptyList()) }
        var selectedType by remember { mutableStateOf<String?>(null) }
        var dateRange by remember { mutableStateOf<DateRange?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var searchQuery by remember { mutableStateOf("") }
        var showDatePicker by remember { mutableStateOf(false) }
        
        // Load data
        LaunchedEffect(Unit) {
            loadConsultationData { items ->
                consultationItems = items
                filteredItems = items
                isLoading = false
            }
        }
        
        // Apply filters when type or date range changes
        LaunchedEffect(selectedType, dateRange, searchQuery) {
            filteredItems = applyFilters(consultationItems, selectedType, dateRange, searchQuery)
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
                            "咨询记录",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
                
                when {
                    isLoading -> {
                        LoadingConsultations()
                    }
                    consultationItems.isEmpty() -> {
                        EmptyConsultations()
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Search and Filter Section
                            SearchAndFilterSection(
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                selectedType = selectedType,
                                onTypeChange = { selectedType = it },
                                dateRange = dateRange,
                                onDateRangeChange = { dateRange = it },
                                onDatePickerShow = { showDatePicker = true }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Results Summary
                            ResultsSummary(filteredItems.size, consultationItems.size)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Consultation List
                            ConsultationList(filteredItems)
                            
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
    fun SearchAndFilterSection(
        searchQuery: String,
        onSearchChange: (String) -> Unit,
        selectedType: String?,
        onTypeChange: (String?) -> Unit,
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
                
                // Type Filter Chips
                Text(
                    text = "咨询类型",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { onTypeChange(null) },
                        label = { Text("全部") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    
                    FilterChip(
                        selected = selectedType == "ai",
                        onClick = { onTypeChange("ai") },
                        label = { Text("AI 咨询") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF30B4FF),
                            selectedLabelColor = Color.White
                        ),
                        leadingIcon = if (selectedType == "ai") {
                            { Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    
                    FilterChip(
                        selected = selectedType == "pro",
                        onClick = { onTypeChange("pro") },
                        label = { Text("专业咨询") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            selectedLabelColor = Color.White
                        ),
                        leadingIcon = if (selectedType == "pro") {
                            { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
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
                .height(52.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                ),
            placeholder = {
                Text(
                    "搜索咨询记录...",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
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
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
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
                    text = "$filteredCount / $totalCount 条记录",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
    
    @Composable
    fun ConsultationList(items: List<ConsultationItem>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEachIndexed { index, item ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInHorizontally(
                        initialOffsetX = { it * (index % 2 + 1) },
                        animationSpec = tween(300, delayMillis = index * 50)
                    )
                ) {
                    ConsultationItemCard(item)
                }
            }
        }
    }
    
    @Composable
    fun ConsultationItemCard(item: ConsultationItem) {
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
                    indication = LocalIndication.current,
                    onClick = { /* Handle item click */ }
                ),
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
                // Type Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (item.type == "ai") {
                                Color(0xFF30B4FF).copy(alpha = 0.2f)
                            } else {
                                Color(0xFF8B5CF6).copy(alpha = 0.2f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.type == "ai") {
                            Icons.Default.SmartToy
                        } else {
                            Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = if (item.type == "ai") {
                            Color(0xFF30B4FF)
                        } else {
                            Color(0xFF8B5CF6)
                        },
                        modifier = Modifier.size(24.dp)
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
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (item.status) {
                                "completed" -> Color(0xFF10B981).copy(alpha = 0.1f)
                                "ongoing" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                                else -> Color(0xFF6B7280).copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = when (item.status) {
                                    "completed" -> "已完成"
                                    "ongoing" -> "进行中"
                                    else -> "未知"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = when (item.status) {
                                        "completed" -> Color(0xFF10B981)
                                        "ongoing" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF6B7280)
                                    },
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Summary
                    Text(
                        text = item.summary ?: "暂无摘要",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bottom Info Row
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
                                text = item.date,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.messageCount} 条消息",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
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
                        "请选择要查看的咨询记录时间范围",
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
    fun LoadingConsultations() {
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
                    text = "加载咨询记录中...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
    
    @Composable
    fun EmptyConsultations() {
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
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无咨询记录",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = "开始你的第一次咨询吧",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
    
    private fun loadConsultationData(callback: (List<ConsultationItem>) -> Unit) {
        lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    // This would use the actual MedicalRecordRepository
                    // For now, we'll generate some sample data
                    generateSampleConsultationItems()
                }
                
                withContext(Dispatchers.Main) {
                    callback(items)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(emptyList())
                }
            }
        }
    }
    
    private fun generateSampleConsultationItems(): List<ConsultationItem> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        return List(15) { index ->
            calendar.add(Calendar.DAY_OF_YEAR, -(index * 2))
            val date = dateFormat.format(calendar.time)
            val isAI = index % 3 == 0
            
            ConsultationItem().apply {
                id = index.toLong()
                sessionId = "session_${1000 + index}"
                type = if (isAI) "ai" else "pro"
                title = if (isAI) {
                    "AI 心理咨询 #${index + 1}"
                } else {
                    "专业心理咨询 #${index + 1}"
                }
                this.date = date
                messageCount = (5..25).random()
                status = if (index < 10) "completed" else "ongoing"
                summary = if (isAI) {
                    "本次AI咨询主要讨论了情绪管理和压力应对策略，提供了一些实用的心理调节技巧。"
                } else {
                    "与专业心理咨询师的深度交流，探讨了个人成长、人际关系等话题，获得了专业的建议和指导。"
                }
            }
        }
    }
    
    private fun applyFilters(
        items: List<ConsultationItem>,
        selectedType: String?,
        dateRange: DateRange?,
        searchQuery: String
    ): List<ConsultationItem> {
        val q = searchQuery.trim()
        return items.filter { item ->
            // Type filter
            val typeMatch = selectedType == null || item.type.equals(selectedType, ignoreCase = true)
            
            // Date range filter
            val dateMatch = dateRange?.let { range ->
                item.date >= range.startDate && item.date <= range.endDate
            } ?: true
            
            // Search filter
            val searchMatch = q.isEmpty() || run {
                val tokens = q.split(Regex("\\s+"))
                val haystack = listOfNotNull(item.title, item.summary, item.sessionId)
                    .joinToString(" ").lowercase()
                tokens.any { token -> haystack.contains(token.lowercase()) }
            }
            
            typeMatch && dateMatch && searchMatch
        }
    }
    
    data class DateRange(
        val startDate: String,
        val endDate: String
    )
}
