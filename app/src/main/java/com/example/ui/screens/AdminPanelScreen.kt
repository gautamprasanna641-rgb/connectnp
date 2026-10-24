package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ConnectNpFirestoreRepository
import com.example.data.ConnectNpRepository
import com.example.data.SampleDataProvider
import com.example.model.Comment
import com.example.model.FirestoreUserProfile
import com.example.model.Post
import com.example.model.User
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.ConnectNpSuccess
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.PoppinsFontFamily
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val userEmail = currentUser?.email

    // Security Gate: Ensure only the authorized admin email has access
    val isAuthorized = ConnectNpRepository.isUserAdmin(userEmail)
    if (!isAuthorized) {
        AccessDeniedView(userEmail = userEmail, onBack = onBack)
        return
    }

    val firestoreRepo = remember(context) { ConnectNpFirestoreRepository(context) }
    val posts by ConnectNpRepository.posts.collectAsState()
    val commentsByPost by ConnectNpRepository.commentsByPost.collectAsState()
    val verifiedUserIds by ConnectNpRepository.verifiedUserIds.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Users state
    var firestoreUsers by remember { mutableStateOf<List<FirestoreUserProfile>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(false) }

    fun refreshUsers() {
        isLoadingUsers = true
        coroutineScope.launch {
            val result = firestoreRepo.getAllUsers()
            isLoadingUsers = false
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                firestoreUsers = list
                // Sync any verified users from Firestore
                val verifiedFromRemote = list.filter { it.isVerified }.map { it.userId }.toSet()
                if (verifiedFromRemote.isNotEmpty()) {
                    ConnectNpRepository.setVerifiedUserIds(verifiedFromRemote)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshUsers()
    }

    // Confirmation dialogs
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    var commentToDelete by remember { mutableStateOf<Pair<String, Comment>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Admin Control Panel",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NepalCrimson,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "OWNER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = userEmail ?: ConnectNpRepository.ADMIN_EMAIL,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshUsers() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("admin_panel_screen")
        ) {
            // Quick Stat Badges
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AdminStatItem(label = "Verified", value = verifiedUserIds.size.toString(), color = ConnectNpBlue)
                    AdminStatItem(label = "Total Posts", value = posts.size.toString(), color = NepalCrimson)
                    val totalComments = commentsByPost.values.sumOf { it.size }
                    AdminStatItem(label = "Comments", value = totalComments.toString(), color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0; searchQuery = "" },
                    text = { Text("Users (${firestoreUsers.size.coerceAtLeast(1)})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.PeopleAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1; searchQuery = "" },
                    text = { Text("Posts (${posts.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2; searchQuery = "" },
                    text = { Text("Comments", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        when (selectedTabIndex) {
                            0 -> "Search users by name or handle..."
                            1 -> "Search posts by author or content..."
                            else -> "Search comments..."
                        },
                        fontSize = 12.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .height(52.dp)
                    .testTag("admin_search_field"),
                singleLine = true
            )

            // Content per tab
            when (selectedTabIndex) {
                0 -> {
                    // Users Management Tab
                    AdminUsersTab(
                        firestoreUsers = firestoreUsers,
                        verifiedUserIds = verifiedUserIds,
                        searchQuery = searchQuery,
                        isLoading = isLoadingUsers,
                        onToggleVerification = { userId, grant ->
                            ConnectNpRepository.toggleUserVerification(userId, grant, context)
                            // Also update local list state
                            firestoreUsers = firestoreUsers.map { u ->
                                if (u.userId == userId) u.copy(isVerified = grant) else u
                            }
                            val msg = if (grant) "Blue Tick granted to user!" else "Blue Tick removed from user!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                1 -> {
                    // Posts Moderation Tab
                    AdminPostsTab(
                        posts = posts,
                        searchQuery = searchQuery,
                        onDeletePost = { post -> postToDelete = post }
                    )
                }
                2 -> {
                    // Comments Moderation Tab
                    AdminCommentsTab(
                        commentsByPost = commentsByPost,
                        posts = posts,
                        searchQuery = searchQuery,
                        onDeleteComment = { postId, comment -> commentToDelete = Pair(postId, comment) }
                    )
                }
            }
        }
    }

    // Delete Post Confirmation Dialog
    if (postToDelete != null) {
        val p = postToDelete!!
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = NepalCrimson, modifier = Modifier.size(32.dp)) },
            title = { Text("Delete Post Permanently?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to delete this post by ${p.author.name}?")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${p.content.take(80)}${if (p.content.length > 80) "..." else ""}\"",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ConnectNpRepository.deletePost(p.id, context)
                        Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                        postToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                    modifier = Modifier.testTag("admin_confirm_delete_post")
                ) {
                    Text("Delete Post", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { postToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Comment Confirmation Dialog
    if (commentToDelete != null) {
        val (postId, comment) = commentToDelete!!
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = NepalCrimson, modifier = Modifier.size(32.dp)) },
            title = { Text("Delete Comment?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Delete comment by ${comment.author.name}?")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${comment.text}\"",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ConnectNpRepository.deleteComment(postId, comment.id, context)
                        Toast.makeText(context, "Comment deleted successfully", Toast.LENGTH_SHORT).show()
                        commentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                    modifier = Modifier.testTag("admin_confirm_delete_comment")
                ) {
                    Text("Delete Comment", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { commentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AdminUsersTab(
    firestoreUsers: List<FirestoreUserProfile>,
    verifiedUserIds: Set<String>,
    searchQuery: String,
    isLoading: Boolean,
    onToggleVerification: (userId: String, grant: Boolean) -> Unit
) {
    val activeUser = ConnectNpRepository.getCurrentActiveUser()

    // Aggregate known users: Firestore records + current user + sample AI Bot user
    val combinedUsers = remember(firestoreUsers, activeUser) {
        val list = mutableListOf<FirestoreUserProfile>()
        list.addAll(firestoreUsers)
        if (list.none { it.userId == activeUser.id }) {
            list.add(
                FirestoreUserProfile(
                    id = activeUser.id,
                    userId = activeUser.id,
                    name = activeUser.name,
                    handle = activeUser.handle,
                    bio = activeUser.bio,
                    photoUrl = activeUser.avatarUrl ?: "",
                    isVerified = activeUser.isVerified
                )
            )
        }
        if (list.none { it.userId == SampleDataProvider.aiBotUser.id }) {
            list.add(
                FirestoreUserProfile(
                    id = SampleDataProvider.aiBotUser.id,
                    userId = SampleDataProvider.aiBotUser.id,
                    name = SampleDataProvider.aiBotUser.name,
                    handle = SampleDataProvider.aiBotUser.handle,
                    bio = SampleDataProvider.aiBotUser.bio,
                    isVerified = true
                )
            )
        }
        list
    }

    val filtered = remember(combinedUsers, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) combinedUsers else {
            combinedUsers.filter {
                it.name.lowercase().contains(q) ||
                it.handle.lowercase().contains(q) ||
                it.userId.lowercase().contains(q)
            }
        }
    }

    if (isLoading && firestoreUsers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
    } else if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No users found matching \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.userId.ifBlank { it.id } }) { user ->
                val isVerified = user.userId in verifiedUserIds || user.isVerified
                AdminUserCard(
                    user = user,
                    isVerified = isVerified,
                    onToggleVerification = { onToggleVerification(user.userId, !isVerified) }
                )
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: FirestoreUserProfile,
    isVerified: Boolean,
    onToggleVerification: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_user_card_${user.userId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ConnectNpBlue),
                contentAlignment = Alignment.Center
            ) {
                if (user.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = user.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified Blue Tick",
                            tint = ConnectNpBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = if (user.handle.startsWith("@")) user.handle else "@${user.handle}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Blue Tick Verification Button
            if (isVerified) {
                OutlinedButton(
                    onClick = onToggleVerification,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NepalCrimson),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("admin_toggle_verify_${user.userId}")
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Revoke", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onToggleVerification,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("admin_toggle_verify_${user.userId}")
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verify", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminPostsTab(
    posts: List<Post>,
    searchQuery: String,
    onDeletePost: (Post) -> Unit
) {
    val filtered = remember(posts, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) posts else {
            posts.filter {
                it.content.lowercase().contains(q) ||
                it.author.name.lowercase().contains(q) ||
                it.author.handle.lowercase().contains(q)
            }
        }
    }

    if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No posts found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { post ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_post_card_${post.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(ConnectNpBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = post.author.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = post.author.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (post.author.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Filled.Verified,
                                                contentDescription = "Verified",
                                                tint = ConnectNpBlue,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${post.author.handle} • ${post.timestamp}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Delete Post Button
                            IconButton(
                                onClick = { onDeletePost(post) },
                                modifier = Modifier.testTag("admin_delete_post_${post.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Post",
                                    tint = NepalCrimson
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = post.content, fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "❤️ ${post.likesCount} likes • 💬 ${post.commentsCount} comments",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ID: ${post.id.take(16)}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCommentsTab(
    commentsByPost: Map<String, List<Comment>>,
    posts: List<Post>,
    searchQuery: String,
    onDeleteComment: (postId: String, Comment) -> Unit
) {
    // Flatten comments into pair of (postId, Comment)
    val allComments = remember(commentsByPost) {
        commentsByPost.flatMap { (postId, comments) ->
            comments.map { comment -> Pair(postId, comment) }
        }
    }

    val filtered = remember(allComments, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) allComments else {
            allComments.filter { (_, comment) ->
                comment.text.lowercase().contains(q) ||
                comment.author.name.lowercase().contains(q) ||
                comment.author.handle.lowercase().contains(q)
            }
        }
    }

    if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No comments found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.second.id }) { (postId, comment) ->
                val parentPost = posts.find { it.id == postId }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_comment_card_${comment.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = comment.author.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = comment.timeAgo,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = comment.text, fontSize = 12.sp)
                            if (parentPost != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "On post: \"${parentPost.content.take(35)}...\"",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteComment(postId, comment) },
                            modifier = Modifier.testTag("admin_delete_comment_${comment.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Comment",
                                tint = NepalCrimson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccessDeniedView(userEmail: String?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = NepalCrimson,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Access Restricted",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The Admin Control Panel is restricted to the app owner (${ConnectNpRepository.ADMIN_EMAIL}). Current account: ${userEmail ?: "Anonymous"}.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue)
        ) {
            Text("Return to Settings")
        }
    }
}
