package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.BlockRelation
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.FollowRelation
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.Story
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MeraInstaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pehle Google se Login karein.")
    }

    // --- USERS & PROFILES ---

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, "users", uid) }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> = runCatching {
        requireUserId()
        val doc = db.collection("users").document(userId).get().await()
        if (doc.exists()) {
            doc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else null
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, "users/$userId", auth)
    }

    suspend fun upsertUserProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarBase64: String,
        isOnline: Boolean = true
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanUsername = username.trim().lowercase().replace(" ", "_").take(30)
            .let { if (it.length < 2) "user_${uid.take(6)}" else it }
        val cleanDisplayName = displayName.trim().ifBlank { cleanUsername }.take(50)
        val docRef = db.collection("users").document(uid)
        val existing = docRef.get().await()
        if (existing.exists()) {
            val updates = mutableMapOf<String, Any>(
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "bio" to bio.take(200),
                "isOnline" to isOnline,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (avatarBase64.isNotEmpty()) {
                updates["avatarBase64"] = avatarBase64
            }
            docRef.update(updates).await()
        } else {
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "bio" to bio.take(200),
                "isOnline" to isOnline,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (avatarBase64.isNotEmpty()) {
                payload["avatarBase64"] = avatarBase64
            }
            docRef.set(payload).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}", auth)
    }

    suspend fun setOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "isOnline" to isOnline,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid", auth)
        }
    }

    // --- FOLLOW & BLOCK SYSTEM ---

    fun observeAllFollows(): Flow<List<FollowRelation>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("follows")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FollowRelation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, "follows", uid) }
        )
    }

    fun observeMyBlocks(): Flow<List<BlockRelation>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("blocks")
                .whereEqualTo("blockerId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BlockRelation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, "blocks", uid) }
        )
    }

    suspend fun followUser(targetUserId: String, myUsername: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(uid != targetUserId) { "Khud ko follow nahi kar sakte" }
        val followId = "${uid}_${targetUserId}"
        val payload = mapOf(
            "followerId" to uid,
            "followingId" to targetUserId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("follows").document(followId).set(payload).await()

        // Send notification: "Rahul ne tumhe Follow kiya"
        createNotification(
            recipientId = targetUserId,
            senderUsername = myUsername,
            type = "follow",
            message = "$myUsername ne tumhe Follow kiya"
        )
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "follows/${auth.currentUser?.uid}_$targetUserId", auth)
    }

    suspend fun unfollowUser(targetUserId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val followId = "${uid}_${targetUserId}"
        db.collection("follows").document(followId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "follows/${auth.currentUser?.uid}_$targetUserId", auth)
    }

    suspend fun blockUser(targetUserId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(uid != targetUserId) { "Khud ko block nahi kar sakte" }
        val blockId = "${uid}_${targetUserId}"
        val payload = mapOf(
            "blockerId" to uid,
            "blockedId" to targetUserId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("blocks").document(blockId).set(payload).await()
        // Also remove follow if following
        try {
            db.collection("follows").document("${uid}_${targetUserId}").delete().await()
        } catch (_: Exception) { }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "blocks/${auth.currentUser?.uid}_$targetUserId", auth)
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val blockId = "${uid}_${targetUserId}"
        db.collection("blocks").document(blockId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "blocks/${auth.currentUser?.uid}_$targetUserId", auth)
    }

    // --- POSTS & REELS ---

    fun observeAllPosts(): Flow<List<Post>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("posts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Post::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, "posts", uid) }
        )
    }

    suspend fun createPost(
        username: String,
        userAvatarBase64: String,
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
        accentColorHex: String = "#E1306C"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("posts").document()
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "username" to username,
            "caption" to caption.take(1000),
            "location" to location.take(100),
            "taggedUsers" to taggedUsers.take(200),
            "filterName" to filterName.take(30),
            "mediaType" to mediaType,
            "isReel" to isReel,
            "musicName" to musicName.take(100),
            "reelDurationSec" to reelDurationSec,
            "overlayText" to overlayText.take(200),
            "accentColorHex" to accentColorHex,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (userAvatarBase64.isNotEmpty()) payload["userAvatarBase64"] = userAvatarBase64
        if (mediaBase64.isNotEmpty()) payload["mediaBase64"] = mediaBase64
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts", auth)
    }

    fun observePostLikes(postId: String): Flow<List<String>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/likes"
        emitAll(
            db.collection("posts").document(postId).collection("likes")
                .snapshots()
                .map { snapshot -> snapshot.documents.map { it.id } }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    suspend fun toggleLike(post: Post, myUsername: String, currentlyLiked: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(post.id)
        val likeRef = postRef.collection("likes").document(uid)
        if (currentlyLiked) {
            likeRef.delete().await()
            if (post.likesCount > 0) {
                postRef.update(
                    mapOf(
                        "likesCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } else {
            likeRef.set(
                mapOf(
                    "userId" to uid,
                    "username" to myUsername,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            postRef.update(
                mapOf(
                    "likesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (post.userId != uid) {
                createNotification(
                    recipientId = post.userId,
                    senderUsername = myUsername,
                    type = "like",
                    message = "$myUsername ne tumhara Post Like kiya",
                    postId = post.id
                )
            }
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}", auth)
    }

    fun observeComments(postId: String): Flow<List<PostComment>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PostComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    suspend fun addComment(post: Post, myUsername: String, text: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanText = text.trim()
        require(cleanText.isNotEmpty()) { "Comment khaali nahi ho sakta" }
        val postRef = db.collection("posts").document(post.id)
        val commentRef = postRef.collection("comments").document()
        commentRef.set(
            mapOf(
                "userId" to uid,
                "username" to myUsername,
                "text" to cleanText.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        postRef.update(
            mapOf(
                "commentsCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        if (post.userId != uid) {
            createNotification(
                recipientId = post.userId,
                senderUsername = myUsername,
                type = "comment",
                message = "$myUsername ne comment kiya: ${cleanText.take(40)}",
                postId = post.id
            )
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments", auth)
    }

    // --- STORIES (24H) ---

    fun observeActiveStories(): Flow<List<Story>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("stories")
                .snapshots()
                .map { snapshot ->
                    val now = System.currentTimeMillis()
                    snapshot.toObjects(
                        Story::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.expiresAtMillis > now }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, "stories", uid) }
        )
    }

    suspend fun createStory(
        username: String,
        userAvatarBase64: String,
        caption: String,
        mediaBase64: String,
        bgColorHex: String = "#E1306C"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("stories").document()
        val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "username" to username,
            "caption" to caption.take(300),
            "bgColorHex" to bgColorHex,
            "expiresAtMillis" to expiresAt,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (userAvatarBase64.isNotEmpty()) payload["userAvatarBase64"] = userAvatarBase64
        if (mediaBase64.isNotEmpty()) payload["mediaBase64"] = mediaBase64
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "stories", auth)
    }

    // --- NOTIFICATIONS ---

    fun observeNotifications(): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AppNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, "notifications", uid) }
        )
    }

    suspend fun markNotificationRead(notificationId: String) {
        try {
            requireUserId()
            db.collection("notifications").document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId", auth)
        }
    }

    private suspend fun createNotification(
        recipientId: String,
        senderUsername: String,
        type: String,
        message: String,
        postId: String = ""
    ) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val payload = mutableMapOf<String, Any>(
                "recipientId" to recipientId,
                "senderId" to uid,
                "senderUsername" to senderUsername,
                "type" to type,
                "message" to message.take(200),
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (postId.isNotEmpty()) payload["postId"] = postId
            db.collection("notifications").document().set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "notifications", auth)
        }
    }

    // --- CHATS / DM ---

    fun chatIdFor(uidA: String, uidB: String): String =
        if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"

    fun observeMyChats(): Flow<List<ChatThread>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("chats")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, "chats", uid) }
        )
    }

    fun observeMessages(otherUserId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val chatId = chatIdFor(uid, otherUserId)
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    suspend fun sendMessage(
        recipientId: String,
        myUsername: String,
        type: String,
        text: String = "",
        mediaBase64: String = "",
        voiceDurationSec: Long = 0L
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val chatId = chatIdFor(uid, recipientId)
        val participants = listOf(uid, recipientId).sorted()
        val chatRef = db.collection("chats").document(chatId)
        val summary = when (type) {
            "photo" -> "📷 Photo bheja"
            "video" -> "🎥 Video bheja"
            "voice" -> "🎤 Voice Message (${voiceDurationSec}s)"
            else -> text.take(100)
        }

        val existingChat = chatRef.get().await()
        if (existingChat.exists()) {
            chatRef.update(
                mapOf(
                    "lastMessage" to summary,
                    "lastSenderId" to uid,
                    "typingUserIds" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            chatRef.set(
                mapOf(
                    "participantIds" to participants,
                    "lastMessage" to summary,
                    "lastSenderId" to uid,
                    "typingUserIds" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val msgPayload = mutableMapOf<String, Any>(
            "senderId" to uid,
            "recipientId" to recipientId,
            "participantIds" to participants,
            "type" to type,
            "text" to text.take(1000),
            "voiceDurationSec" to voiceDurationSec,
            "isSeen" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (mediaBase64.isNotEmpty()) msgPayload["mediaBase64"] = mediaBase64
        chatRef.collection("messages").document().set(msgPayload).await()

        createNotification(
            recipientId = recipientId,
            senderUsername = myUsername,
            type = "message",
            message = "Naya Message aaya: $myUsername - $summary"
        )
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "chats", auth)
    }

    suspend fun markMessagesSeen(otherUserId: String, unreadMessageIds: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        if (unreadMessageIds.isEmpty()) return
        val chatId = chatIdFor(uid, otherUserId)
        unreadMessageIds.forEach { msgId ->
            try {
                db.collection("chats").document(chatId)
                    .collection("messages").document(msgId)
                    .update("isSeen", true)
                    .await()
            } catch (_: Exception) { }
        }
    }

    private fun handleStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
        throw error
    }
}
