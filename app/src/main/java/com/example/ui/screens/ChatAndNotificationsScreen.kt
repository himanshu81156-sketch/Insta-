package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.ui.MeraInstaViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.InstaCoral
import com.example.ui.theme.OnlineGreen
import com.example.ui.util.ImageUtils

@Composable
fun NotificationsScreen(
    viewModel: MeraInstaViewModel,
    onBack: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    BackHandler { onBack() }
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche")
            }
            Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Abhi koi naya notification nahi hai.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    LaunchedEffect(notif.id) {
                        if (!notif.isRead) viewModel.markNotificationRead(notif.id)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProfileClick(notif.senderId) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(avatarBase64 = "", username = notif.senderUsername, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.message, fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold)
                                Text(notif.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = InstaCoral)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListScreen(
    viewModel: MeraInstaViewModel,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    BackHandler { onBack() }
    val chats by viewModel.myChats.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val otherUsers = remember(users, viewModel.currentUserId) {
        users.filter { it.userId != viewModel.currentUserId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche")
            }
            Text("Messages / Chat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (chats.isNotEmpty()) {
                item {
                    Text("Aapki Baatcheet", style = MaterialTheme.typography.labelLarge, color = InstaCoral, fontWeight = FontWeight.Bold)
                }
                items(chats, key = { it.id }) { thread ->
                    val otherId = thread.participantIds.firstOrNull { it != viewModel.currentUserId } ?: ""
                    val otherUser = users.find { it.userId == otherId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { if (otherId.isNotEmpty()) onOpenChat(otherId) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarBase64 = otherUser?.avatarBase64 ?: "",
                            username = otherUser?.username ?: "User",
                            size = 48.dp,
                            isOnline = otherUser?.isOnline == true
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(otherUser?.username ?: "Dost", fontWeight = FontWeight.Bold)
                            Text(
                                text = thread.lastMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        if (otherUser?.isOnline == true) {
                            Text("Online", color = OnlineGreen, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text("Dosto ko Message Bhejein", style = MaterialTheme.typography.labelLarge, color = InstaCoral, fontWeight = FontWeight.Bold)
            }

            if (otherUsers.isEmpty()) {
                item {
                    Text("Abhi koi aur user online nahi hai.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(otherUsers, key = { it.userId }) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenChat(user.userId) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(user.avatarBase64, user.username, size = 44.dp, isOnline = user.isOnline)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.username, fontWeight = FontWeight.Bold)
                            Text(if (user.isOnline) "Online hai" else "Offline", style = MaterialTheme.typography.labelSmall, color = if (user.isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDetailScreen(
    viewModel: MeraInstaViewModel,
    otherUserId: String,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val otherUser = users.find { it.userId == otherUserId }
    val messages by viewModel.observeMessages(otherUserId).collectAsStateWithLifecycle(initialValue = emptyList())
    var messageText by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val b64 = ImageUtils.uriToCompressedBase64(context, uri)
            if (b64.isNotEmpty()) {
                viewModel.sendMessage(otherUserId, type = "photo", mediaBase64 = b64)
            }
        }
    }

    LaunchedEffect(messages) {
        val unread = messages.filter { it.recipientId == viewModel.currentUserId && !it.isSeen }.map { it.id }
        if (unread.isNotEmpty()) {
            viewModel.markMessagesSeen(otherUserId, unread)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche")
            }
            UserAvatar(
                avatarBase64 = otherUser?.avatarBase64 ?: "",
                username = otherUser?.username ?: "U",
                size = 38.dp,
                isOnline = otherUser?.isOnline == true
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(otherUser?.username ?: "Dost", fontWeight = FontWeight.Bold)
                Text(
                    text = if (messageText.isNotBlank()) "Typing..." else if (otherUser?.isOnline == true) "Online" else "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (otherUser?.isOnline == true) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider()

        // Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == viewModel.currentUserId
                MessageBubble(msg = msg, isMine = isMine)
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Photo bhejein")
            }
            IconButton(onClick = {
                viewModel.sendMessage(otherUserId, type = "video", text = "🎥 Video Clip")
            }) {
                Icon(Icons.Default.Videocam, contentDescription = "Video bhejein")
            }
            IconButton(onClick = {
                if (isRecordingVoice) {
                    isRecordingVoice = false
                    viewModel.sendMessage(otherUserId, type = "voice", text = "Voice Message", voiceDurationSec = 4L)
                } else {
                    isRecordingVoice = true
                }
            }) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice message",
                    tint = if (isRecordingVoice) InstaCoral else MaterialTheme.colorScheme.onSurface
                )
            }
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text(if (isRecordingVoice) "Recording... tap mic to send" else "Message likhein...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input"),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(otherUserId, type = "text", text = messageText.trim())
                        messageText = ""
                    }
                },
                modifier = Modifier.testTag("send_chat_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Bhejein", tint = InstaCoral)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, isMine: Boolean) {
    val bitmap = remember(msg.mediaBase64) { ImageUtils.decodeBase64ToBitmap(msg.mediaBase64) }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isMine) InstaCoral else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 270.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else if (msg.type == "voice") {
                    Text(
                        text = "🎤 Voice Message (${msg.voiceDurationSec}s)",
                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = msg.text,
                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isMine) {
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = if (msg.isSeen) "Seen" else "Sent",
                            tint = if (msg.isSeen) Color(0xFF93C5FD) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
