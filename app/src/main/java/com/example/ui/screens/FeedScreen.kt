package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SampleDataProvider
import com.example.model.*
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.ConnectNpSuccess
import com.example.ui.theme.NepalCrimson

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
    onNavigateToMarketplace: () -> Unit,
    onNavigateToReels: () -> Unit
) {
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
            StoriesSection(
                stories = stories,
                isNepali = isNepali,
                onStoryClick = onStoryClick,
                onAddStoryClick = onAddStoryClick
            )
        }

        // Create Post Prompt Box
        item {
            CreatePostHeaderBox(
                isNepali = isNepali,
                onCreatePostClick = onCreatePostClick
            )
        }

        // Posts List
        items(posts, key = { it.id }) { post ->
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
                onOpenComments = { activeCommentPostId = post.id }
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
    isNepali: Boolean,
    onCreatePostClick: () -> Unit
) {
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
                    Text(
                        text = SampleDataProvider.currentUser.name.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
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
                            text = if (isNepali) "के सोच्दै हुनुहुन्छ, प्रसन्न?" else "What's on your mind, Prasanna?",
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
    onOpenComments: () -> Unit
) {
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
                    Text(
                        text = post.author.name.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
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

                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (post.isBookmarked) ConnectNpBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            // Post Media / Image
            if (post.mediaResId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = post.mediaResId),
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

                // Share Button
                TextButton(onClick = { /* Share */ }) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    post: Post,
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    val sampleComments = remember {
        mutableStateListOf(
            Comment("c1", SampleDataProvider.aayushUser, "Proud of this movement! Connecting Nepal with world-class tech 🇳🇵", "1h ago", 12, true),
            Comment("c2", SampleDataProvider.poojaUser, "Amazing photography and initiative! Looking forward to the meetup.", "45m ago", 8, false)
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = if (isNepali) "टिप्पणीहरू (${sampleComments.size + post.commentsCount})" else "Comments (${sampleComments.size + post.commentsCount})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Existing Comments
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sampleComments) { comment ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ConnectNpBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(comment.author.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text(comment.author.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(comment.text, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(comment.timeAgo, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            sampleComments.add(
                                Comment(
                                    id = "c_${System.currentTimeMillis()}",
                                    author = SampleDataProvider.currentUser,
                                    text = newCommentText,
                                    timeAgo = "Just now"
                                )
                            )
                            onAddComment(newCommentText)
                            newCommentText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ConnectNpBlue)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}
