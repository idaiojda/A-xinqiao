package com.example.xinqiao.community

/**
 * 仓库提供者：默认使用本地假数据，可在初始化时替换为远程仓库。
 */
object CommunityRepositoryProvider {
    @Volatile
    lateinit var current: CommunityRepository
}
