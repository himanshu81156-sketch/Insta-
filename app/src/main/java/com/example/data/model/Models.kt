package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarBase64: String = "",
    val isOnline: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowRelation(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followingId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockRelation(
    @DocumentId val id: String = "",
    val blockerId: String = "",
    val blockedId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarBase64: String = "",
    val caption: String = "",
    val location: String = "",
    val taggedUsers: String = "",
    val filterName: String = "Normal",
    val mediaType: String = "photo",
    val mediaBase64: String = "",
    val isReel: Boolean = false,
    val musicName: String = "",
    val reelDurationSec: Long = 15L,
    val overlayText: String = "",
    val accentColorHex: String = "#E1306C",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostLike(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarBase64: String = "",
    val caption: String = "",
    val mediaBase64: String = "",
    val bgColorHex: String = "#E1306C",
    val expiresAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val type: String = "follow",
    val message: String = "",
    val postId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val typingUserIds: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val participantIds: List<String> = emptyList(),
    val type: String = "text",
    val text: String = "",
    val mediaBase64: String = "",
    val voiceDurationSec: Long = 0L,
    val isSeen: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
