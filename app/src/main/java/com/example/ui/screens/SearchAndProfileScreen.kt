package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.MeraInstaViewModel
import com.example.ui.components.FollowListBottomSheet
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.InstaAmber
import com.example.ui.theme.InstaCoral
import com.example.ui.theme.InstaPurple
import com.example.ui.util.ImageUtils

@Composable
fun SearchScreen(
    viewModel: MeraInstaViewModel,
    onProfileClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val posts by viewModel.allPosts.collectAsStateWithLifecycle()
    val follows by viewModel.allFollows.collectAsStateWithLifecycle()
    val blocks by viewModel.myBlocks.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedPostIds.collectAsStateWithLifecycle()

    var fullScreenPost by remember { mutableStateOf<Post?>(null) }

    val blockedIds = remember(blocks) { blocks.map { it.blockedId }.toSet() }
    val followingIds = remember(follows, viewModel.currentUserId) {
        follows.filter { it.followerId == viewModel.currentUserId }.map { it.followingId }.toSet()
    }

    val matchedUsers = remember(query, users, blockedIds) {
        if (query.isBlank()) emptyList()
        else users.filter {
            it.userId != viewModel.currentUserId &&
                it.userId !in blockedIds &&
                (it.username.contains(query, ignoreCase = true) ||
                    it.displayName.contains(query, ignoreCase = true))
        }
    }

    val visiblePosts = remember(posts, blockedIds) {
        posts.filter { it.userId !in blockedIds }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Dost dhoondhein (naam ya username)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Saaf karein")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("search_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        if (query.isNotBlank()) {
            if (matchedUsers.isEmpty()) {
                Text(
                    text = "Koi user nahi mila.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchedUsers, key = { it.userId }) { user ->
                        val isFollowing = user.userId in followingIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onProfileClick(user.userId) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(user.avatarBase64, user.username, size = 44.dp, isOnline = user.isOnline)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.username, fontWeight = FontWeight.Bold)
                                Text(user.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { viewModel.followOrUnfollow(user.userId, isFollowing) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else InstaCoral,
                                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                                )
                            ) {
                                Text(if (isFollowing) "Following" else "Follow")
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(visiblePosts, key = { it.id }) { post ->
                    GridPostTile(post = post, onClick = { fullScreenPost = post })
                }
            }
        }
    }

    fullScreenPost?.let { post ->
        Dialog(
            onDismissRequest = { fullScreenPost = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { fullScreenPost = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Band karein")
                        }
                        Text("Post", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    PostCard(
                        post = post,
                        viewModel = viewModel,
                        isSaved = post.id in savedIds,
                        onProfileClick = {
                            fullScreenPost = null
                            onProfileClick(it)
                        },
                        onCommentClick = {},
                        onShareClick = {},
                        onSaveClick = { viewModel.toggleSavePost(it.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun GridPostTile(post: Post, onClick: () -> Unit) {
    val bitmap = remember(post.mediaBase64) { ImageUtils.decodeBase64ToBitmap(post.mediaBase64) }
    val colorFilter = remember(post.filterName) { ImageUtils.colorFilterFor(post.filterName) }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = post.caption,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(InstaCoral, InstaAmber, InstaPurple)))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.caption.take(30),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (post.isReel || post.mediaType == "video") {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Reel",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: MeraInstaViewModel,
    targetUserId: String,
    onBackClick: (() -> Unit)? = null,
    onProfileClick: (String) -> Unit,
    onMessageUserClick: (String) -> Unit,
    onOpenPermissionsWizard: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val allFollows by viewModel.allFollows.collectAsStateWithLifecycle()
    val myBlocks by viewModel.myBlocks.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val isMe = targetUserId == viewModel.currentUserId
    val profile = allUsers.find { it.userId == targetUserId }
    val userPosts = remember(allPosts, targetUserId) { allPosts.filter { it.userId == targetUserId } }

    val followerUsers = remember(allFollows, allUsers, targetUserId) {
        val ids = allFollows.filter { it.followingId == targetUserId }.map { it.followerId }.toSet()
        allUsers.filter { it.userId in ids }
    }
    val followingUsers = remember(allFollows, allUsers, targetUserId) {
        val ids = allFollows.filter { it.followerId == targetUserId }.map { it.followingId }.toSet()
        allUsers.filter { it.userId in ids }
    }
    val myFollowingIds = remember(allFollows, viewModel.currentUserId) {
        allFollows.filter { it.followerId == viewModel.currentUserId }.map { it.followingId }.toSet()
    }
    val myBlockedIds = remember(myBlocks) { myBlocks.map { it.blockedId }.toSet() }
    val isFollowingTarget = targetUserId in myFollowingIds
    val isBlockedTarget = targetUserId in myBlockedIds

    var showEditProfile by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showFollowSheetTitle by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.Close, contentDescription = "Peeche jayein")
                }
            }
            Text(
                text = profile?.username ?: "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isMe) {
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier.testTag("profile_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        // Avatar + Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarBase64 = profile?.avatarBase64 ?: "",
                username = profile?.username ?: "U",
                size = 80.dp,
                showStoryRing = true,
                isOnline = profile?.isOnline == true
            )
            Spacer(Modifier.width(16.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(count = userPosts.size, label = "Posts") {}
                StatColumn(count = followerUsers.size, label = "Followers") {
                    showFollowSheetTitle = "Followers"
                }
                StatColumn(count = followingUsers.size, label = "Following") {
                    showFollowSheetTitle = "Following"
                }
            }
        }

        // Name & Bio
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(profile?.displayName ?: "", fontWeight = FontWeight.Bold)
            if (!profile?.bio.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(profile?.bio ?: "", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isMe) {
                OutlinedButton(
                    onClick = { showEditProfile = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_button")
                ) {
                    Text("Edit Profile / Bio Change")
                }
            } else {
                Button(
                    onClick = { viewModel.followOrUnfollow(targetUserId, isFollowingTarget) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_follow_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowingTarget) MaterialTheme.colorScheme.surfaceVariant else InstaCoral,
                        contentColor = if (isFollowingTarget) MaterialTheme.colorScheme.onSurface else Color.White
                    )
                ) {
                    Text(if (isFollowingTarget) "Unfollow" else "Follow")
                }
                OutlinedButton(
                    onClick = { onMessageUserClick(targetUserId) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_message_button")
                ) {
                    Text("Message")
                }
                OutlinedButton(
                    onClick = { viewModel.blockOrUnblock(targetUserId, isBlockedTarget) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBlockedTarget) "Unblock" else "Block")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

        if (userPosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Abhi koi Post nahi hai.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(userPosts, key = { it.id }) { post ->
                    GridPostTile(post = post, onClick = {})
                }
            }
        }
    }

    showFollowSheetTitle?.let { title ->
        val list = if (title == "Followers") followerUsers else followingUsers
        FollowListBottomSheet(
            title = title,
            users = list,
            followingIds = myFollowingIds,
            blockedIds = myBlockedIds,
            currentUserId = viewModel.currentUserId,
            onFollowToggle = { uid, isF -> viewModel.followOrUnfollow(uid, isF) },
            onBlockToggle = { uid, isB -> viewModel.blockOrUnblock(uid, isB) },
            onUserClick = { uid ->
                showFollowSheetTitle = null
                onProfileClick(uid)
            },
            onDismiss = { showFollowSheetTitle = null }
        )
    }

    if (showEditProfile && profile != null) {
        EditProfileDialog(
            initialName = profile.displayName,
            initialUsername = profile.username,
            initialBio = profile.bio,
            initialAvatarBase64 = profile.avatarBase64,
            onDismiss = { showEditProfile = false },
            onSave = { name, uname, bio, avatar ->
                viewModel.saveProfile(uname, name, bio, avatar) {
                    showEditProfile = false
                }
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DarkMode, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Dark Mode", modifier = Modifier.weight(1f))
                        Switch(checked = isDarkTheme, onCheckedChange = { viewModel.toggleTheme() })
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSettings = false
                                showEditProfile = true
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Bio aur Profile Badlein")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSettings = false
                                onOpenPermissionsWizard()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("App Permissions Setup")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSettings = false
                                onLogoutClick()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text("Logout Karein", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Band Karein") }
            }
        )
    }
}

@Composable
private fun StatColumn(count: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text("$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialUsername: String,
    initialBio: String,
    initialAvatarBase64: String,
    onDismiss: () -> Unit,
    onSave: (name: String, username: String, bio: String, avatar: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var username by remember { mutableStateOf(initialUsername) }
    var bio by remember { mutableStateOf(initialBio) }
    var avatar by remember { mutableStateOf(initialAvatarBase64) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            avatar = ImageUtils.uriToCompressedBase64(context, uri, maxDim = 320)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile Edit Karein") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                UserAvatar(avatar, username, size = 64.dp, onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
                Text("Photo badalne ke liye tap karein", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Naam") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, maxLines = 3)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, username, bio, avatar) },
                colors = ButtonDefaults.buttonColors(containerColor = InstaCoral)
            ) {
                Text("Save Karein")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Radd Karein") }
        }
    )
}
