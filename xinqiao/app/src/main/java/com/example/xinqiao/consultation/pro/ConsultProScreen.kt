package com.example.xinqiao.consultation.pro

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.CircleShape
// coil imports removed; using Glide via AndroidView for image loading
import androidx.core.content.ContextCompat
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.xinqiao.R

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ConsultProScreen(vm: ConsultProViewModel = viewModel()) {
    val ctx = LocalContext.current
    val consultants by vm.consultants.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val cities by vm.cities.collectAsState()
    val cityDict by vm.cityDict.collectAsState()
    val locationCity by vm.locationCity.collectAsState()
    val locationError by vm.locationError.collectAsState()
    val recentCities by vm.recentCities.collectAsState()

    val themeColor = androidx.compose.ui.graphics.Color(0xFF2F54EB)
    val scrollState = rememberScrollState()
    val listState = rememberLazyListState()

    // 定位权限申请：一次性请求精确/近似定位，任一授予即进行定位
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            vm.detectLocationCity(true)
        } else {
            Toast.makeText(ctx, "未授予定位权限", Toast.LENGTH_SHORT).show()
        }
    }

    val token = remember { readToken(ctx) }
    LaunchedEffect(Unit) {
        // 鍚姩鏃朵粎鍔犺浇鏁版嵁锛屼笉鐢宠瀹氫綅鏉冮檺銆佷笉瑙﹀彂瀹氫綅
        vm.refresh(token)
        vm.loadCityDict(token)
    }
    

    val pullRefreshState = rememberPullRefreshState(refreshing = loading, onRefresh = { vm.refresh(token) })

    var query by remember { mutableStateOf("") }
    // 顶部筛选栏状态
    var selectedConcern by remember { mutableStateOf(vm.field ?: "全部") }
    var selectedCity by remember { mutableStateOf(vm.city ?: "全部") }
    var selectedPriceRange by remember { mutableStateOf("不限") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showCitySheet by remember { mutableStateOf(false) }
    var priceAsc by remember { mutableStateOf(vm.sort == "价格从低到高") }
    // 排序菜单状态与当前选择
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(vm.sort ?: "综合评分") }

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
            // 搜索
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                    placeholder = { Text("搜索咨询师、城市或标签") }
                )
            }

            // 底部筛选弹层：咨询方式
            if (showFilterSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showFilterSheet = false },
                    sheetState = sheetState
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("咨询方式", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val modes = listOf("全部", "文字咨询", "语音咨询", "视频咨询")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            modes.forEach { m ->
                                AssistChip(
                                    onClick = {
                                        vm.setFilters(vm.field, m, vm.sort)
                                        showFilterSheet = false
                                        delayedReload { vm.refresh(token) }
                                    },
                                    label = { Text(m) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (m == (vm.mode ?: "全部")) androidx.compose.ui.graphics.Color(0xFFE8F0FE) else androidx.compose.ui.graphics.Color(0xFFF7F7F7),
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            // 顶部分类筛选栏：困扰 / 城市 / 价格 + 排序
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DropdownTab(
                        label = "困扰",
                        options = listOf("全部", "焦虑缓解", "抑郁纾解", "职场压力", "亲子关系", "子女教育"),
                        onSelect = { selectedConcern = it; vm.setFilters(it, vm.mode, vm.sort); delayedReload { vm.refresh(token) } }
                    )
                    // 城市：仅作为弹层入口（定位按钮移动到弹层顶部）
                    Row(
                        modifier = Modifier.clickable { showCitySheet = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("城市", color = androidx.compose.ui.graphics.Color(0xFF111111), fontSize = 16.sp)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF111111))
                    }
                    DropdownTab(
                        label = "价格",
                        options = listOf("不限", "<199", "200-299", "300-499", "500+"),
                        onSelect = { selectedPriceRange = it }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Outlined.FilterList, contentDescription = "筛选")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // 排序下拉菜单
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Outlined.SwapVert, contentDescription = "排序")
                            }
                            val sortOptions = listOf("综合评分", "价格从低到高", "价格从高到低", "评分从高到低", "咨询量从高到低")
                            androidx.compose.material3.DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                sortOptions.forEach { opt ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            selectedSort = opt
                                            priceAsc = (opt == "价格从低到高")
                                            vm.setFilters(vm.field, vm.mode, opt)
                                            showSortMenu = false
                                            delayedReload { vm.refresh(token) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Divider(color = androidx.compose.ui.graphics.Color(0xFFF5F5F5), thickness = 1.dp)
            }

            Box(modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)) {
                val displayList by produceState(
                    initialValue = emptyList<Consultant>(),
                    consultants, query, selectedConcern, selectedCity, selectedPriceRange, selectedSort
                ) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        val q = query.trim().lowercase()
                        val baseSearch = if (q.isEmpty()) consultants else consultants.filter { c ->
                            c.name.lowercase().contains(q) ||
                            c.title.lowercase().contains(q) ||
                            c.skills.any { it.lowercase().contains(q) }
                        }
                        // 本地“困扰”类型筛选（后端未实现该筛选）：按技能/标题关键词匹配
                        fun concernKeywords(label: String): List<String> = when (label) {
                            "全部" -> emptyList()
                            "焦虑缓解" -> listOf("焦虑缓解", "焦虑")
                            "抑郁纾解" -> listOf("抑郁")
                            "职场压力" -> listOf("职场压力")
                            "亲子关系" -> listOf("亲子教育")
                            "子女教育" -> listOf("亲子教育")
                            else -> listOf(label)
                        }
                        val byConcern = if (selectedConcern == "全部") baseSearch else {
                            val keys = concernKeywords(selectedConcern)
                            baseSearch.filter { c ->
                                keys.any { k ->
                                    c.skills.any { it.contains(k) } || c.title.contains(k)
                                }
                            }
                        }
                        // 城市规范化以消除“上海/上海市”等字面差异
                        fun normalizeCity(name: String?): String {
                            var s = (name ?: "").trim()
                            if (s.isEmpty()) return s
                            if (s.endsWith("自治区")) s = s.removeSuffix("自治区")
                            if (s.endsWith("特别行政区")) s = s.removeSuffix("特别行政区")
                            if (s.endsWith("省")) s = s.removeSuffix("省")
                            if (s.endsWith("市")) s = s.removeSuffix("市")
                            return s
                        }
                        // 本地城市筛选（与后端参数 city 互补，保证前端也能生效）
                        val byCity = if (selectedCity == "全部") byConcern else byConcern.filter {
                            normalizeCity(it.city) == normalizeCity(selectedCity)
                        }
                        // 本地价格区间过滤
                        val filtered = when (selectedPriceRange) {
                            "不限" -> byCity
                            "<199" -> byCity.filter { it.price < 199 }
                            "200-299" -> byCity.filter { it.price in 200..299 }
                            "300-499" -> byCity.filter { it.price in 300..499 }
                            "500+" -> byCity.filter { it.price >= 500 }
                            else -> byCity
                        }
                        // 本地排序（后端演示数据未实现排序）
                        when (selectedSort) {
                            "价格从低到高" -> filtered.sortedBy { it.price }
                            "价格从高到低" -> filtered.sortedByDescending { it.price }
                            "评分从高到低" -> filtered.sortedByDescending { it.rating }
                            "咨询量从高到低" -> filtered.sortedByDescending { it.consultCount }
                            else -> filtered // 综合评分保持原序
                        }
                    }
                }
                if (displayList.isEmpty() && !loading) {
                    EmptyState()
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        if (loading && displayList.isEmpty()) {
                            items(5) {
                                ConsultantCardSkeleton()
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                        items(displayList, key = { it.id }, contentType = { "consultant-card" }) { c: Consultant ->
                            ConsultantCard(c, themeColor, onClick = {
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
                                        it.putExtra("price", c.price)
                                        it.putExtra("duration", c.durationMinutes)
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
                PullRefreshIndicator(refreshing = loading, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter))
            }

            // 城市分层弹窗
            if (showCitySheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showCitySheet = false },
                    sheetState = sheetState
                ) {
                    CitySelectorSheet(
                        dict = cityDict,
                        locationCity = locationCity,
                        locationError = locationError,
                        recentCities = recentCities,
                        onLocate = {
                            val fineGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fineGranted || coarseGranted) {
                                vm.detectLocationCity(true)
                            } else {
                                locationPermLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            }
                        },
                        onSelect = { city ->
                            showCitySheet = false
                            selectedCity = city
                            vm.updateCity(city)
                            delayedReload { vm.refresh(token) }
                        }
                    )
                }
            }
        }
    // 保持函数未闭合，将列表加载逻辑放在 Composable 内部

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
}

