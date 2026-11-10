package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.UserProfile
import com.example.ui.MeraInstaViewModel
import com.example.ui.theme.InstaAmber
import com.example.ui.theme.InstaCoral
import com.example.ui.theme.InstaPurple
import com.example.ui.theme.OnlineGreen
import com.example.ui.util.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun UserAvatar(
    avatarBase64: String,
    username: String,
    size: Dp = 42.dp,
    showStoryRing: Boolean = false,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val bitmap = remember(avatarBase64) { ImageUtils.decodeBase64ToBitmap(avatarBase64) }
    val ringModifier = if (showStoryRing) {
        Modifier
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(listOf(InstaAmber, InstaCoral, InstaPurple)),
                shape = CircleShape
            )
            .padding(3.dp)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(size + if (showStoryRing) 8.dp else 0.dp)
            .then(ringModifier)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "$username profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(InstaCoral, InstaPurple))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.take(1).uppercase().ifBlank { "M" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    viewModel: MeraInstaViewModel,
    isSaved: Boolean,
    onProfileClick: (String) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onSaveClick: (Post) -> Unit
) {
    val likers by viewModel.observePostLikes(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val isLiked = viewModel.currentUserId in likers
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val bitmap = remember(post.mediaBase64) { ImageUtils.decodeBase64ToBitmap(post.mediaBase64) }
    val colorFilter = remember(post.filterName) { ImageUtils.colorFilterFor(post.filterName) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 12.dp)
    ) {
        // Post header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarBase64 = post.userAvatarBase64,
                username = post.username,
                size = 38.dp,
                onClick = { onProfileClick(post.userId) }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProfileClick(post.userId) }
            ) {
                Text(
                    text = post.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (post.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = post.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (post.filterName != "Normal" && post.filterName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = post.filterName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Media with Double-Tap Heart Animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(post.id, isLiked) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (!isLiked) {
                                viewModel.toggleLike(post, false)
                            }
                            scope.launch {
                                heartScale.snapTo(0.4f)
                                heartAlpha.snapTo(1f)
                                heartScale.animateTo(1.3f, spring(dampingRatio = 0.45f))
                                heartAlpha.animateTo(0f)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = post.caption.ifBlank { "Post photo" },
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (post.filterName == "Blur Background") Modifier.blur(2.dp) else Modifier)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(InstaCoral, InstaAmber, InstaPurple)
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (post.mediaType == "video") {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = post.caption.ifBlank { "Mera Insta Moment" },
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (heartAlpha.value > 0f) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Liked animation",
                    tint = Color.White,
                    modifier = Modifier
                        .size(96.dp)
                        .scale(heartScale.value)
                        .alpha(heartAlpha.value)
                )
            }
        }

        // Action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.toggleLike(post, isLiked) },
                modifier = Modifier.testTag("like_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) InstaCoral else MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(
                onClick = { onCommentClick(post) },
                modifier = Modifier.testTag("comment_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comment"
                )
            }
            IconButton(
                onClick = { onShareClick(post) },
                modifier = Modifier.testTag("share_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Share"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { onSaveClick(post) },
                modifier = Modifier.testTag("save_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save"
                )
            }
        }

        // Likes count & caption
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(
                text = "${maxOf(post.likesCount, likers.size.toLong())} likes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            if (post.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${post.username}  ${post.caption}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (post.taggedUsers.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tagged: ${post.taggedUsers}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InstaCoral
                )
            }
            if (post.commentsCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${post.commentsCount} comments dekhein",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onCommentClick(post) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    viewModel: MeraInstaViewModel,
    onDismiss: () -> Unit
) {
    val comments by viewModel.observeComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (comments.isEmpty()) {
                Text(
                    text = "Abhi koi comment nahi hai. Pehla comment karein!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.id }) { item: PostComment ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = item.username,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Comment likhein...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(post, commentText)
                            commentText = ""
                        }
                    },
                    modifier = Modifier.testTag("send_comment_button")
                ) {
                    Text("Bhejein")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListBottomSheet(
    title: String,
    users: List<UserProfile>,
    followingIds: Set<String>,
    blockedIds: Set<String>,
    currentUserId: String,
    onFollowToggle: (String, Boolean) -> Unit,
    onBlockToggle: (String, Boolean) -> Unit,
    onUserClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (users.isEmpty()) {
                Text(
                    text = "Yaha abhi koi nahi hai.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(users, key = { it.userId }) { user ->
                        val isFollowing = user.userId in followingIds
                        val isBlocked = user.userId in blockedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserClick(user.userId) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                avatarBase64 = user.avatarBase64,
                                username = user.username,
                                size = 40.dp,
                                isOnline = user.isOnline
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.username,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (user.userId != currentUserId) {
                                OutlinedButton(
                                    onClick = { onFollowToggle(user.userId, isFollowing) },
                                    contentPadding = ButtonDefaults.ContentPadding
                                ) {
                                    Text(if (isFollowing) "Unfollow" else "Follow")
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { onBlockToggle(user.userId, isBlocked) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(if (isBlocked) "Unblock" else "Block")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
