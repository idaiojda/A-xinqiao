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
import androidx.core.content.ContextCompat
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
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
    

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = loading)

    var query by remember { mutableStateOf("") }
    // 顶部筛选栏状态
    var selectedConcern by remember { mutableStateOf(vm.field ?: "全部") }
    var selectedCity by remember { mutableStateOf(vm.city ?: "全部") }
    var selectedPriceRange by remember { mutableStateOf("不限") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showCitySheet by remember { mutableStateOf(false) }
    var priceAsc by remember { mutableStateOf(vm.sort == "价格从低到高") }

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
                        IconButton(onClick = {
                            priceAsc = !priceAsc
                            val s = if (priceAsc) "价格从低到高" else "价格从高到低"
                            vm.setFilters(vm.field, vm.mode, s)
                            delayedReload { vm.refresh(token) }
                        }) {
                            Icon(Icons.Outlined.SwapVert, contentDescription = "排序")
                        }
                    }
                }
                Divider(color = androidx.compose.ui.graphics.Color(0xFFF5F5F5), thickness = 1.dp)
            }

            Box(modifier = Modifier
                .fillMaxSize()) {
                SwipeRefresh(state = swipeRefreshState, onRefresh = { vm.refresh(token) }) {
                val displayList = remember(consultants, query, selectedPriceRange) {
                    val q = query.trim().lowercase()
                    val base = if (q.isEmpty()) consultants else consultants.filter { c ->
                        c.name.lowercase().contains(q) ||
                        c.title.lowercase().contains(q) ||
                        c.skills.any { it.lowercase().contains(q) }
                    }
                    // 本地价格区间过滤
                    when (selectedPriceRange) {
                        "不限" -> base
                        "<199" -> base.filter { it.price < 199 }
                        "200-299" -> base.filter { it.price in 200..299 }
                        "300-499" -> base.filter { it.price in 300..499 }
                        "500+" -> base.filter { it.price >= 500 }
                        else -> base
                    }
                }
                if (displayList.isEmpty() && !loading) {
                    EmptyState()
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        items(displayList) { c: Consultant ->
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
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, androidx.compose.ui.graphics.Color(0xFFF0F0F0), RectangleShape)
            .clip(RectangleShape)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(c.title, color = androidx.compose.ui.graphics.Color(0xFF666666), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val skills = c.skills.take(2)
                    skills.forEach { skill ->
                        Box(modifier = Modifier
                            .padding(end = 6.dp)
                            .background(androidx.compose.ui.graphics.Color(0xFFF0F5FF))) {
                            Text(skill, color = themeColor, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                        }
                    }
                    val extra = c.skills.size - skills.size
                    if (extra > 0) Text("+${extra}", color = themeColor, style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("￥${c.price} / 次", color = themeColor, style = MaterialTheme.typography.titleSmall)
                Text("${c.durationMinutes} 分钟", color = androidx.compose.ui.graphics.Color(0xFF999999), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBook, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                    Text("立即预约", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
                }
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
