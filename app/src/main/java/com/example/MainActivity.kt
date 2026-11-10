package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.MeraInstaRepository
import com.example.ui.MeraInstaViewModel
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PermissionsWizardDialog
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.InstaCoral
import com.example.ui.theme.MeraInstaTheme
import com.example.ui.theme.OnlineGreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeraInstaRoot()
        }
    }
}

sealed interface OverlayRoute {
    data object None : OverlayRoute
    data object Notifications : OverlayRoute
    data object ChatList : OverlayRoute
    data class ChatDetail(val userId: String) : OverlayRoute
    data class OtherProfile(val userId: String) : OverlayRoute
}

@Composable
fun MeraInstaRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var showWelcome by rememberSaveable { mutableStateOf(auth.currentUser == null) }
    var globalDarkTheme by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { showWelcome = false },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    if (showWelcome && currentUser == null) {
        MeraInstaTheme(darkTheme = globalDarkTheme) {
            WelcomeScreen(
                isDarkTheme = globalDarkTheme,
                onToggleTheme = { globalDarkTheme = !globalDarkTheme },
                onContinue = { showWelcome = false }
            )
        }
    } else if (currentUser == null) {
        MeraInstaTheme(darkTheme = globalDarkTheme) {
            AuthScreen(
                isDarkTheme = globalDarkTheme,
                onToggleTheme = { globalDarkTheme = !globalDarkTheme },
                onAuthSuccess = {}
            )
        }
    } else {
        val uid = currentUser!!.uid
        val displayName = currentUser!!.displayName ?: ""
        key(uid) {
            val viewModel: MeraInstaViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MeraInstaViewModel(MeraInstaRepository(db, Firebase.auth), uid)
                    }
                }
            )
            val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MeraInstaTheme(darkTheme = isDark) {
                AuthenticatedMeraInstaApp(
                    viewModel = viewModel,
                    initialDisplayName = displayName,
                    onLogout = {
                        viewModel.setOnlineStatus(false)
                        signOutUser(credentialManager, scope)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMeraInstaApp(
    viewModel: MeraInstaViewModel,
    initialDisplayName: String,
    onLogout: () -> Unit
) {
    val myProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val toastMsg by viewModel.hindiToastMessage.collectAsStateWithLifecycle()

    var settledInitialLoad by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(900)
        settledInitialLoad = true
    }

    if (myProfile == null && !settledInitialLoad) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = InstaCoral)
        }
        return
    }

    if (myProfile == null) {
        ProfileSetupScreen(
            initialName = initialDisplayName,
            onSaveProfile = { username, name, bio, avatar ->
                viewModel.saveProfile(username, name, bio, avatar)
            }
        )
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var overlayRoute by remember { mutableStateOf<OverlayRoute>(OverlayRoute.None) }
    var showPermissionsWizard by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMsg) {
        toastMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val unreadNotifs = notifications.count { !it.isRead }
    val isOnline = myProfile?.isOnline == true

    if (overlayRoute !is OverlayRoute.None) {
        BackHandler { overlayRoute = OverlayRoute.None }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val route = overlayRoute) {
                    OverlayRoute.Notifications -> NotificationsScreen(
                        viewModel = viewModel,
                        onBack = { overlayRoute = OverlayRoute.None },
                        onProfileClick = { uid ->
                            overlayRoute = OverlayRoute.OtherProfile(uid)
                        }
                    )
                    OverlayRoute.ChatList -> ChatListScreen(
                        viewModel = viewModel,
                        onBack = { overlayRoute = OverlayRoute.None },
                        onOpenChat = { uid -> overlayRoute = OverlayRoute.ChatDetail(uid) }
                    )
                    is OverlayRoute.ChatDetail -> ChatDetailScreen(
                        viewModel = viewModel,
                        otherUserId = route.userId,
                        onBack = { overlayRoute = OverlayRoute.ChatList }
                    )
                    is OverlayRoute.OtherProfile -> ProfileScreen(
                        viewModel = viewModel,
                        targetUserId = route.userId,
                        onBackClick = { overlayRoute = OverlayRoute.None },
                        onProfileClick = { uid -> overlayRoute = OverlayRoute.OtherProfile(uid) },
                        onMessageUserClick = { uid -> overlayRoute = OverlayRoute.ChatDetail(uid) },
                        onOpenPermissionsWizard = { showPermissionsWizard = true },
                        onLogoutClick = onLogout
                    )
                    OverlayRoute.None -> Unit
                }
            }
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (selectedTab != 3) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.setOnlineStatus(!isOnline) }
                            ) {
                                Text(
                                    text = "Mera Insta",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = InstaCoral
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) OnlineGreen else Color.Gray)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isOnline) "Online" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnline) OnlineGreen else Color.Gray
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.toggleTheme() },
                                modifier = Modifier.testTag("topbar_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Theme badlein"
                                )
                            }
                            IconButton(
                                onClick = { overlayRoute = OverlayRoute.Notifications },
                                modifier = Modifier.testTag("topbar_notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifs > 0) {
                                            Badge { Text("$unreadNotifs") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Notifications")
                                }
                            }
                            IconButton(
                                onClick = { overlayRoute = OverlayRoute.ChatList },
                                modifier = Modifier.testTag("topbar_chat_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        modifier = Modifier.testTag("nav_search")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.AddBox, contentDescription = "+ Post") },
                        label = { Text("+ Post") },
                        modifier = Modifier.testTag("nav_post")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Movie, contentDescription = "Reels") },
                        label = { Text("Reels") },
                        modifier = Modifier.testTag("nav_reels")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> HomeFeedScreen(
                        viewModel = viewModel,
                        onProfileClick = { uid ->
                            if (uid == viewModel.currentUserId) selectedTab = 4
                            else overlayRoute = OverlayRoute.OtherProfile(uid)
                        },
                        onCreatePostClick = { selectedTab = 2 },
                        onSendPostToChat = { targetUid, post ->
                            viewModel.sendMessage(
                                recipientId = targetUid,
                                type = "text",
                                text = "Post Share kiya: ${post.username} - ${post.caption.take(60)}"
                            )
                        }
                    )
                    1 -> SearchScreen(
                        viewModel = viewModel,
                        onProfileClick = { uid ->
                            if (uid == viewModel.currentUserId) selectedTab = 4
                            else overlayRoute = OverlayRoute.OtherProfile(uid)
                        }
                    )
                    2 -> CreatePostScreen(
                        viewModel = viewModel,
                        onPostPublished = { isReel ->
                            selectedTab = if (isReel) 3 else 0
                        }
                    )
                    3 -> ReelsScreen(
                        viewModel = viewModel,
                        onProfileClick = { uid ->
                            if (uid == viewModel.currentUserId) selectedTab = 4
                            else overlayRoute = OverlayRoute.OtherProfile(uid)
                        },
                        onCreateReelClick = { selectedTab = 2 }
                    )
                    4 -> ProfileScreen(
                        viewModel = viewModel,
                        targetUserId = viewModel.currentUserId,
                        onProfileClick = { uid ->
                            if (uid != viewModel.currentUserId) {
                                overlayRoute = OverlayRoute.OtherProfile(uid)
                            }
                        },
                        onMessageUserClick = { uid -> overlayRoute = OverlayRoute.ChatDetail(uid) },
                        onOpenPermissionsWizard = { showPermissionsWizard = true },
                        onLogoutClick = onLogout
                    )
                }
            }
        }
    }

    if (showPermissionsWizard) {
        PermissionsWizardDialog(onDismiss = { showPermissionsWizard = false })
    }
}
