package com.example.xinqiao.consultation.pro

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.xinqiao.activity.LoginActivity
import com.example.xinqiao.activity.MyAppointmentsActivity
import com.example.xinqiao.activity.AppointmentDetailActivity
import com.example.xinqiao.activity.ConsultantDetailActivity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultProScreen(vm: ConsultProViewModel = viewModel()) {
    val ctx = LocalContext.current
    val consultants by vm.consultants.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val themeColor = androidx.compose.ui.graphics.Color(0xFF2F54EB)
    val scrollState = rememberScrollState()
    val listState = rememberLazyListState()

    val token = remember { readToken(ctx) }
    LaunchedEffect(Unit) {
        // 启动时不再强制跳转登录；直接刷新数据（无 token 也可尝试获取公开列表）
        vm.refresh(token)
    }

    val swipeState = rememberSwipeRefreshState(isRefreshing = loading)

    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    actionIconContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                ),
                title = { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("咨询", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (ctx is Activity) {
                            if (readLoginStatus(ctx)) {
                                ctx.startActivity(Intent(ctx, MyAppointmentsActivity::class.java))
                            } else {
                                Toast.makeText(ctx, "请先登录", Toast.LENGTH_SHORT).show()
                                ctx.startActivity(Intent(ctx, LoginActivity::class.java))
                            }
                        }
                    }) { Icon(Icons.Default.List, contentDescription = "咨询记录") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            // 搜索栏
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                    placeholder = { Text("搜索咨询师、领域或标签") }
                )
            }
            // 筛选栏
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    FilterRadioGroup(
                        label = "咨询领域",
                        options = listOf("全部", "抑郁干预", "焦虑缓解", "职场压力", "情感关系", "亲子教育"),
                        selected = vm.field ?: "全部",
                        onSelect = { vm.setFilters(it, vm.mode, vm.sort); delayedReload { vm.refresh(token) } },
                        themeColor = themeColor
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    FilterRadioGroup(
                        label = "咨询形式",
                        options = listOf("全部", "文字咨询", "语音咨询", "视频咨询"),
                        selected = vm.mode ?: "全部",
                        onSelect = { vm.setFilters(vm.field, it, vm.sort); delayedReload { vm.refresh(token) } },
                        themeColor = themeColor
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    FilterRadioGroup(
                        label = "排序方式",
                        options = listOf("综合评分", "咨询量", "价格从低到高", "价格从高到低"),
                        selected = vm.sort ?: "综合评分",
                        onSelect = { vm.setFilters(vm.field, vm.mode, it); delayedReload { vm.refresh(token) } },
                        themeColor = themeColor
                    )
                }
                Divider(color = androidx.compose.ui.graphics.Color(0xFFF5F5F5), thickness = 1.dp)
            }

            SwipeRefresh(state = swipeState, onRefresh = { vm.refresh(token) }) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val displayList = remember(consultants, query) {
                        val q = query.trim().lowercase()
                        if (q.isEmpty()) consultants else consultants.filter { c ->
                            c.name.lowercase().contains(q) ||
                            c.title.lowercase().contains(q) ||
                            c.skills.any { it.lowercase().contains(q) }
                        }
                    }
                    if (displayList.isEmpty() && !loading) {
                        EmptyState()
                    } else {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            items(displayList) { c ->
                                ConsultantItem(c, themeColor, onClick = {
                                    if (ctx is Activity) {
                                        val it = Intent(ctx, ConsultantDetailActivity::class.java)
                                        it.putExtra("consultantId", c.id)
                                        ctx.startActivity(it)
                                    }
                                }, onBook = {
                                    if (ctx is Activity) {
                                        if (readLoginStatus(ctx)) {
                                            val it = Intent(ctx, AppointmentDetailActivity::class.java)
                                            it.putExtra("consultantId", c.id)
                                            it.putExtra("name", c.name)
                                            it.putExtra("mode", c.defaultMode)
                                            ctx.startActivity(it)
                                        } else {
                                            Toast.makeText(ctx, "请先登录", Toast.LENGTH_SHORT).show()
                                            ctx.startActivity(Intent(ctx, LoginActivity::class.java))
                                        }
                                    }
                                })
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // 列表末尾自动加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastIndex >= consultants.size - 2 && consultants.isNotEmpty()
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !loading) vm.loadMore(token)
    }
}

