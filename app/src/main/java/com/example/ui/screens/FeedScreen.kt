package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.ConnectNpFirestoreRepository
import com.example.data.ConnectNpRepository
import com.example.data.SampleDataProvider
import com.example.model.*
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.ConnectNpSuccess
import com.example.ui.theme.NepalCrimson
import com.example.util.NetworkConnectivityObserver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    posts: List<Post>,
    stories: List<Story>,
    isNepali: Boolean,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onToggleReaction: (String, ReactionType) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onSharePost: (String) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onNavigateToMarketplace: () -> Unit,
    onNavigateToReels: () -> Unit,
    currentUser: User? = null
) {
    val context = LocalContext.current
    val activeUser = currentUser ?: remember { ConnectNpRepository.getCurrentActiveUser() }
    val blockedUserIds by ConnectNpRepository.blockedUserIds.collectAsState()
    val reportedPostIds by ConnectNpRepository.reportedPostIds.collectAsState()

    // Observe network connectivity (TASK 5)
    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val isOnline by connectivityObserver.isOnline.collectAsState(initial = true)

    // Filter out blocked users & reported content
    val filteredPosts = remember(posts, blockedUserIds, reportedPostIds) {
        posts.filter { post ->
            !ConnectNpRepository.isUserBlocked(post.author.id, post.author.handle, post.author.name) &&
            post.id !in reportedPostIds
        }
    }

    var selectedFeedTab by remember { mutableIntStateOf(0) } // 0: For You, 1: Following, 2: Local Nepal
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var activeReactionPickerPostId by remember { mutableStateOf<String?>(null) }

    val tabTitles = if (isNepali) {
        listOf("सिफारिस (For You)", "पछ्याउँदै (Following)", "नेपाल बज (Buzz)")
    } else {
        listOf("For You", "Following", "Kathmandu Buzz")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("feed_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Offline Banner (TASK 5)
        if (!isOnline) {
            item {
                Surface(
                    color = Color(0xFF263238),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNepali) "अफलाइन मोड — सुरक्षित क्यास सामग्री देखाइँदैछ" else "Offline Mode — Showing cached Himalayan posts",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Feed Category Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedFeedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 12.dp,
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFeedTab == index,
                        onClick = { selectedFeedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedFeedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }
        }

        // Stories Bar
        item {
            val visibleStories = remember(stories, blockedUserIds) {
                stories.filter { !ConnectNpRepository.isUserBlocked(it.author.id, it.author.handle, it.author.name) }
            }
            StoriesSection(
                stories = visibleStories,
                isNepali = isNepali,
                onStoryClick = onStoryClick,
                onAddStoryClick = onAddStoryClick
            )
        }

        // Create Post Prompt Box
        item {
            CreatePostHeaderBox(
                user = activeUser,
                isNepali = isNepali,
                onCreatePostClick = onCreatePostClick
            )
        }

        // Search Bar Card (TASK 3)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenSearch() }
                    .testTag("feed_search_card"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ConnectNpBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isNepali) "प्रयोगकर्ता वा साथीहरू खोज्नुहोस्..." else "Search users, creators & Nepali community...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Filtered Posts List (Hiding blocked users and reported posts)
        items(filteredPosts, key = { it.id }) { post ->
            PostCardItem(
                post = post,
                isNepali = isNepali,
                isReactionPickerOpen = activeReactionPickerPostId == post.id,
                onToggleReactionPicker = {
                    activeReactionPickerPostId = if (activeReactionPickerPostId == post.id) null else post.id
                },
                onSelectReaction = { reaction ->
                    onToggleReaction(post.id, reaction)
                    activeReactionPickerPostId = null
                },
                onVotePoll = { optionId -> onVotePoll(post.id, optionId) },
                onToggleBookmark = { onToggleBookmark(post.id) },
                onOpenComments = { activeCommentPostId = post.id },
                onSharePost = { onSharePost(post.id) },
                onBlockAuthor = { authorId, authorName, authorHandle ->
                    ConnectNpRepository.blockUser(authorId, authorName, context, authorHandle)
                    Toast.makeText(context, "Blocked $authorName", Toast.LENGTH_SHORT).show()
                },
                onReportPost = { postId, authorId, reason ->
                    ConnectNpRepository.reportPost(postId, authorId, reason, context)
                    Toast.makeText(context, "Report submitted. Thank you for keeping CONNECTNP safe.", Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Comments Sheet
    if (activeCommentPostId != null) {
        val currentPost = posts.find { it.id == activeCommentPostId }
        if (currentPost != null) {
            CommentBottomSheet(
                post = currentPost,
                isNepali = isNepali,
                onDismiss = { activeCommentPostId = null },
                onAddComment = { commentText ->
                    onAddComment(currentPost.id, commentText)
                }
            )
        }
    }
}

@Composable
fun StoriesSection(
    stories: List<Story>,
    isNepali: Boolean,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // "Add Story" Card
            item {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(138.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onAddStoryClick() }
                        .testTag("add_story_card"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(ConnectNpBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = ConnectNpBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .offset(y = (-14).dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ConnectNpBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = if (isNepali) "तपाईंको कथा" else "Add Story",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(y = (-10).dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Other User Stories
            items(stories.drop(1).size) { index ->
                val story = stories[index + 1]
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(138.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (story.isViewed) 1.dp else 2.5.dp,
                            brush = if (story.isViewed)
                                Brush.linearGradient(listOf(Color.LightGray, Color.Gray))
                            else
                                Brush.linearGradient(listOf(ConnectNpBlue, NepalCrimson)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onStoryClick(index + 1) }
                        .testTag("story_card_$index")
                ) {
                    if (story.mediaResId != null) {
                        Image(
                            painter = painterResource(id = story.mediaResId),
                            contentDescription = story.caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(ConnectNpBlue, NepalCrimson)
                                    )
                                )
                        )
                    }

                    // Dark gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )

                    // Avatar top left
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ConnectNpBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = story.author.name.take(1),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Author name bottom
                    Text(
                        text = story.author.name.split(" ").first(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePostHeaderBox(
    user: User,
    isNepali: Boolean,
    onCreatePostClick: () -> Unit
) {
    val userFirstName = user.name.split(" ").firstOrNull()?.ifBlank { "there" } ?: "there"
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ConnectNpBlue),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.avatarUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = user.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = user.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { onCreatePostClick() }
                        .testTag("feed_create_post_bar")
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = if (isNepali) "के सोच्दै हुनुहुन्छ, $userFirstName?" else "What's on your mind, $userFirstName?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp))

            // Action short buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCreatePostClick() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = "Photo",
                        tint = ConnectNpSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isNepali) "तस्बिर" else "Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCreatePostClick() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Poll,
                        contentDescription = "Poll",
                        tint = ConnectNpBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isNepali) "मतदान" else "Poll", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCreatePostClick() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = NepalCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isNepali) "स्थान" else "Location", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun PostCardItem(
    post: Post,
    isNepali: Boolean,
    isReactionPickerOpen: Boolean,
    onToggleReactionPicker: () -> Unit,
    onSelectReaction: (ReactionType) -> Unit,
    onVotePoll: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenComments: () -> Unit,
    onSharePost: () -> Unit = {},
    onBlockAuthor: (String, String, String) -> Unit = { _, _, _ -> },
    onReportPost: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Author row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ConnectNpBlue),
                    contentAlignment = Alignment.Center
                ) {
                    if (!post.author.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = post.author.avatarUrl,
                            contentDescription = post.author.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = post.author.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.author.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (post.author.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = ConnectNpBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (post.locationName != null) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = NepalCrimson,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${post.locationName} • ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = post.timestamp,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bookmark Icon
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (post.isBookmarked) ConnectNpBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Three-dot Menu (TASK 2: Play Store UGC Compliance)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("post_menu_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isNepali) "पोस्ट रिपोर्ट गर्नुहोस्" else "Report Post") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            },
                            onClick = {
                                showMenu = false
                                showReportDialog = true
                            },
                            modifier = Modifier.testTag("report_post_menu_item")
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isNepali) "ब्लक गर्नुहोस् (${post.author.name})" else "Block ${post.author.name}",
                                    color = NepalCrimson
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Block, contentDescription = null, tint = NepalCrimson)
                            },
                            onClick = {
                                showMenu = false
                                showBlockDialog = true
                            },
                            modifier = Modifier.testTag("block_user_menu_item")
                        )
                    }
                }
            }

            // Post Content Text
            Text(
                text = post.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Post Media / Image with Coil & Asynchronous Loading Placeholder (TASK 5)
            if (post.mediaResId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.mediaResId)
                        .crossfade(true)
                        .build(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = ConnectNpBlue,
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BrokenImage,
                                contentDescription = "Error loading image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    contentDescription = "Post media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )
            }

            // Poll View if attached
            if (post.poll != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📊 ${post.poll.question}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        post.poll.options.forEach { option ->
                            val isVoted = post.poll.userVotedOptionId == option.id
                            val percent = if (post.poll.totalVotes > 0) {
                                (option.votes.toFloat() / post.poll.totalVotes * 100).toInt()
                            } else 0

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isVoted) ConnectNpBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onVotePoll(option.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = option.text,
                                        fontSize = 13.sp,
                                        fontWeight = if (isVoted) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isVoted) ConnectNpBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$percent% (${option.votes})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stats row (reactions count, comments count)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "👍 ❤️ 🙏 ${post.likesCount}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${post.commentsCount} comments • ${post.sharesCount} shares",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 14.dp))

            // Reaction Picker overlay popup
            AnimatedVisibility(visible = isReactionPickerOpen) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ReactionType.values().forEach { reaction ->
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { onSelectReaction(reaction) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = reaction.emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Reaction Button
                TextButton(
                    onClick = onToggleReactionPicker,
                    modifier = Modifier.testTag("reaction_button_${post.id}")
                ) {
                    val currentReaction = post.userReaction
                    if (currentReaction != null) {
                        Text(text = "${currentReaction.emoji} ${currentReaction.label}", color = ConnectNpBlue, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isNepali) "मन पर्यो" else "Like")
                    }
                }

                // Comment Button
                TextButton(
                    onClick = onOpenComments,
                    modifier = Modifier.testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isNepali) "टिप्पणी" else "Comment")
                }

                // Share Button (TASK 4: Native Android Share Intent)
                val cardContext = LocalContext.current
                TextButton(
                    onClick = {
                        val shareContent = buildString {
                            append("${post.author.name} on CONNECTNP 🇳🇵:\n\n")
                            append(post.content)
                            if (post.locationName != null) {
                                append("\n📍 ${post.locationName}")
                            }
                            append("\n\nJoin CONNECTNP: https://connectnp.app/post/${post.id}")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareContent)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(
                            sendIntent,
                            if (isNepali) "पोस्ट शेयर गर्नुहोस्" else "Share Post via"
                        )
                        cardContext.startActivity(shareIntent)
                        onSharePost()
                    },
                    modifier = Modifier.testTag("share_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isNepali) "शेयर" else "Share")
                }
            }
        }
    }

    // Report Dialog (Play Store UGC Safety Compliance)
    if (showReportDialog) {
        var selectedReason by remember { mutableStateOf("Inappropriate Content") }
        val reasons = listOf(
            "Spam or misleading scam",
            "Inappropriate content or nudity",
            "Harassment or hate speech",
            "Misinformation or false news",
            "Violence or physical threat"
        )
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            icon = { Icon(Icons.Default.Flag, contentDescription = null, tint = NepalCrimson) },
            title = { Text(if (isNepali) "पोस्ट रिपोर्ट गर्नुहोस्" else "Report Post", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isNepali) "कृपया रिपोर्ट गर्नुको कारण छान्नुहोस्:" else "Why are you reporting this post?",
                        fontSize = 13.sp
                    )
                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(reason, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportPost(post.id, post.author.id, selectedReason)
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson)
                ) {
                    Text(if (isNepali) "रिपोर्ट पेस गर्नुहोस्" else "Submit Report")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Block User Dialog (Play Store UGC Safety Compliance)
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            icon = { Icon(Icons.Default.Block, contentDescription = null, tint = NepalCrimson) },
            title = { Text(if (isNepali) "प्रयोगकर्ता ब्लक गर्नुहोस्?" else "Block ${post.author.name}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (isNepali)
                        "के तपाईं @${post.author.handle} लाई ब्लक गर्न निश्चित हुनुहुन्छ? तपाईंले उहाँका पोस्ट, कमेन्ट वा सन्देशहरू देख्नुहुने छैन।"
                    else
                        "Are you sure you want to block @${post.author.handle}? You will no longer see their posts, stories, or messages.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBlockAuthor(post.author.id, post.author.name, post.author.handle)
                        showBlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson)
                ) {
                    Text(if (isNepali) "हो, ब्लक गर्नुहोस्" else "Block User")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    post: Post,
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestoreRepo = remember { ConnectNpFirestoreRepository(context) }
    var newCommentText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val firestoreComments by firestoreRepo.observeComments(post.id).collectAsState(initial = emptyList())
    val localRepoComments by ConnectNpRepository.commentsByPost.collectAsState()
    val postLocalComments = localRepoComments[post.id] ?: emptyList()

    val allComments = remember(firestoreComments, postLocalComments, post.id) {
        val list = mutableListOf<Comment>()
        if (post.id == "p1" && firestoreComments.isEmpty() && postLocalComments.isEmpty()) {
            list.add(Comment("c1", SampleDataProvider.aayushUser, "Proud of this movement! Connecting Nepal with world-class tech 🇳🇵", "1h ago", 12, true))
            list.add(Comment("c2", SampleDataProvider.poojaUser, "Amazing photography and initiative! Looking forward to the meetup.", "45m ago", 8, false))
        }
        firestoreComments.forEach { fc ->
            list.add(
                Comment(
                    id = fc.id,
                    author = User(
                        id = fc.userId,
                        name = fc.authorName.ifBlank { "ConnectNP User" },
                        handle = if (fc.authorHandle.startsWith("@")) fc.authorHandle else "@${fc.authorHandle}",
                        avatarUrl = fc.authorAvatarUrl.ifBlank { null }
                    ),
                    text = fc.text,
                    timeAgo = "Just now"
                )
            )
        }
        postLocalComments.forEach { lc ->
            if (list.none { it.id == lc.id || (it.text == lc.text && it.author.id == lc.author.id) }) {
                list.add(lc)
            }
        }
        list.filter { !ConnectNpRepository.isUserBlocked(it.author.id, it.author.handle, it.author.name) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("comment_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNepali) "टिप्पणीहरू (${allComments.size})" else "Comments (${allComments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Cloud Firestore ☁️",
                    fontSize = 10.sp,
                    color = ConnectNpBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Comments List
            if (allComments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isNepali) "कुनै टिप्पणी छैन। पहिलो टिप्पणी गर्नुहोस्! 🇳🇵" else "No comments yet. Start the conversation! 🇳🇵",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allComments, key = { it.id }) { comment ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ConnectNpBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!comment.author.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = comment.author.avatarUrl,
                                        contentDescription = comment.author.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = comment.author.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = comment.author.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.text,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.timeAgo,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add Comment Input Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text(if (isNepali) "टिप्पणी लेख्नुहोस्..." else "Write a comment...") },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    singleLine = true,
                    enabled = !isSending
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val commentToSend = newCommentText.trim()
                        if (commentToSend.isNotBlank()) {
                            isSending = true
                            val activeUser = ConnectNpRepository.getCurrentActiveUser()
                            onAddComment(commentToSend)

                            coroutineScope.launch {
                                try {
                                    firestoreRepo.addComment(
                                        postId = post.id,
                                        text = commentToSend,
                                        authorName = activeUser.name,
                                        authorHandle = activeUser.handle,
                                        authorAvatarUrl = activeUser.avatarUrl ?: ""
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.w("FeedScreen", "Comment saved locally, Firestore error: ${e.message}")
                                } finally {
                                    isSending = false
                                }
                            }
                            newCommentText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ConnectNpBlue)
                        .testTag("comment_send_button"),
                    enabled = !isSending && newCommentText.isNotBlank()
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}