// Java Bridge锛氭棤鍙傚叆鍙ｏ紝渚夸簬 ConsultationView.java 璋冪敤
@Composable
fun ConsultProScreenEntry() {
    ConsultProScreen()
}

@Composable
fun ConsultantCardLegacy(
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
            // 头像 + 认证角标
            Box(contentAlignment = Alignment.TopStart) {
                AndroidView(factory = { ctx ->
                    ImageView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(60.dp.value.toInt(), 60.dp.value.toInt())
                        setBackgroundColor(Color.TRANSPARENT)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        Glide.with(ctx)
                            .load(c.avatarUrl)
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.default_avatar)
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
            // 中部信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(6.dp))
                    Text(c.title, color = androidx.compose.ui.graphics.Color(0xFF666666), style = MaterialTheme.typography.bodySmall)
                }
                Row {
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
                    Text("评分 ${c.rating}", color = androidx.compose.ui.graphics.Color(0xFFFF9500), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(12.dp))
                    Text("咨询量 ${c.consultCount}+", color = androidx.compose.ui.graphics.Color(0xFF999999), style = MaterialTheme.typography.bodySmall)
                }
            }
            // 右侧价格与预约按钮
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("￥${c.price} / 次", color = themeColor, style = MaterialTheme.typography.titleSmall)
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
fun FilterRadioGroup(
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
fun ConsultantCard(
    c: Consultant,
    themeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onBook: () -> Unit
) {
    val gradient = Brush.horizontalGradient(listOf(ComposeColor(0xFF6B8AFD), ComposeColor(0xFF4F46E5)))
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val elevate by animateDpAsState(if (pressed) 6.dp else 4.dp, label = "cardElev")
    val ty by animateDpAsState(if (pressed) (-3).dp else 0.dp, label = "cardTy")
    val bg by animateColorAsState(if (pressed) ComposeColor(0xFFF9FAFB) else MaterialTheme.colorScheme.surface, label = "cardBg")

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = elevate),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp)
            .offset(y = ty)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(Modifier.fillMaxSize()) {
            // 顶部渐变边框
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(gradient)
                    .align(Alignment.TopCenter)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 左侧头像区 72dp
                Box(modifier = Modifier.width(72.dp)) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .border(3.dp, gradient, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .border(2.dp, ComposeColor.White, CircleShape)
                        ) {
                            AsyncImage(
                                model = c.avatarUrl,
                                contentDescription = null,
                                placeholder = painterResource(R.drawable.default_avatar),
                                error = painterResource(R.drawable.default_avatar),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                    }
                    // 在线状态指示（近似为圆形）
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (c.certified) ComposeColor(0xFF10B981) else ComposeColor(0xFFE5E7EB))
                            .border(1.dp, ComposeColor.White, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
                Spacer(Modifier.width(12.dp))

                // 右侧信息区
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        // 第一层：姓名 + 认证标签 + 职称
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                c.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ComposeColor(0xFF1F2937)
                            )
                            if (c.certified) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ComposeColor(0xFFE6F0FF))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "认证咨询师",
                                        fontSize = 10.sp,
                                        color = ComposeColor(0xFF2C6ECB),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            c.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = ComposeColor(0xFF6B7280)
                        )

                        Spacer(Modifier.height(8.dp))
                        // 第二层：擅长领域标签（最多显示 2/3/4 取决于屏幕宽度）
                        val conf = LocalConfiguration.current
                        val maxTags = when {
                            conf.screenWidthDp in 360..389 -> 2
                            conf.screenWidthDp >= 410 -> 4
                            else -> 3
                        }
                        val skills = c.skills.take(maxTags)
                        // 缓存技能标签的渐变，避免每次重组重复创建
                        val chipGradient = remember {
                            Brush.horizontalGradient(
                                listOf(ComposeColor(0xFFF0F7FF), ComposeColor(0xFFE6F0FF))
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            skills.forEach { skill ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipGradient)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        skill,
                                        fontSize = 12.sp,
                                        color = ComposeColor(0xFF2C6ECB)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        // 第三层：评分 + 咨询量 + 简介（简介数据缺失，使用职称替代作为简要说明）
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("★", color = ComposeColor(0xFFFF9F1C), fontSize = 20.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                String.format("%.1f", c.rating),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ComposeColor(0xFFFF9F1C)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("•", color = ComposeColor(0xFFD1D5DB), fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "咨询量${c.consultCount}+",
                                fontSize = 11.sp,
                                color = ComposeColor(0xFF9CA3AF)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            c.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = ComposeColor(0xFF6B7280),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 第四层：价格 + 预约按钮
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "¥${c.price}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ComposeColor(0xFF2C6ECB)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "/${c.durationMinutes}分钟",
                            fontSize = 11.sp,
                            color = ComposeColor(0xFF9CA3AF)
                        )
                        Spacer(Modifier.height(8.dp))
                        GradientButton(
                            text = "立即预约",
                            onClick = onBook,
                            gradient = gradient,
                            modifier = Modifier
                                .width(90.dp)
                                .height(36.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "btnScale")
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ComposeColor.White)
    }
}

@Composable
fun ConsultantCardSkeleton() {
    val shimmerColors = listOf(
        ComposeColor(0xFFF3F4F6),
        ComposeColor(0xFFE5E7EB),
        ComposeColor(0xFFF3F4F6)
    )
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 2000)
            )
            anim.snapTo(0f)
        }
    }
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = 300f * anim.value, y = 0f)
    )
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(16.dp)) {
            // 左侧头像占位
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.height(22.dp).fillMaxWidth(0.4f).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.height(16.dp).fillMaxWidth(0.6f).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) {
                        Box(Modifier.height(20.dp).width(60.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.height(32.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(brush))
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(Modifier.height(20.dp).width(60.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.height(36.dp).width(90.dp).clip(RoundedCornerShape(18.dp)).background(brush))
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 占位图
            Box(modifier = Modifier.size(80.dp).background(androidx.compose.ui.graphics.Color(0xFFCCCCCC)))
            Spacer(Modifier.height(12.dp))
            Text("暂无符合条件的咨询师，可尝试调整筛选条件", color = androidx.compose.ui.graphics.Color(0xFF999999))
        }
    }
}

fun delayedReload(block: () -> Unit) {
    // 绠€鍗曞欢锟?100ms锛岄伩鍏嶉绻佽锟?    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(block, 100)
}

fun readToken(ctx: android.content.Context): String? {
    val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
    return sp.getString("auth_token", null)
}

fun readLoginStatus(ctx: android.content.Context): Boolean {
    val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
    return sp.getBoolean("isLogin", false)
}
@Composable
fun DropdownTab(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = androidx.compose.ui.graphics.Color(0xFF111111), fontSize = 16.sp)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF111111))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { expanded = false; onSelect(opt) })
            }
        }
    }
}
@Composable
fun CitySelectorSheet(
    dict: CityDict?,
    locationCity: String?,
    locationError: String?,
    recentCities: List<String>,
    onLocate: () -> Unit,
    onSelect: (String) -> Unit
) {
    val tabs = dict?.tabs ?: emptyList()
    var tabIndex by remember { mutableStateOf(0) }
    // 偏向默认选中城市较多的分组（或“广东”）
    fun findBestGroupIndex(groups: List<CityGroup>): Int {
        if (groups.isEmpty()) return 0
        val idxByName = groups.indexOfFirst { it.label.contains("广东") }
        if (idxByName >= 0) return idxByName
        return groups.withIndex().maxByOrNull { it.value.cities.size }?.index ?: 0
    }
    var selectedGroupIndex by remember(dict) { mutableStateOf(findBestGroupIndex(tabs.getOrNull(0)?.groups ?: emptyList())) }
    var locating by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    // 定位成功后自动应用定位城市：仅在本次点击“使用当前定位”触发的定位流程中生效
    LaunchedEffect(locationCity, locating) {
        val lc = locationCity
        if (locating && !lc.isNullOrBlank()) {
            // 置位为 false 避免重复触发，然后直接选择并由上层关闭弹窗
            locating = false
            onSelect(lc)
        }
    }
    // 定位失败时提示并复位
    LaunchedEffect(locationError, locating) {
        val err = locationError
        if (locating && !err.isNullOrBlank()) {
            locating = false
            Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show()
        }
    }

    // 使用 Box 实现底部固定操作区，内容区域预留底部间距
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("城市选择", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            // 顶部：定位与最近浏览
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("定位", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF666666))
                Spacer(modifier = Modifier.height(6.dp))
                val chipBg = androidx.compose.ui.graphics.Color(0xFFF7F7F7)
                if (!locationCity.isNullOrBlank()) {
                    AssistChip(
                        onClick = { onSelect(locationCity!!) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF2F54EB))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(locationCity!!)
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = chipBg)
                    )
                } else {
                    AssistChip(
                        onClick = { locating = true; onLocate() },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF2F54EB))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (locating) "正在定位…" else "使用当前定位")
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = chipBg)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (recentCities.isNotEmpty()) {
                    Text("最近浏览", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF666666))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recentCities) { c: String ->
                            androidx.compose.material3.OutlinedButton(
                                onClick = { onSelect(c) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE5E5E5)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) { Text(c, color = androidx.compose.ui.graphics.Color(0xFF333333)) }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (tabs.isEmpty()) {
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxWidth()) {
                    items(listOf("全部")) { c: String ->
                        AssistChip(onClick = { onSelect(c) }, label = { Text(c) })
                    }
                }
                Spacer(modifier = Modifier.height(84.dp)) // 预留底部操作区高度
                return@Box
            }

            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tabIndex == i, onClick = {
                        tabIndex = i
                        selectedGroupIndex = findBestGroupIndex(tabs.getOrNull(i)?.groups ?: emptyList())
                    }) {
                        Text(t.label, modifier = Modifier.padding(12.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val current = tabs.getOrNull(tabIndex)
            if (current != null && current.groups.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // 左侧分组列表（选中高亮 + 指示条）
                    LazyColumn(modifier = Modifier.width(96.dp)) {
                        items(current.groups.size) { idx ->
                            val g = current.groups[idx]
                            val selected = selectedGroupIndex == idx
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (selected) androidx.compose.ui.graphics.Color(0xFFF2F6FF) else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { selectedGroupIndex = idx }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Box(modifier = Modifier.width(4.dp).height(18.dp).background(if (selected) androidx.compose.ui.graphics.Color(0xFF2F54EB) else androidx.compose.ui.graphics.Color.Transparent))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    g.label,
                                    color = if (selected) androidx.compose.ui.graphics.Color(0xFF2F54EB) else androidx.compose.ui.graphics.Color(0xFF333333)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // 右侧城市网格
                    val cities = current.groups.getOrNull(selectedGroupIndex)?.cities ?: emptyList()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(cities) { c: String ->
                            androidx.compose.material3.OutlinedButton(
                                onClick = { onSelect(c) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE5E5E5)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) { Text(c, color = androidx.compose.ui.graphics.Color(0xFF333333)) }
                        }
                    }
                }
            } else {
                // 热门城市或简单列表
                val hotCities = current?.cities ?: emptyList()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(hotCities) { c: String ->
                        androidx.compose.material3.OutlinedButton(
                            onClick = { onSelect(c) },
                            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE5E5E5)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) { Text(c, color = androidx.compose.ui.graphics.Color(0xFF333333)) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(84.dp)) // 内容底部为操作区预留空间
        }

        // 底部固定操作区
        Surface(
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            color = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = { onSelect("全部") },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("不限城市")
                }
            }
        }
    }
}
