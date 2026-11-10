package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.ui.MeraInstaViewModel
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.UserAvatar
import com.example.ui.theme.InstaAmber
import com.example.ui.theme.InstaCoral
import com.example.ui.theme.InstaPurple
import com.example.ui.util.ImageUtils

@Composable
fun CreatePostScreen(
    viewModel: MeraInstaViewModel,
    onPostPublished: (isReel: Boolean) -> Unit
) {
    val context = LocalContext.current
    var isReel by remember { mutableStateOf(false) }
    var mediaBase64 by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("photo") }
    var selectedFilter by remember { mutableStateOf("Normal") }
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var taggedUsers by remember { mutableStateOf("") }

    // Reel specific
    var reelDuration by remember { mutableLongStateOf(15L) }
    var selectedMusic by remember { mutableStateOf("Kesariya Beats") }
    var overlayText by remember { mutableStateOf("") }

    val musicOptions = listOf("Kesariya Beats", "Desi Lo-Fi", "Party Vibes", "Sufi Chill", "No Music")
    val durations = listOf(15L, 30L, 60L)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            mediaBase64 = ImageUtils.bitmapToCompressedBase64(bitmap)
            mediaType = if (isReel) "reel" else "photo"
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: ""
            mediaType = when {
                isReel -> "reel"
                mime.startsWith("video") -> "video"
                else -> "photo"
            }
            mediaBase64 = ImageUtils.uriToCompressedBase64(context, uri)
        }
    }

    val previewBitmap = remember(mediaBase64) { ImageUtils.decodeBase64ToBitmap(mediaBase64) }
    val previewFilter = remember(selectedFilter) { ImageUtils.colorFilterFor(selectedFilter) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (isReel) "Nayi Reel Banayein (9:16)" else "Naya Post Banayein",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isReel,
                onClick = { isReel = false; mediaType = "photo" },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) {
                Text("Photo / Video Post")
            }
            SegmentedButton(
                selected = isReel,
                onClick = { isReel = true; mediaType = "reel" },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) {
                Text("Reels (9:16)")
            }
        }

        // Camera & Gallery buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_camera_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Camera")
            }
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_gallery_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Gallery")
            }
        }

        // Live Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isReel) Modifier.aspectRatio(9f / 12f) else Modifier.height(220.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(InstaCoral, InstaPurple))),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Preview",
                    contentScale = ContentScale.Crop,
                    colorFilter = previewFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (selectedFilter == "Blur Background") Modifier.blur(2.dp) else Modifier)
                )
            } else {
                Text(
                    text = if (isReel) "9:16 Reel Preview\n${reelDuration}s • $selectedMusic" else "Photo / Moment Preview",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isReel && overlayText.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = overlayText,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Filter Picker
        Text("Filter Chunein", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ImageUtils.availableFilters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        if (isReel) {
            Text("Reel Duration", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEach { sec ->
                    FilterChip(
                        selected = reelDuration == sec,
                        onClick = { reelDuration = sec },
                        label = { Text("${sec} sec") }
                    )
                }
            }

            Text("Music Add Karein", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(musicOptions) { song ->
                    FilterChip(
                        selected = selectedMusic == song,
                        onClick = { selectedMusic = song },
                        label = { Text(song) }
                    )
                }
            }

            OutlinedTextField(
                value = overlayText,
                onValueChange = { overlayText = it },
                label = { Text("Reel par Text add karein") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Caption likhein...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_caption_input"),
            maxLines = 3
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location add karein (jaise: Mumbai, Delhi)") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = taggedUsers,
            onValueChange = { taggedUsers = it },
            label = { Text("Tag People (jaise: @priya, @rahul)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                if (mediaBase64.isNotEmpty()) {
                    ImageUtils.saveBase64ToGallery(context, mediaBase64, if (isReel) "MeraInsta_Reel" else "MeraInsta_Post")
                }
                viewModel.publishPost(
                    caption = caption.ifBlank { if (isReel) "Meri Nayi Reel!" else "Naya Post!" },
                    location = location,
                    taggedUsers = taggedUsers,
                    filterName = selectedFilter,
                    mediaType = mediaType,
                    mediaBase64 = mediaBase64,
                    isReel = isReel,
                    musicName = if (isReel) selectedMusic else "",
                    reelDurationSec = if (isReel) reelDuration else 0L,
                    overlayText = overlayText
                ) {
                    Toast.makeText(context, "Post/Reel publish aur Gallery me save ho gaya!", Toast.LENGTH_SHORT).show()
                    onPostPublished(isReel)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("publish_post_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InstaCoral)
        ) {
            Text(
                text = if (isReel) "Reel Publish Karein" else "Post Share Karein",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ReelsScreen(
    viewModel: MeraInstaViewModel,
    onProfileClick: (String) -> Unit,
    onCreateReelClick: () -> Unit
) {
    val reels by viewModel.reelsList.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    val allFollows by viewModel.allFollows.collectAsStateWithLifecycle()
    var activeCommentPost by remember { mutableStateOf<Post?>(null) }

    if (reels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = InstaCoral,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Abhi koi Reel nahi hai.\nPehli 9:16 Reel aap banayein!",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCreateReelClick,
                    colors = ButtonDefaults.buttonColors(containerColor = InstaCoral)
                ) {
                    Text("+ Reel Banayein")
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })
    val followingIds = remember(allFollows, viewModel.currentUserId) {
        allFollows.filter { it.followerId == viewModel.currentUserId }.map { it.followingId }.toSet()
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val reel = reels[page]
        val likers by viewModel.observePostLikes(reel.id).collectAsStateWithLifecycle(initialValue = emptyList())
        val isLiked = viewModel.currentUserId in likers
        val isSaved = reel.id in savedIds
        val isFollowing = reel.userId in followingIds
        val bitmap = remember(reel.mediaBase64) { ImageUtils.decodeBase64ToBitmap(reel.mediaBase64) }
        val colorFilter = remember(reel.filterName) { ImageUtils.colorFilterFor(reel.filterName) }

        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = reel.caption,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(InstaPurple, InstaCoral, InstaAmber))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            if (reel.overlayText.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = reel.overlayText,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Right side actions
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { viewModel.toggleLike(reel, isLiked) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) InstaCoral else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text("${maxOf(reel.likesCount, likers.size.toLong())}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { activeCommentPost = reel }) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Text("${reel.commentsCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { /* Share handled via toast */ }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { viewModel.toggleSavePost(reel.id) }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Music", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Bottom info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 76.dp, bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(reel.userAvatarBase64, reel.username, size = 36.dp, onClick = { onProfileClick(reel.userId) })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = reel.username,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onProfileClick(reel.userId) }
                    )
                    if (reel.userId != viewModel.currentUserId) {
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { viewModel.followOrUnfollow(reel.userId, isFollowing) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        ) {
                            Text(if (isFollowing) "Following" else "Follow", color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(reel.caption, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${reel.musicName.ifBlank { "Original Audio" }} • ${reel.reelDurationSec}s",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    activeCommentPost?.let { post ->
        CommentsBottomSheet(post = post, viewModel = viewModel, onDismiss = { activeCommentPost = null })
    }
}
