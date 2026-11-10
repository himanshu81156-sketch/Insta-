package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.Story
import com.example.ui.MeraInstaViewModel
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.InstaAmber
import com.example.ui.theme.InstaCoral
import com.example.ui.theme.InstaPurple
import com.example.ui.util.ImageUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    viewModel: MeraInstaViewModel,
    onProfileClick: (String) -> Unit,
    onCreatePostClick: () -> Unit,
    onSendPostToChat: (String, Post) -> Unit
) {
    val posts by viewModel.homeFeedPosts.collectAsStateWithLifecycle()
    val stories by viewModel.activeStories.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val onlyFollowing by viewModel.feedOnlyFollowing.collectAsStateWithLifecycle()
    val me by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var activeCommentPost by remember { mutableStateOf<Post?>(null) }
    var sharePostTarget by remember { mutableStateOf<Post?>(null) }
    var showCreateStory by remember { mutableStateOf(false) }
    var activeStoryIndex by remember { mutableStateOf<Int?>(null) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshFeed() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Stories Row
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Meri Story +
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable { showCreateStory = true }
                                .testTag("add_story_button")
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                UserAvatar(
                                    avatarBase64 = me?.avatarBase64 ?: "",
                                    username = me?.username ?: "Me",
                                    size = 60.dp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(InstaCoral)
                                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Story lagayein",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Meri Story",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Friends' 24h Stories
                    items(stories.size) { index ->
                        val story = stories[index]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable { activeStoryIndex = index }
                        ) {
                            UserAvatar(
                                avatarBase64 = story.userAvatarBase64,
                                username = story.username,
                                size = 60.dp,
                                showStoryRing = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = story.username,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }

            // Feed Filter Chips (Sabhi Posts vs Following)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !onlyFollowing,
                        onClick = { viewModel.setFeedFilter(false) },
                        label = { Text("Sabhi Posts") }
                    )
                    FilterChip(
                        selected = onlyFollowing,
                        onClick = { viewModel.setFeedFilter(true) },
                        label = { Text("Sirf Following") }
                    )
                }
            }

            if (posts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (onlyFollowing) {
                                "Aapke followed dosto ka abhi koi post nahi hai."
                            } else {
                                "Abhi koi post nahi hai. Pehla Post aap banayein!"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onCreatePostClick,
                            colors = ButtonDefaults.buttonColors(containerColor = InstaCoral)
                        ) {
                            Text("+ Naya Post Banayein")
                        }
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        viewModel = viewModel,
                        isSaved = post.id in savedIds,
                        onProfileClick = onProfileClick,
                        onCommentClick = { activeCommentPost = it },
                        onShareClick = { sharePostTarget = it },
                        onSaveClick = { viewModel.toggleSavePost(it.id) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }

    activeCommentPost?.let { post ->
        CommentsBottomSheet(
            post = post,
            viewModel = viewModel,
            onDismiss = { activeCommentPost = null }
        )
    }

    sharePostTarget?.let { post ->
        val otherUsers = allUsers.filter { it.userId != viewModel.currentUserId }
        AlertDialog(
            onDismissRequest = { sharePostTarget = null },
            title = { Text("Dost ko Share Karein") },
            text = {
                if (otherUsers.isEmpty()) {
                    Text("Abhi koi aur user online nahi hai.")
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(otherUsers, key = { it.userId }) { u ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSendPostToChat(u.userId, post)
                                        sharePostTarget = null
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(u.avatarBase64, u.username, size = 36.dp, isOnline = u.isOnline)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(u.username, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sharePostTarget = null }) {
                    Text("Band Karein")
                }
            }
        )
    }

    if (showCreateStory) {
        CreateStoryDialog(
            onDismiss = { showCreateStory = false },
            onPublish = { caption, mediaBase64, bgHex ->
                viewModel.publishStory(caption, mediaBase64, bgHex) {
                    showCreateStory = false
                }
            }
        )
    }

    activeStoryIndex?.let { startIdx ->
        if (stories.isNotEmpty()) {
            StoryViewerDialog(
                stories = stories,
                initialIndex = startIdx.coerceIn(0, stories.lastIndex),
                onDismiss = { activeStoryIndex = null }
            )
        }
    }
}

@Composable
fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onPublish: (caption: String, mediaBase64: String, bgHex: String) -> Unit
) {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var mediaBase64 by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#E1306C") }
    val colors = listOf("#E1306C", "#833AB4", "#F77737", "#405DE6", "#10B981")

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaBase64 = ImageUtils.uriToCompressedBase64(context, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("24 Ghante Wali Story Lagayein") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Story par kya likhna hai?") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (mediaBase64.isEmpty()) "Gallery se Photo Chunein" else "Photo Chuna Gaya ✓")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(caption, mediaBase64, selectedColor) },
                colors = ButtonDefaults.buttonColors(containerColor = InstaCoral)
            ) {
                Text("Story Lagayein")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Radd Karein") }
        }
    )
}

@Composable
fun StoryViewerDialog(
    stories: List<Story>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var progress by remember { mutableStateOf(0f) }
    val story = stories[currentIndex]
    val bitmap = remember(story.mediaBase64) { ImageUtils.decodeBase64ToBitmap(story.mediaBase64) }

    LaunchedEffect(currentIndex) {
        progress = 0f
        while (progress < 1f) {
            delay(50)
            progress += 0.01f
        }
        if (currentIndex < stories.lastIndex) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable {
                    if (currentIndex < stories.lastIndex) currentIndex++ else onDismiss()
                }
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Story",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val bgColor = try {
                    Color(android.graphics.Color.parseColor(story.bgColorHex))
                } catch (_: Exception) {
                    InstaCoral
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(bgColor, InstaPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.caption.ifBlank { "${story.username} ki Story" },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, start = 16.dp, end = 16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(story.userAvatarBase64, story.username, size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(story.username, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("24 ghante ki Story", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Band karein", tint = Color.White)
                    }
                }
            }

            if (bitmap != null && story.caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(20.dp)
                ) {
                    Text(
                        text = story.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
