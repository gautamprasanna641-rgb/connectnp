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
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.ConnectNpTheme
import com.example.ui.theme.NepalCrimson

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by ConnectNpRepository.isDarkMode.collectAsStateWithLifecycle()
            ConnectNpTheme(darkTheme = isDark) {
                ConnectNpMainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectNpMainApp() {
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

    // Modals / Dialogs
    var activeStoryIndex by remember { mutableStateOf<Int?>(null) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
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
                        ConnectNpRepository.sendMoney(recipient, amount, remarks)
                    },
                    onTopUp = { amount, source ->
                        ConnectNpRepository.topUpWallet(amount, source)
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
                            onStoryClick = { index -> activeStoryIndex = index },
                            onAddStoryClick = {
                                ConnectNpRepository.addStory("Loving ConnectNP Super-App vibes! 🇳🇵✨")
                                activeStoryIndex = 0
                            },
                            onCreatePostClick = { showCreatePostDialog = true },
                            onToggleReaction = { postId, reaction ->
                                ConnectNpRepository.toggleReaction(postId, reaction)
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
                            onNavigateToMarketplace = { currentTab = NavigationTab.MARKETPLACE },
                            onNavigateToReels = { currentTab = NavigationTab.REELS }
                        )
                    }
                    NavigationTab.REELS -> {
                        ReelsScreen(
                            reels = reels,
                            isNepali = isNepali,
                            onToggleLike = { ConnectNpRepository.toggleReelLike(it) },
                            onToggleSave = { ConnectNpRepository.toggleReelSave(it) }
                        )
                    }
                    NavigationTab.CHATS -> {
                        ChatScreen(
                            threads = chatThreads,
                            messagesByThread = messagesByThread,
                            isNepali = isNepali,
                            onSendMessage = { threadId, text, mediaType, duration ->
                                ConnectNpRepository.sendMessage(threadId, text, mediaType, duration)
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
    if (activeStoryIndex != null) {
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
            onDismiss = { showCreatePostDialog = false },
            onSubmitPost = { content, location, mediaResId ->
                ConnectNpRepository.addPost(content, location, mediaResId)
            }
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

                listOf(
                    Pair("Aayush Sharma reacted 🙏 Namaste to your post", "10m ago"),
                    Pair("Escrow payment received: रु 4,500 deposited into ConnectNP Pay", "1h ago"),
                    Pair("Pooja Shrestha shared a new 4K Himalayan reel", "3h ago"),
                    Pair("ConnectNP AI is ready to help translate and write posts", "1d ago")
                ).forEach { (notif, time) ->
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
                            Text(text = notif, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
