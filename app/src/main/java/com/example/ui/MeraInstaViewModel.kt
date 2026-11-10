package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.BlockRelation
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.FollowRelation
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.Story
import com.example.data.model.UserProfile
import com.example.data.repository.MeraInstaRepository
import com.example.data.toHindiErrorMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeraInstaViewModel(
    private val repository: MeraInstaRepository,
    val currentUserId: String
) : ViewModel() {

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _savedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIds: StateFlow<Set<String>> = _savedPostIds.asStateFlow()

    private val _hindiToastMessage = MutableStateFlow<String?>(null)
    val hindiToastMessage: StateFlow<String?> = _hindiToastMessage.asStateFlow()

    private val _feedOnlyFollowing = MutableStateFlow(false)
    val feedOnlyFollowing: StateFlow<Boolean> = _feedOnlyFollowing.asStateFlow()

    val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsers()
        .catch { e -> handleFlowError("observeAllUsers", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val currentUserProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { e -> handleFlowError("observeCurrentUser", e); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val allFollows: StateFlow<List<FollowRelation>> = repository.observeAllFollows()
        .catch { e -> handleFlowError("observeAllFollows", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myBlocks: StateFlow<List<BlockRelation>> = repository.observeMyBlocks()
        .catch { e -> handleFlowError("observeMyBlocks", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allPosts: StateFlow<List<Post>> = repository.observeAllPosts()
        .catch { e -> handleFlowError("observeAllPosts", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeStories: StateFlow<List<Story>> = repository.observeActiveStories()
        .catch { e -> handleFlowError("observeActiveStories", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications()
        .catch { e -> handleFlowError("observeNotifications", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myChats: StateFlow<List<ChatThread>> = repository.observeMyChats()
        .catch { e -> handleFlowError("observeMyChats", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val homeFeedPosts: StateFlow<List<Post>> = combine(
        allPosts,
        allFollows,
        myBlocks,
        _feedOnlyFollowing
    ) { posts, follows, blocks, onlyFollowing ->
        val blockedIds = blocks.map { it.blockedId }.toSet()
        val followingIds = follows.filter { it.followerId == currentUserId }.map { it.followingId }.toSet()
        posts.filter { post ->
            !post.isReel &&
                post.userId !in blockedIds &&
                (!onlyFollowing || post.userId == currentUserId || post.userId in followingIds)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reelsList: StateFlow<List<Post>> = combine(allPosts, myBlocks) { posts, blocks ->
        val blockedIds = blocks.map { it.blockedId }.toSet()
        posts.filter { it.isReel && it.userId !in blockedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.setOnlineStatus(true)
        }
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setFeedFilter(onlyFollowing: Boolean) {
        _feedOnlyFollowing.value = onlyFollowing
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(650)
            _isRefreshing.value = false
            _hindiToastMessage.value = "Feed update ho gaya!"
        }
    }

    fun clearMessage() {
        _hindiToastMessage.value = null
    }

    fun toggleSavePost(postId: String) {
        val current = _savedPostIds.value
        if (postId in current) {
            _savedPostIds.value = current - postId
            _hindiToastMessage.value = "Saved se hata diya gaya."
        } else {
            _savedPostIds.value = current + postId
            _hindiToastMessage.value = "Post Save ho gaya!"
        }
    }

    fun saveProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarBase64: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.upsertUserProfile(
                username = username,
                displayName = displayName,
                bio = bio,
                avatarBase64 = avatarBase64,
                isOnline = true
            )
            res.onSuccess {
                _hindiToastMessage.value = "Profile save ho gaya!"
                onComplete()
            }.onFailure { e ->
                _hindiToastMessage.value = toHindiErrorMessage(e)
            }
        }
    }

    fun setOnlineStatus(online: Boolean) {
        viewModelScope.launch {
            repository.setOnlineStatus(online)
        }
    }

    fun followOrUnfollow(targetUserId: String, currentlyFollowing: Boolean) {
        viewModelScope.launch {
            val myName = currentUserProfile.value?.username ?: "User"
            val res = if (currentlyFollowing) {
                repository.unfollowUser(targetUserId)
            } else {
                repository.followUser(targetUserId, myName)
            }
            res.onSuccess {
                _hindiToastMessage.value = if (currentlyFollowing) "Unfollow kar diya." else "Follow kar liya!"
            }.onFailure { e ->
                _hindiToastMessage.value = toHindiErrorMessage(e)
            }
        }
    }

    fun blockOrUnblock(targetUserId: String, currentlyBlocked: Boolean) {
        viewModelScope.launch {
            val res = if (currentlyBlocked) {
                repository.unblockUser(targetUserId)
            } else {
                repository.blockUser(targetUserId)
            }
            res.onSuccess {
                _hindiToastMessage.value = if (currentlyBlocked) "Unblock kar diya." else "User ko Block kar diya."
            }.onFailure { e ->
                _hindiToastMessage.value = toHindiErrorMessage(e)
            }
        }
    }

    fun publishPost(
        caption: String,
        location: String,
        taggedUsers: String,
        filterName: String,
        mediaType: String,
        mediaBase64: String,
        isReel: Boolean,
        musicName: String = "",
        reelDurationSec: Long = 15L,
        overlayText: String = "",
        accentColorHex: String = "#E1306C",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val me = currentUserProfile.value
            val username = me?.username ?: "user_${currentUserId.take(5)}"
            val avatar = me?.avatarBase64 ?: ""
            val res = repository.createPost(
                username = username,
                userAvatarBase64 = avatar,
                caption = caption,
                location = location,
                taggedUsers = taggedUsers,
                filterName = filterName,
                mediaType = mediaType,
                mediaBase64 = mediaBase64,
                isReel = isReel,
                musicName = musicName,
                reelDurationSec = reelDurationSec,
                overlayText = overlayText,
                accentColorHex = accentColorHex
            )
            res.onSuccess {
                _hindiToastMessage.value = if (isReel) "Reel publish ho gayi!" else "Post publish ho gaya!"
                onSuccess()
            }.onFailure { e ->
                _hindiToastMessage.value = toHindiErrorMessage(e)
            }
        }
    }

    fun publishStory(caption: String, mediaBase64: String, bgColorHex: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val me = currentUserProfile.value
            val username = me?.username ?: "user_${currentUserId.take(5)}"
            val avatar = me?.avatarBase64 ?: ""
            val res = repository.createStory(username, avatar, caption, mediaBase64, bgColorHex)
            res.onSuccess {
                _hindiToastMessage.value = "24 ghante wali Story lag gayi!"
                onSuccess()
            }.onFailure { e ->
                _hindiToastMessage.value = toHindiErrorMessage(e)
            }
        }
    }

    fun observePostLikes(postId: String): Flow<List<String>> =
        repository.observePostLikes(postId).catch { emit(emptyList()) }

    fun toggleLike(post: Post, currentlyLiked: Boolean) {
        viewModelScope.launch {
            val myName = currentUserProfile.value?.username ?: "User"
            repository.toggleLike(post, myName, currentlyLiked).onFailure { e ->
                _hindiToastMessage.value = toHindiErrorMessage(e)
            }
        }
    }

    fun observeComments(postId: String): Flow<List<PostComment>> =
        repository.observeComments(postId).catch { emit(emptyList()) }

    fun addComment(post: Post, text: String) {
        viewModelScope.launch {
            val myName = currentUserProfile.value?.username ?: "User"
            repository.addComment(post, myName, text).onFailure { e ->
                _hindiToastMessage.value = toHindiErrorMessage(e)
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun observeMessages(otherUserId: String): Flow<List<ChatMessage>> =
        repository.observeMessages(otherUserId).catch { emit(emptyList()) }

    fun sendMessage(
        recipientId: String,
        type: String,
        text: String = "",
        mediaBase64: String = "",
        voiceDurationSec: Long = 0L
    ) {
        viewModelScope.launch {
            val myName = currentUserProfile.value?.username ?: "User"
            repository.sendMessage(recipientId, myName, type, text, mediaBase64, voiceDurationSec)
                .onFailure { e -> _hindiToastMessage.value = toHindiErrorMessage(e) }
        }
    }

    fun markMessagesSeen(otherUserId: String, unreadIds: List<String>) {
        viewModelScope.launch {
            repository.markMessagesSeen(otherUserId, unreadIds)
        }
    }

    private fun handleFlowError(tag: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("MeraInstaVM", "$tag error", rootError)
        _hindiToastMessage.value = toHindiErrorMessage(rootError)
    }
}
