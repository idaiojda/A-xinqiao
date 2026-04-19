package com.example.xinqiao.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.xinqiao.consultation.pro.ConsultRepository
import com.example.xinqiao.consultation.pro.Certificate
import com.example.xinqiao.consultation.pro.CaseItem
import com.example.xinqiao.consultation.pro.ConsultantDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConsultantDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cid = intent.getStringExtra("consultantId") ?: ""
        setContent { ConsultantDetailScreen(cid, onBack = { finish() }) }
    }
}

private fun readToken(ctx: Context): String? {
    val sp = ctx.getSharedPreferences("loginInfo", Context.MODE_PRIVATE)
    return sp.getString("auth_token", null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsultantDetailScreen(consultantId: String, onBack: () -> Unit) {
    val themeColor = Color(0xFF7B68EE)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var detail by remember { mutableStateOf<ConsultantDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dialogCert by remember { mutableStateOf<Certificate?>(null) }
    
    val token = remember { readToken(ctx) }
    val repository = remember { ConsultRepository(ctx) }
    
    LaunchedEffect(consultantId) {
        loading = true
        error = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.fetchConsultantDetail(consultantId, token)
            }
            result.onSuccess {
                detail = it
                loading = false
            }.onFailure {
                error = it.message ?: "加载失败"
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
                title = { Text("咨询师详情", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } }
            )
        },
        bottomBar = {
            if (detail != null) {
                BottomActionBar(
                    price = detail!!.price,
                    defaultMode = detail!!.defaultMode,
                    onBook = {
                        val it = Intent(ctx, AppointmentDetailActivity::class.java)
                        it.putExtra("consultantId", detail!!.id)
                        it.putExtra("name", detail!!.name)
                        it.putExtra("mode", detail!!.defaultMode)
                        it.putExtra("price", detail!!.price)
                        it.putExtra("priceText", detail!!.priceText)
                        it.putExtra("priceVoice", detail!!.priceVoice)
                        it.putExtra("priceVideo", detail!!.priceVideo)
                        ctx.startActivity(it)
                    }
                )
            }
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = themeColor)
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("加载失败", fontSize = 16.sp, color = Color(0xFF666666))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, fontSize = 14.sp, color = Color(0xFF999999))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            val result = withContext(Dispatchers.IO) {
                                repository.fetchConsultantDetail(consultantId, token)
                            }
                            result.onSuccess {
                                detail = it
                                loading = false
                            }.onFailure {
                                error = it.message ?: "加载失败"
                                loading = false
                            }
                        }
                    }) {
                        Text("重试")
                    }
                }
            }
        } else if (detail != null) {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(bottom = 84.dp)
            ) {
                item { HeaderBanner(detail!!) }
                item { Spacer(modifier = Modifier.height(60.dp)) }
                item { PersonalInfoSection(detail!!) }
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item { IntroSection(detail!!.intro) }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            if (dialogCert != null) {
                AlertDialog(
                    onDismissRequest = { dialogCert = null },
                    confirmButton = {
                        TextButton(onClick = { dialogCert = null }) { Text("我知道了") }
                    },
                    title = { Text(dialogCert!!.title) },
                    text = { Text(dialogCert!!.desc) }
                )
            }
        }
    }
}

@Composable
private fun HeaderBanner(detail: ConsultantDetail) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)) {
        // 背景证书图片
        AsyncImage(
            model = "https://picsum.photos/seed/cert${detail.id}/900/600",
            contentDescription = "证书背景",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // 半透明遮罩
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
        )
        
        // 中间的证书信息
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 证书图标或小图
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                // 处理 base64 图片
                val avatarUrl = detail.avatarUrl
                if (avatarUrl?.startsWith("data:image/") == true) {
                    // Base64 图片：解码并显示
                    val base64Data = avatarUrl.substringAfter("base64,")
                    val bitmap = remember(base64Data) {
                        try {
                            val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "证书",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        // 解码失败，使用 AsyncImage
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "证书",
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // URL 图片：使用 AsyncImage
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "证书",
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "心理咨询师基础培训合格证书",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        // 底部咨询师信息卡片
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = 40.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    detail.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    detail.title,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(12.dp))
                // 城市标签
                if (!detail.city.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            detail.city,
                            fontSize = 13.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(detail: ConsultantDetail) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "个人信息",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // 学历
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "学历",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
                Text(
                    detail.education,
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 工作年限
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "工作年限",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
                Text(
                    detail.workYear,
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 擅长领域
            if (detail.skills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "擅长领域",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 使用FlowRow显示标签
                com.google.accompanist.flowlayout.FlowRow(
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    detail.skills.forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF0F4FF),
                            modifier = Modifier
                        ) {
                            Text(
                                text = skill,
                                fontSize = 13.sp,
                                color = Color(0xFF5B7FFF),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CertificateSection(certificates: List<Certificate>, onClick: (Certificate) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("资质证书", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            certificates.forEach { c ->
                ElevatedSuggestionChip(onClick = { onClick(c) }, label = { Text(c.title) })
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun CasesSection(cases: List<CaseItem>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("典型案例", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        cases.forEach { it ->
            CaseCard(it)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CaseCard(it: CaseItem) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(it.title, fontWeight = FontWeight.SemiBold)
            Text(it.time, color = Color(0xFF888888), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(it.summary, color = Color(0xFF444444))
        }
    }
}

@Composable
private fun ReviewDistributionSection(dist: List<Int>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("评价分布", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        val max = dist.maxOrNull()?.coerceAtLeast(1) ?: 1
        val labels = listOf("5星", "4星", "3星", "2星", "1星")
        dist.forEachIndexed { idx, v ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(labels[idx], modifier = Modifier.width(36.dp))
                Box(modifier = Modifier
                    .height(10.dp)
                    .width((v * 200f / max).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF69C0FF)))
                Spacer(modifier = Modifier.width(8.dp))
                Text("$v")
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun IntroSection(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "咨询师简介",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun BottomActionBar(price: Int, defaultMode: String, onBook: () -> Unit) {
    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "¥$price",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B)
                    )
                    Text(
                        " /次",
                        fontSize = 14.sp,
                        color = Color(0xFF999999)
                    )
                }
                Text(
                    "方式：$defaultMode",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            Button(
                onClick = onBook,
                modifier = Modifier
                    .height(44.dp)
                    .width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5B7FFF)
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    "立即预约",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
