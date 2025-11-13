package com.example.xinqiao.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

class ConsultantDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cid = intent.getStringExtra("consultantId") ?: ""
        setContent { ConsultantDetailScreen(cid, onBack = { finish() }) }
    }
}

private data class Certificate(val title: String, val desc: String)
private data class CaseItem(val title: String, val time: String, val summary: String)
private data class ConsultantDetail(
    val id: String,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val rating: Double,
    val consultCount: Int,
    val price: Int,
    val defaultMode: String,
    val city: String?,
    val skills: List<String>,
    val certificates: List<Certificate>,
    val cases: List<CaseItem>,
    val reviewDist: List<Int>, // 5→1 星的数量
    val intro: String,
)

private fun loadConsultantDetail(id: String): ConsultantDetail {
    return ConsultantDetail(
        id = id,
        name = "王心怡",
        title = "国家二级心理咨询师 / 家庭治疗师",
        avatarUrl = "https://picsum.photos/seed/$id/300/300",
        bannerUrl = "https://picsum.photos/seed/${id}b/900/600",
        rating = 4.8,
        consultCount = 326,
        price = 299,
        defaultMode = "文字咨询",
        city = "上海",
        skills = listOf("亲密关系", "家庭系统", "焦虑抑郁", "自我成长"),
        certificates = listOf(
            Certificate("国家二级咨询师", "人社部国家二级心理咨询师资格证书，编号 20XXXXXXXX"),
            Certificate("婚姻家庭咨询", "完成结构式家庭治疗系统训练 120 学时，导师：XXX"),
            Certificate("认知行为治疗", "CBT 系统训练与督导，案例通过导师评审")
        ),
        cases = listOf(
            CaseItem("亲密关系沟通重建", "2024-06-18", "伴侣沟通困境，采用结构式家庭治疗 + 共情训练，六次会谈达成稳定沟通模式。"),
            CaseItem("职场焦虑疏解", "2024-05-02", "高压环境下持续焦虑，CBT 重构负性自动思维，三周练习显著改善睡眠与专注。")
        ),
        reviewDist = listOf(42, 18, 6, 3, 1),
        intro = "10 年从业经验，专注亲密关系与家庭系统治疗；擅长结合 CBT 与结构式家庭治疗进行综合干预。倡导循证与稳定节奏，重视家庭成员间边界与功能重建。"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsultantDetailScreen(consultantId: String, onBack: () -> Unit) {
    val themeColor = Color(0xFF2F54EB)
    val ctx = LocalContext.current
    val detail by remember(consultantId) { mutableStateOf(loadConsultantDetail(consultantId)) }
    var dialogCert by remember { mutableStateOf<Certificate?>(null) }

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
            BottomActionBar(
                price = detail.price,
                defaultMode = detail.defaultMode,
                onBook = {
                    val it = Intent(ctx, AppointmentDetailActivity::class.java)
                    it.putExtra("consultantId", detail.id)
                    it.putExtra("name", detail.name)
                    it.putExtra("mode", detail.defaultMode)
                    it.putExtra("price", detail.price)
                    ctx.startActivity(it)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 84.dp)
        ) {
            item { HeaderBanner(detail) }
            item { CoreInfoSection(detail) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { CertificateSection(detail.certificates, onClick = { dialogCert = it }) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { CasesSection(detail.cases) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { ReviewDistributionSection(detail.reviewDist) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { IntroSection(detail.intro) }
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

@Composable
private fun HeaderBanner(detail: ConsultantDetail) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)) {
        AsyncImage(
            model = detail.bannerUrl ?: detail.avatarUrl,
            contentDescription = "banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x88000000)),
                    startY = 50f, endY = Float.POSITIVE_INFINITY
                )
            )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            AsyncImage(
                model = detail.avatarUrl,
                contentDescription = "avatar",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(detail.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE6F0FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "认证咨询师",
                            color = Color(0xFF2C6ECB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(detail.title, color = Color(0xFFEEEEEE), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC53D), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${detail.rating} · ${detail.consultCount}次咨询", color = Color(0xFFEEEEEE), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CoreInfoSection(detail: ConsultantDetail) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val city = detail.city ?: ""
            if (city.isNotBlank()) {
                AssistChip(onClick = {}, label = { Text(city) })
                Spacer(modifier = Modifier.width(8.dp))
            }
            detail.skills.take(4).forEach { s ->
                AssistChip(onClick = {}, label = { Text(s) })
                Spacer(modifier = Modifier.width(8.dp))
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
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("更多介绍", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起" else "展开更多") }
    }
}

@Composable
private fun BottomActionBar(price: Int, defaultMode: String, onBook: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("¥$price / 次", fontWeight = FontWeight.SemiBold)
                Text("默认：$defaultMode", color = Color(0xFF666666), fontSize = 12.sp)
            }
            Button(onClick = onBook) { Text("立即预约") }
        }
    }
}