// Java Bridge：无参入口，便于 ConsultationView.java 调用
@Composable
fun ConsultProScreenEntry() {
    ConsultProScreen()
}

@Composable
private fun FilterRadioGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    themeColor: androidx.compose.ui.graphics.Color
) {
    Column {
        Text(label, color = androidx.compose.ui.graphics.Color(0xFF666666), style = MaterialTheme.typography.bodySmall)
        Row { options.forEach { opt ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .clip(RectangleShape)
                    .background(if (opt == selected) androidx.compose.ui.graphics.Color(0xFFE8F0FE) else androidx.compose.ui.graphics.Color.Transparent)
            ) {
                RadioButton(selected = opt == selected, onClick = { onSelect(opt) }, colors = RadioButtonDefaults.colors(
                    selectedColor = themeColor
                ))
                Text(opt, color = if (opt == selected) themeColor else androidx.compose.ui.graphics.Color(0xFF333333))
            }
        } }
    }
}

@Composable
private fun ConsultantItem(
    c: Consultant,
    themeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onBook: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .border(1.dp, androidx.compose.ui.graphics.Color(0xFFF0F0F0), RectangleShape)
        .clip(RectangleShape)
        .clickable { onClick() }) {
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)) {
            // 头像 + 认证徽章
            Box(contentAlignment = Alignment.TopStart) {
                AndroidView(factory = { ctx ->
                    ImageView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(60.dp.value.toInt(), 60.dp.value.toInt())
                        setBackgroundColor(Color.TRANSPARENT)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        Glide.with(ctx)
                            .load(c.avatarUrl)
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .circleCrop()
                            .into(this)
                    }
                }, modifier = Modifier.size(60.dp))
                Box(modifier = Modifier
                    .offset(y = 48.dp)
                    .background(androidx.compose.ui.graphics.Color(0xFFFFE5E5))) {
                    Text("认证", color = androidx.compose.ui.graphics.Color(0xFFCC0000), fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // 中间信息区
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(6.dp))
                    Text(c.title, color = androidx.compose.ui.graphics.Color(0xFF666666), style = MaterialTheme.typography.bodySmall)
                }
                Row { // 擅长领域标签（最多2个）
                    c.skills.take(2).forEach { skill ->
                        Box(modifier = Modifier
                            .padding(end = 6.dp)
                            .background(androidx.compose.ui.graphics.Color(0xFFF0F5FF))) {
                            Text(skill, color = themeColor, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                        }
                    }
                    val extra = c.skills.size - 2
                    if (extra > 0) Text("+${extra}", color = themeColor, style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★ ${c.rating}", color = androidx.compose.ui.graphics.Color(0xFFFF9500), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(12.dp))
                    Text("咨询量 ${c.consultCount}+", color = androidx.compose.ui.graphics.Color(0xFF999999), style = MaterialTheme.typography.bodySmall)
                }
            }
            // 右侧价格与预约按钮
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¥${c.price} / 次", color = themeColor, style = MaterialTheme.typography.titleSmall)
                Text("${c.durationMinutes} 分钟", color = androidx.compose.ui.graphics.Color(0xFF999999), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBook, colors = ButtonDefaults.buttonColors(containerColor = themeColor), modifier = Modifier.width(80.dp).height(30.dp)) {
                    Text("立即预约", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 简化空态图标
            Box(modifier = Modifier.size(80.dp).background(androidx.compose.ui.graphics.Color(0xFFCCCCCC)))
            Spacer(Modifier.height(12.dp))
            Text("暂无符合条件的咨询师，可尝试调整筛选条件", color = androidx.compose.ui.graphics.Color(0xFF999999))
        }
    }
}

private fun delayedReload(block: () -> Unit) {
    // 简单延迟 100ms，避免频繁请求
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(block, 100)
}

private fun readToken(ctx: android.content.Context): String? {
    val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
    return sp.getString("auth_token", null)
}

private fun readLoginStatus(ctx: android.content.Context): Boolean {
    val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
    return sp.getBoolean("isLogin", false)
}