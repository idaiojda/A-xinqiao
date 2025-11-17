package com.example.xinqiao.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun RecommendedGroupCardNew(
    controller: CommunityController
) {
    val tokens = CommunityTokensInstance
    var groups by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadErr by remember { mutableStateOf<String?>(null) }
    var applyMessage by remember { mutableStateOf<String?>(null) }
    var isApplying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var recommendedName by remember { mutableStateOf<String?>(null) }
    var recommendedDesc by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val remote = CommunityRepositoryProvider.current.getGroups()
            groups = remote
        } catch (e: Exception) {
            groups = emptyList()
            loadErr = "加载推荐小组失败：" + (e.message ?: "网络异常")
        }
    }

    LaunchedEffect(groups) {
        if (groups.isNotEmpty()) {
            var pick = groups.first()
            try {
                val candidates = groups.take(5)
                val counts = mutableListOf<Pair<String, Int>>()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    candidates.forEach { g ->
                        val c = try { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupMemberCount(g) } catch (_: Exception) { 0 }
                        counts.add(g to c)
                    }
                }
                pick = counts.maxByOrNull { it.second }?.first ?: pick
            } catch (_: Exception) { }
            recommendedName = pick
            try {
                val info = CommunityRepositoryProvider.current.getGroupInfo(pick)
                val d2 = info.schedule
                val d3 = info.frequency
                recommendedDesc = listOf(d2, d3).firstOrNull { !it.isNullOrBlank() } ?: ""
                if (recommendedDesc.isBlank()) {
                    val d = try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupInfo(pick)?.description ?: ""
                        }
                    } catch (_: Exception) { "" }
                    recommendedDesc = if (d.isNotBlank()) d else "加入一起坚持与互助"
                }
            } catch (_: Exception) {
                val d = try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupInfo(pick)?.description ?: ""
                    }
                } catch (_: Exception) { "" }
                recommendedDesc = if (d.isNotBlank()) d else "加入一起坚持与互助"
            }
        } else {
            recommendedName = null
            recommendedDesc = ""
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
                text = if (recommendedName != null) "推荐加入：$recommendedName" else "暂无推荐小组",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = tokens.type.CardTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.color.Neutral900
                )
            )
            Text(
                text = if (recommendedDesc.isNotBlank()) recommendedDesc else "",
                style = MaterialTheme.typography.bodySmall.copy(color = tokens.color.Neutral700)
            )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.M)
        ) {
            Button(
                onClick = {
                    val name = recommendedName ?: return@Button
                    scope.launch {
                        isApplying = true
                        try {
                            val ok = CommunityRepositoryProvider.current.setGroupJoin(name, true)
                                if (!ok) {
                                    try {
                                        val dao = CommunityLocalCache.database()?.groupDao()
                                        val cur = dao?.get(name)
                                        val rules = cur?.rulesJson ?: com.google.gson.Gson().toJson(listOf("友善沟通", "禁止外传", "支持鼓励"))
                                        val admin = cur?.adminName ?: ""
                                        val freq = cur?.frequency ?: ""
                                        val sched = cur?.schedule ?: ""
                                        val mc = cur?.memberCount ?: 0
                                        dao?.upsert(GroupInfoEntity(name = name, memberCount = mc, rulesJson = rules, joined = true, adminName = admin, frequency = freq, schedule = sched))
                                    } catch (_: Exception) { }
                                }
                                try {
                                    val user = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx) ?: ""
                                    if (user.isNotBlank()) {
                                        val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                                        val raw = sp.getString("joinedGroups_" + user, "[]")
                                        val arr = try { com.google.gson.Gson().fromJson(raw, java.util.ArrayList::class.java) as MutableList<String> } catch (_: Exception) { mutableListOf() }
                                        if (!arr.contains(name)) arr.add(name)
                                        sp.edit().putString("joinedGroups_" + user, com.google.gson.Gson().toJson(arr)).apply()
                                    }
                                } catch (_: Exception) { }
                                controller.setJoined(name, true)
                                applyMessage = "已加入"
                            } catch (e: Exception) {
                                applyMessage = "加入失败：" + (e.message ?: "网络异常")
                            } finally {
                                isApplying = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(tokens.corner.Button),
                    enabled = !isApplying && recommendedName != null
                ) {
                    Text(if (isApplying) "处理中…" else "加入")
                }
                OutlinedButton(
                    onClick = {
                        val n = recommendedName ?: return@OutlinedButton
                        controller.openGroup(n)
                    },
                    shape = RoundedCornerShape(tokens.corner.Button),
                    enabled = recommendedName != null
                ) {
                    Text("查看介绍")
                }
            }
            applyMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = tokens.color.Neutral700)
            }
            loadErr?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = tokens.color.Danger)
            }
        }
    }
}
