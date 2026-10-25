package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ConnectNpRepository
import com.example.model.MessageMediaType
import com.example.model.User
import com.example.model.toOfficialPage
import com.example.model.toPost
import com.example.model.toWalletTransaction
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.ConnectNpTheme
import com.example.ui.theme.NepalCrimson

import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.ConnectNpFirestoreRepository
import com.example.service.ConnectNpMessagingService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by ConnectNpRepository.isDarkMode.collectAsStateWithLifecycle()
            ConnectNpTheme(darkTheme = isDark) {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        ConnectNpMainApp(
            currentUserId = currentUser?.uid ?: "",
            onSignOutUser = { currentUser = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectNpMainApp(
    currentUserId: String = "",
    onSignOutUser: () -> Unit = {}
) {
    val posts by ConnectNpRepository.posts.collectAsStateWithLifecycle()
    val stories by ConnectNpRepository.stories.collectAsStateWithLifecycle()
    val reels by ConnectNpRepository.reels.collectAsStateWithLifecycle()
    val chatThreads by ConnectNpRepository.chatThreads.collectAsStateWithLifecycle()
    val messagesByThread by ConnectNpRepository.messagesByThread.collectAsStateWithLifecycle()
    val marketItems by ConnectNpRepository.marketItems.collectAsStateWithLifecycle()
    val watchVideos by ConnectNpRepository.watchVideos.collectAsStateWithLifecycle()
    val jobs by ConnectNpRepository.jobs.collectAsStateWithLifecycle()
    val datingProfiles by ConnectNpRepository.datingProfiles.collectAsStateWithLifecycle()
    val datingMatches by ConnectNpRepository.datingMatches.collectAsStateWithLifecycle()
    val walletBalance by ConnectNpRepository.walletBalance.collectAsStateWithLifecycle()
    val transactions by ConnectNpRepository.transactions.collectAsStateWithLifecycle()
    val isNepali by ConnectNpRepository.isNepaliLanguage.collectAsStateWithLifecycle()
    val isDark by ConnectNpRepository.isDarkMode.collectAsStateWithLifecycle()
    val unreadNotifications by ConnectNpRepository.unreadNotifications.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(NavigationTab.FEED) }
    var showPayScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val firestoreRepo = remember(context) { ConnectNpFirestoreRepository(context) }
    val userProfileDoc by firestoreRepo.observeUserProfile().collectAsStateWithLifecycle(initialValue = null)

    val currentActiveUser by ConnectNpRepository.currentActiveUser.collectAsStateWithLifecycle()

    LaunchedEffect(userProfileDoc, Firebase.auth.currentUser) {
        ConnectNpRepository.updateActiveUser(userProfileDoc, Firebase.auth.currentUser)
        userProfileDoc?.blockedUsers?.let { blockedList ->
            if (blockedList.isNotEmpty()) {
                ConnectNpRepository.setBlockedUserIds(blockedList)
            }
        }
        userProfileDoc?.walletBalance?.let { balance ->
            ConnectNpRepository.setWalletBalance(balance.toDouble())
        }
    }

    // Real-time Firestore sync for Posts
    LaunchedEffect(firestoreRepo) {
        firestoreRepo.observeAllPosts().collect { fsPosts ->
            ConnectNpRepository.setPosts(fsPosts.map { it.toPost() })
        }
    }

    // Real-time Firestore sync for Wallet Transactions
    LaunchedEffect(firestoreRepo) {
        firestoreRepo.observeTransactions().collect { fsTransactions ->
            ConnectNpRepository.setTransactions(fsTransactions.map { it.toWalletTransaction() })
        }
    }

    // Real-time Firestore sync for Official Page
    LaunchedEffect(firestoreRepo) {
        firestoreRepo.observeOfficialPage().collect { fsPage ->
            if (fsPage != null) {
                val currentUid = Firebase.auth.currentUser?.uid ?: ""
                ConnectNpRepository.setOfficialPage(fsPage.toOfficialPage(currentUid))
            }
        }
    }

    // Request Notification permission on Android 13+ (TASK 3)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "Notification permission granted: $isGranted")
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        ConnectNpMessagingService.createNotificationChannels(context)

        // Attempt FCM registration gracefully only if Google Play Services is available
        try {
            val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(context)
            if (resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        if (!token.isNullOrBlank()) {
                            Log.d("MainActivity", "FCM token retrieved successfully")
                            ConnectNpRepository.setFcmToken(token)
                            val uid = Firebase.auth.currentUser?.uid
                            if (uid != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        ConnectNpFirestoreRepository(context).updateFcmToken(token)
                                    } catch (e: Exception) {
                                        Log.d("MainActivity", "Failed updating FCM token to Firestore: ${e.message}")
                                    }
                                }
                            }
                        }
                    } else {
                        Log.i("MainActivity", "FCM registration not active in current environment: ${task.exception?.message}")
                    }
                }
            } else {
                Log.i("MainActivity", "Google Play Services not available for FCM (code: $resultCode). Local notifications active.")
            }
        } catch (e: Exception) {
            Log.i("MainActivity", "FCM token check skipped: ${e.message}")
        }
    }

    // Android back navigation: Return to Feed if on another tab or closing pay screen
    androidx.activity.compose.BackHandler(enabled = currentTab != NavigationTab.FEED || showPayScreen) {
        if (showPayScreen) {
            showPayScreen = false
        } else {
            currentTab = NavigationTab.FEED
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Modals / Dialogs
    var activeStoryIndex by remember { mutableStateOf<Int?>(null) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var activeCallUserAndVideo by remember { mutableStateOf<Pair<User, Boolean>?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showSplashScreen by remember { mutableStateOf(false) }

    val totalUnreadMessages = remember(chatThreads) {
        chatThreads.sumOf { it.unreadCount }
    }

    Scaffold(
        topBar = {
            ConnectNpTopBar(
                walletBalance = walletBalance,
                unreadNotifications = unreadNotifications,
                isNepali = isNepali,
                isDark = isDark,
                onOpenWallet = { showPayScreen = true },
                onOpenQrScanner = { showQrDialog = true },
                onOpenNotifications = { showNotificationsSheet = true },
                onOpenSearch = { showSearchDialog = true },
                onToggleLanguage = { ConnectNpRepository.toggleLanguage() },
                onToggleDark = { ConnectNpRepository.toggleDarkMode() }
            )
        },
        bottomBar = {
            ConnectNpBottomBar(
                currentTab = currentTab,
                unreadMessagesCount = totalUnreadMessages,
                isNepali = isNepali,
                onTabSelected = { tab ->
                    showPayScreen = false
                    currentTab = tab
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showPayScreen) {
                ConnectNpPayScreen(
                    balance = walletBalance,
                    transactions = transactions,
                    isNepali = isNepali,
                    onSendMoney = { recipient, amount, remarks ->
                        ConnectNpRepository.sendMoney(recipient, amount, remarks, context)
                    },
                    onTopUp = { amount, source ->
                        ConnectNpRepository.topUpWallet(amount, source, context)
                    },
                    onScanQr = { showQrDialog = true }
                )
            } else {
                when (currentTab) {
                    NavigationTab.FEED -> {
                        FeedScreen(
                            posts = posts,
                            stories = stories,
                            isNepali = isNepali,
                            currentUser = currentActiveUser,
                            onStoryClick = { index -> activeStoryIndex = index },
                            onAddStoryClick = {
                                ConnectNpRepository.addStory("Loving ConnectNP Super-App vibes! 🇳🇵✨")
                                activeStoryIndex = 0
                            },
                            onCreatePostClick = { showCreatePostDialog = true },
                            onToggleReaction = { postId, reaction ->
                                ConnectNpRepository.toggleReaction(postId, reaction, context)
                            },
                            onVotePoll = { postId, optId ->
                                ConnectNpRepository.votePoll(postId, optId)
                            },
                            onToggleBookmark = { postId ->
                                ConnectNpRepository.toggleBookmark(postId)
                            },
                            onAddComment = { postId, text ->
                                ConnectNpRepository.addComment(postId, text)
                            },
                            onSharePost = { postId ->
                                ConnectNpRepository.incrementShareCount(postId)
                            },
                            onOpenSearch = { showSearchDialog = true },
                            onNavigateToMarketplace = { currentTab = NavigationTab.MARKETPLACE },
                            onNavigateToReels = { currentTab = NavigationTab.REELS }
                        )
                    }
                    NavigationTab.REELS -> {
                        ReelsScreen(
                            reels = reels,
                            isNepali = isNepali,
                            onToggleLike = { ConnectNpRepository.toggleReelLike(it, context) },
                            onToggleSave = { ConnectNpRepository.toggleReelSave(it) }
                        )
                    }
                    NavigationTab.CHATS -> {
                        ChatScreen(
                            threads = chatThreads,
                            messagesByThread = messagesByThread,
                            isNepali = isNepali,
                            onSendMessage = { threadId, text, mediaType, duration ->
                                ConnectNpRepository.sendMessage(threadId, text, mediaType, duration, context)
                            },
                            onStartCall = { user, isVideo ->
                                activeCallUserAndVideo = Pair(user, isVideo)
                            }
                        )
                    }
                    NavigationTab.MARKETPLACE -> {
                        MarketplaceScreen(
                            items = marketItems,
                            isNepali = isNepali,
                            onAddListing = { title, price, cat, loc, desc ->
                                ConnectNpRepository.addMarketListing(title, price, cat, loc, desc)
                            },
                            onBuyEscrow = { item ->
                                ConnectNpRepository.sendMoney("Seller: ${item.seller.name}", item.priceNpr.toDouble(), "Escrow for ${item.title}")
                                showPayScreen = true
                            }
                        )
                    }
                    NavigationTab.PROFILE -> {
                        ProfileScreen(
                            isNepali = isNepali,
                            onNavigateToSettings = { currentTab = NavigationTab.SETTINGS },
                            onOpenWallet = { showPayScreen = true },
                            onOpenQr = { showQrDialog = true }
                        )
                    }
                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            isNepali = isNepali,
                            isDarkMode = isDark,
                            onToggleLanguage = { ConnectNpRepository.toggleLanguage() },
                            onToggleDarkMode = { ConnectNpRepository.toggleDarkMode() },
                            onAccountDeleted = { onSignOutUser() },
                            onSignOut = { onSignOutUser() }
                        )
                    }
                    NavigationTab.WATCH -> {
                        WatchScreen(
                            videos = watchVideos,
                            isNepali = isNepali,
                            onToggleSubscribe = { ConnectNpRepository.toggleSubscribe(it) }
                        )
                    }
                    NavigationTab.HUB -> {
                        HubScreen(
                            jobs = jobs,
                            datingProfiles = datingProfiles,
                            datingMatches = datingMatches,
                            isNepali = isNepali,
                            isDarkMode = isDark,
                            onApplyJob = { ConnectNpRepository.applyForJob(it) },
                            onSwipeDating = { id, liked -> ConnectNpRepository.swipeDatingProfile(id, liked) },
                            onToggleLanguage = { ConnectNpRepository.toggleLanguage() },
                            onToggleDarkMode = { ConnectNpRepository.toggleDarkMode() },
                            onOpenLogin = { showLoginDialog = true },
                            onOpenSplash = { showSplashScreen = true }
                        )
                    }
                }
            }
        }
    }

    // Story Viewer Dialog
    if (activeStoryIndex != null && activeStoryIndex!! in stories.indices) {
        StoryViewerDialog(
            stories = stories,
            initialIndex = activeStoryIndex!!,
            onDismiss = { activeStoryIndex = null },
            onStoryViewed = { id -> ConnectNpRepository.markStoryViewed(id) }
        )
    }

    // Create Post Dialog
    if (showCreatePostDialog) {
        CreatePostDialog(
            isNepali = isNepali,
            currentUser = currentActiveUser,
            onDismiss = { showCreatePostDialog = false },
            onSubmitPost = { content, location, mediaResId, mediaUri ->
                val activeUser = ConnectNpRepository.getCurrentActiveUser()
                ConnectNpRepository.addPost(
                    content = content,
                    location = location,
                    mediaResId = mediaResId,
                    customAuthor = activeUser,
                    mediaUri = mediaUri
                )
                coroutineScope.launch {
                    try {
                        val repo = ConnectNpFirestoreRepository(context)
                        repo.createPost(
                            content = content,
                            location = location ?: "Kathmandu, Nepal",
                            authorName = activeUser.name,
                            authorHandle = activeUser.handle
                        )
                    } catch (e: Exception) {
                        Log.w("MainActivity", "Firestore post error: ${e.message}")
                    }
                }
                showCreatePostDialog = false
            }
        )
    }

    // User Search Dialog (TASK 3)
    if (showSearchDialog) {
        UserSearchDialog(
            isNepali = isNepali,
            onDismiss = { showSearchDialog = false }
        )
    }

    // Audio & Video Call Overlay
    if (activeCallUserAndVideo != null) {
        val (callUser, isVideo) = activeCallUserAndVideo!!
        CallOverlayDialog(
            user = callUser,
            isVideoCall = isVideo,
            onEndCall = { activeCallUserAndVideo = null }
        )
    }

    // QR Profile & Scanner Dialog
    if (showQrDialog) {
        QrScannerDialog(
            onDismiss = { showQrDialog = false },
            onSimulateScanSuccess = {
                currentTab = NavigationTab.CHATS
                showPayScreen = false
            }
        )
    }

    // Notifications Bottom Sheet
    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showNotificationsSheet = false
                ConnectNpRepository.clearNotifications()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNepali) "सूचनाहरू (Notifications)" else "Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    TextButton(onClick = { ConnectNpRepository.clearNotifications() }) {
                        Text(if (isNepali) "सबै पढेको चिन्ह लगाउनुहोस्" else "Mark all read")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (unreadNotifications == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isNepali) "कुनै नयाँ सूचना छैन" else "No notifications yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isNepali) "तपाईंका सबै गतिविधिहरूको सूचना यहाँ देखिनेछ।" else "All your activity alerts will appear here.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ConnectNpBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = ConnectNpBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNepali) "तपाईंको नयाँ गतिविधि सूचना प्राप्त भएको छ" else "You have new activity notifications",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(text = "Just now", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }

    // Splash Screen
    if (showSplashScreen) {
        ConnectNpSplashScreen(onFinish = { showSplashScreen = false })
    }

    // Login Dialog
    if (showLoginDialog) {
        ConnectNpLoginDialog(
            onDismiss = { showLoginDialog = false },
            onLoginSuccess = { showLoginDialog = false }
        )
    }
}
