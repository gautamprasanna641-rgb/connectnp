package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ConnectNpFirestoreRepository
import com.example.data.ConnectNpRepository
import com.example.data.SampleDataProvider
import com.example.model.Post
import com.example.ui.components.EditProfileDialog
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.PoppinsFontFamily
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isNepali: Boolean,
    onNavigateToSettings: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenQr: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val firestoreRepo = remember { ConnectNpFirestoreRepository(context) }

    val userProfileDoc by firestoreRepo.observeUserProfile().collectAsState(initial = null)

    LaunchedEffect(userProfileDoc) {
        userProfileDoc?.let {
            ConnectNpRepository.updateActiveUser(profileDoc = it)
        }
    }

    var displayName by remember(currentUser, userProfileDoc) {
        val resolved = userProfileDoc?.name?.takeIf { it.isNotBlank() }
            ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "ConnectNP Member"
        mutableStateOf(resolved)
    }
    var userBio by remember(userProfileDoc) {
        mutableStateOf(userProfileDoc?.bio ?: "Passionate creator on CONNECTNP • Building Nepal's digital future 🇳🇵")
    }
    var userPhotoUrl by remember(currentUser, userProfileDoc) {
        mutableStateOf(userProfileDoc?.photoUrl?.ifBlank { null } ?: currentUser?.photoUrl?.toString())
    }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Posts, 1: Bookmarks, 2: Badges

    val posts by ConnectNpRepository.posts.collectAsState()
    val myPosts = remember(posts, currentUser) {
        posts.filter { it.author.id == "user_me" || it.author.id == (currentUser?.uid ?: "") }
    }
    val bookmarkedPosts = remember(posts) {
        posts.filter { it.isBookmarked }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Hero Profile Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ConnectNpBlue.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(3.dp, ConnectNpBlue, CircleShape)
                            .background(
                                Brush.linearGradient(listOf(ConnectNpBlue, NepalCrimson))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = displayName.take(1).uppercase(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified Nepal Citizen",
                            tint = ConnectNpBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "@${currentUser?.email?.substringBefore("@") ?: "connectnp_user"}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = userBio,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Follower & Post Stats Row (Dynamic from Firestore)
                    val followersCount = userProfileDoc?.followersCount ?: 0L
                    val followingCount = userProfileDoc?.followingCount ?: 0L
                    val followersDisplay = if (followersCount <= 0L) "0" else String.format("%,d", followersCount)
                    val followingDisplay = if (followingCount <= 0L) "0" else String.format("%,d", followingCount)
                    val postsDisplay = if (myPosts.isEmpty()) "0" else "${myPosts.size}"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(count = postsDisplay, label = if (isNepali) "पोस्टहरू" else "Posts")
                        VerticalDivider(modifier = Modifier.height(30.dp))
                        StatItem(count = followersDisplay, label = if (isNepali) "फलोअरहरू" else "Followers")
                        VerticalDivider(modifier = Modifier.height(30.dp))
                        StatItem(count = followingDisplay, label = if (isNepali) "फलो गर्दै" else "Following")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Profile Actions (Edit Profile, Settings, Wallet)
                    Row(
                        modifier = Modifier.fillMaxWidth(0.95f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showEditDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isNepali) "सम्पादन गर्नुहोस्" else "Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenQr,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = "QR Code", modifier = Modifier.size(16.dp))
                        }

                        OutlinedButton(
                            onClick = onNavigateToSettings,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("profile_settings_shortcut")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Tab Selector (My Posts / Bookmarks / Badges)
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ConnectNpBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isNepali) "मेरो पोस्टहरू (${myPosts.size})" else "My Posts (${myPosts.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isNepali) "सुरक्षित (${bookmarkedPosts.size})" else "Saved (${bookmarkedPosts.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(if (isNepali) "प्रमाणपत्र र ब्याज" else "Badges & Trust", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                )
            }
        }

        // Tab Contents
        when (selectedTab) {
            0 -> {
                if (myPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Article, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (isNepali) "कुनै पोस्ट छैन" else "No posts authored yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(myPosts, key = { it.id }) { post ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = post.timestamp, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = post.content, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("❤️ ${post.likesCount} Likes", fontSize = 11.sp, color = NepalCrimson)
                                    Text("💬 ${post.commentsCount} Comments", fontSize = 11.sp, color = ConnectNpBlue)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (bookmarkedPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isNepali) "कुनै सुरक्षित पोस्ट छैन" else "No bookmarked posts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(bookmarkedPosts, key = { it.id }) { post ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "By ${post.author.name} • ${post.timestamp}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = post.content, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            2 -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TrustBadgeCard(
                            title = "Nepal Verified Identity 🇳🇵",
                            subtitle = "Verified Member Account",
                            icon = Icons.Default.VerifiedUser,
                            color = ConnectNpBlue
                        )
                        TrustBadgeCard(
                            title = "ConnectNP Pay Escrow Enabled 💰",
                            subtitle = "Zero-fraud protection on all marketplace transactions",
                            icon = Icons.Default.AccountBalanceWallet,
                            color = Color(0xFF2E7D32)
                        )
                        TrustBadgeCard(
                            title = "Privacy Protected & Encrypted 🛡️",
                            subtitle = "End-to-end messaging protocol & safe community",
                            icon = Icons.Default.Security,
                            color = NepalCrimson
                        )
                    }
                }
            }
        }
    }

    // Edit Profile Dialog (TASK 1: Firestore & Storage Picture Upload)
    if (showEditDialog) {
        EditProfileDialog(
            initialName = displayName,
            initialBio = userBio,
            initialPhotoUrl = userPhotoUrl,
            isNepali = isNepali,
            onDismiss = { showEditDialog = false },
            onProfileUpdated = { newName, newBio, newPhotoUrl ->
                displayName = newName
                userBio = newBio
                userPhotoUrl = newPhotoUrl
                ConnectNpRepository.updateActiveUser(name = newName, bio = newBio, photoUrl = newPhotoUrl)
            }
        )
    }
}

@Composable
fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TrustBadgeCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
