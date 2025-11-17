package com.example.xinqiao.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

sealed class CommunityUiState {
    object List : CommunityUiState()
    object AnonymousPost : CommunityUiState()
    data class Group(val name: String) : CommunityUiState()
    object CreateGroup : CommunityUiState()
    data class PostDetail(val post: ThemePost) : CommunityUiState()
}

data class Question(val id: String, val title: String, val content: String)
data class Comment(val id: String, val author: String, val text: String)

class CommunityController {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    var uiState: CommunityUiState by mutableStateOf(CommunityUiState.List)
    var selectedTab: Int by mutableStateOf(0)
    var searchText: String by mutableStateOf("")
    var comfortModeOn: Boolean by mutableStateOf(true)
    var showFabSheet: Boolean by mutableStateOf(false)
    var selectedCategoryIndex: Int by mutableStateOf(0)

    var isLoading: Boolean by mutableStateOf(false)
    var errorMessage: String? by mutableStateOf(null)
    var groups: List<String> by mutableStateOf(emptyList())

    private val backStack = mutableStateListOf<CommunityUiState>()
    private fun navigate(to: CommunityUiState) { backStack.add(uiState); uiState = to }

    fun openAnonymousPost() { navigate(CommunityUiState.AnonymousPost) }
    fun openGroup(name: String) { navigate(CommunityUiState.Group(name)) }
    fun openCreateGroup() { navigate(CommunityUiState.CreateGroup) }
    fun openPost(post: ThemePost) { navigate(CommunityUiState.PostDetail(post)) }
    fun selectTab(index: Int) { selectedTab = index }
    fun updateSearch(text: String) { searchText = text }

    fun canGoBack(): Boolean = backStack.isNotEmpty()
    fun goBack() { if (backStack.isNotEmpty()) { uiState = backStack.removeAt(backStack.lastIndex) } else { uiState = CommunityUiState.List } }
    fun handleBackPressed(): Boolean { return if (canGoBack()) { goBack(); true } else false }

    fun toggleComfortMode() { comfortModeOn = !comfortModeOn }
    fun openFabSheet() { showFabSheet = true }
    fun closeFabSheet() { showFabSheet = false }
    fun selectCategory(index: Int) { selectedCategoryIndex = index }

    private val commentsByPost = androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<Comment>>()
    private val commentsVisible = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    private val membership = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    var groupsVersion: Int by mutableStateOf(0)

    fun getComments(postId: String): List<Comment> = commentsByPost[postId] ?: emptyList()
    fun isCommentsVisible(postId: String): Boolean = commentsVisible[postId] == true
    fun toggleComments(postId: String) { commentsVisible[postId] = !(commentsVisible[postId] ?: false) }
    fun setComments(postId: String, list: List<Comment>) {
        val state = commentsByPost.getOrPut(postId) { androidx.compose.runtime.mutableStateListOf() }
        state.clear(); state.addAll(list)
    }

    fun addComment(postId: String, text: String, author: String = if (comfortModeOn) "匿名用户" else "我") {
        val list = commentsByPost.getOrPut(postId) { androidx.compose.runtime.mutableStateListOf() }
        val cid = "$postId-${System.currentTimeMillis()}"
        list.add(Comment(id = cid, author = author, text = text))
        scope.launch {
            try {
                CommunityRepositoryProvider.current.postPostComment(postId, text, author)
            } catch (_: Exception) { }
        }
    }

    fun isJoined(groupName: String): Boolean = membership[groupName] == true
    fun setJoined(groupName: String, joined: Boolean) { membership[groupName] = joined; groupsVersion++ }
}
