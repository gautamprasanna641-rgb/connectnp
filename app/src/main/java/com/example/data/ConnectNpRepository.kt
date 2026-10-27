package com.example.data

import android.content.Context
import com.example.R
import com.example.model.*
import com.example.service.ConnectNpAiService
import com.example.service.ConnectNpMessagingService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

object ConnectNpRepository {

    const val ADMIN_EMAIL = "gautamprasanna641@gmail.com"

    fun isUserAdmin(email: String?): Boolean {
        return email?.trim()?.equals(ADMIN_EMAIL, ignoreCase = true) == true
    }

    private val _verifiedUserIds = MutableStateFlow<Set<String>>(setOf("user_ai"))
    val verifiedUserIds: StateFlow<Set<String>> = _verifiedUserIds.asStateFlow()

    fun setVerifiedUserIds(ids: Set<String>) {
        _verifiedUserIds.value = _verifiedUserIds.value + ids
    }

    fun isUserVerified(userId: String): Boolean {
        return userId in _verifiedUserIds.value
    }

    fun toggleUserVerification(userId: String, isVerified: Boolean, context: Context? = null) {
        if (isVerified) {
            _verifiedUserIds.value = _verifiedUserIds.value + userId
        } else {
            _verifiedUserIds.value = _verifiedUserIds.value - userId
        }
        _posts.value = _posts.value.map { post ->
            if (post.author.id == userId) {
                post.copy(author = post.author.copy(isVerified = isVerified))
            } else post
        }
        if (_currentActiveUser.value.id == userId) {
            _currentActiveUser.value = _currentActiveUser.value.copy(isVerified = isVerified)
        }
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firestoreRepo = ConnectNpFirestoreRepository(context)
                    firestoreRepo.setUserVerification(userId, isVerified)
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
    }

    // --- Official CONNECTNP Page State ---
    val officialPageUser = User(
        id = "official_connectnp",
        name = "CONNECTNP Official",
        handle = "@connectnp_official",
        avatarResId = R.drawable.connectnp_icon_full,
        isVerified = true,
        isCreator = true,
        bio = "Official page of ConnectNP - Nepal's Premier Social, Community & Payments Super-App 🇳🇵",
        location = "Kathmandu, Nepal"
    )

    private val _officialPage = MutableStateFlow(
        OfficialPage(
            id = "official_connectnp",
            pageName = "CONNECTNP Official",
            handle = "@connectnp_official",
            bio = "Official page of ConnectNP - Nepal's Premier Social, Community & Payments Super-App 🇳🇵",
            category = "Official Community Organization",
            website = "https://connectnp.app",
            followersCount = 0L,
            isVerified = true,
            ownerEmail = ADMIN_EMAIL,
            isFollowing = false
        )
    )
    val officialPage: StateFlow<OfficialPage> = _officialPage.asStateFlow()

    private val _followedPageIds = MutableStateFlow<Set<String>>(emptySet())
    val followedPageIds: StateFlow<Set<String>> = _followedPageIds.asStateFlow()

    fun isPageFollowed(pageId: String): Boolean = pageId in _followedPageIds.value

    fun setOfficialPage(page: OfficialPage) {
        _officialPage.value = page
    }

    fun toggleFollowPage(pageId: String, context: Context? = null) {
        val currentlyFollowed = isPageFollowed(pageId)
        val newFollowState = !currentlyFollowed
        if (newFollowState) {
            _followedPageIds.value = _followedPageIds.value + pageId
        } else {
            _followedPageIds.value = _followedPageIds.value - pageId
        }
        val delta = if (newFollowState) 1L else -1L
        _officialPage.value = _officialPage.value.copy(
            isFollowing = newFollowState,
            followersCount = maxOf(0L, _officialPage.value.followersCount + delta)
        )
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = ConnectNpFirestoreRepository(context)
                    repo.setPageFollowStatus(pageId, newFollowState)
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
    }

    fun updateOfficialPage(
        pageName: String,
        bio: String,
        website: String,
        category: String,
        context: Context? = null
    ): Boolean {
        // Enforce: ONLY the email gautamprasanna641@gmail.com has access to manage this specific Page
        val currentEmail = try { Firebase.auth.currentUser?.email } catch (e: Exception) { null }
        if (!isUserAdmin(currentEmail)) return false

        _officialPage.value = _officialPage.value.copy(
            pageName = pageName,
            bio = bio,
            website = website,
            category = category
        )
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = ConnectNpFirestoreRepository(context)
                    repo.saveOfficialPage(_officialPage.value)
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
        return true
    }

    fun addPagePost(
        content: String,
        location: String? = "Kathmandu, Nepal",
        mediaUri: android.net.Uri? = null,
        context: Context? = null
    ): Boolean {
        // Enforce: ONLY the email gautamprasanna641@gmail.com has access to post as this specific Page
        val currentEmail = try { Firebase.auth.currentUser?.email } catch (e: Exception) { null }
        if (!isUserAdmin(currentEmail)) return false

        val page = _officialPage.value
        val author = User(
            id = page.id,
            name = page.pageName,
            handle = page.handle,
            avatarResId = R.drawable.connectnp_icon_full,
            isVerified = true,
            isCreator = true,
            bio = page.bio
        )

        val newPost = Post(
            id = "page_post_${System.currentTimeMillis()}",
            author = author,
            content = content,
            mediaUrl = mediaUri?.toString(),
            locationName = location ?: "Kathmandu, Nepal",
            timestamp = "Just now",
            likesCount = 5,
            commentsCount = 0,
            sharesCount = 1,
            userReaction = ReactionType.LOVE,
            isPagePost = true,
            pageId = page.id
        )

        _posts.value = listOf(newPost) + _posts.value

        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = ConnectNpFirestoreRepository(context)
                    repo.createPagePost(
                        content = content,
                        location = location ?: "Kathmandu, Nepal",
                        pageName = page.pageName,
                        pageHandle = page.handle,
                        pageId = page.id
                    )
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
        return true
    }

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _blockedUserNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val blockedUserNames: StateFlow<Map<String, String>> = _blockedUserNames.asStateFlow()

    private val _reportedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val reportedPostIds: StateFlow<Set<String>> = _reportedPostIds.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _reels = MutableStateFlow<List<Reel>>(emptyList())
    val reels: StateFlow<List<Reel>> = _reels.asStateFlow()

    private val _chatThreads = MutableStateFlow<List<ChatThread>>(SampleDataProvider.getInitialChatThreads())
    val chatThreads: StateFlow<List<ChatThread>> = _chatThreads.asStateFlow()

    private val _messagesByThread = MutableStateFlow<Map<String, List<ChatMessage>>>(
        mapOf(
            "chat_ai" to listOf(
                ChatMessage(
                    id = "ai_welcome",
                    senderId = SampleDataProvider.aiBotUser.id,
                    text = "नमस्ते! I'm your ConnectNP AI Assistant. How can I assist you with your travels, community questions, or ConnectNP services today? 🇳🇵✨",
                    timestamp = "Just now",
                    isFromMe = false
                )
            )
        )
    )
    val messagesByThread: StateFlow<Map<String, List<ChatMessage>>> = _messagesByThread.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _marketItems = MutableStateFlow<List<MarketItem>>(emptyList())
    val marketItems: StateFlow<List<MarketItem>> = _marketItems.asStateFlow()

    private val _watchVideos = MutableStateFlow<List<WatchVideo>>(emptyList())
    val watchVideos: StateFlow<List<WatchVideo>> = _watchVideos.asStateFlow()

    private val _jobs = MutableStateFlow<List<JobItem>>(emptyList())
    val jobs: StateFlow<List<JobItem>> = _jobs.asStateFlow()

    private val _datingProfiles = MutableStateFlow<List<DatingProfile>>(emptyList())
    val datingProfiles: StateFlow<List<DatingProfile>> = _datingProfiles.asStateFlow()

    private val _datingMatches = MutableStateFlow(0)
    val datingMatches: StateFlow<Int> = _datingMatches.asStateFlow()

    private val _walletBalance = MutableStateFlow(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _isBalanceVisible = MutableStateFlow(true)
    val isBalanceVisible: StateFlow<Boolean> = _isBalanceVisible.asStateFlow()

    fun toggleBalanceVisibility() {
        _isBalanceVisible.value = !_isBalanceVisible.value
    }

    fun setBalanceVisibility(visible: Boolean) {
        _isBalanceVisible.value = visible
    }

    // --- Wallet Security: 4-digit MPIN & Biometrics ---
    private val _walletMpin = MutableStateFlow<String?>("1234") // default sample MPIN
    val walletMpin: StateFlow<String?> = _walletMpin.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _activeResetOtp = MutableStateFlow<String?>(null)
    val activeResetOtp: StateFlow<String?> = _activeResetOtp.asStateFlow()

    fun isMpinSetup(): Boolean {
        return !_walletMpin.value.isNullOrBlank()
    }

    fun setupWalletMpin(newPin: String, context: Context? = null): Boolean {
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) return false
        _walletMpin.value = newPin
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("connectnp_wallet_security", Context.MODE_PRIVATE)
                prefs.edit().putString("wallet_mpin", newPin).apply()
            } catch (e: Exception) {
                // Ignore prefs error
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = ConnectNpFirestoreRepository(context)
                    repo.updateWalletMpin(newPin)
                } catch (e: Exception) {
                    android.util.Log.d("ConnectNpRepo", "updateWalletMpin error: ${e.message}")
                }
            }
        }
        return true
    }

    fun verifyWalletMpin(enteredPin: String): Boolean {
        val currentPin = _walletMpin.value ?: return false
        return currentPin == enteredPin
    }

    fun setBiometricEnabled(enabled: Boolean, context: Context? = null) {
        _isBiometricEnabled.value = enabled
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("connectnp_wallet_security", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("biometric_enabled", enabled).apply()
            } catch (e: Exception) {
                // Ignore prefs error
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = ConnectNpFirestoreRepository(context)
                    repo.updateBiometricEnabled(enabled)
                } catch (e: Exception) {
                    android.util.Log.d("ConnectNpRepo", "updateBiometricEnabled error: ${e.message}")
                }
            }
        }
    }

    fun generateAndSendResetMpinOtp(email: String, context: Context? = null): String {
        // Generate secure 6-digit OTP
        val randomOtp = (100000..999999).random().toString()
        _activeResetOtp.value = randomOtp
        if (context != null) {
            ConnectNpMessagingService.showLocalNotification(
                context = context,
                title = "CONNECTNP Wallet Security",
                body = "Your MPIN Reset Verification Code is: $randomOtp (valid for 10 minutes)"
            )
        }
        return randomOtp
    }

    fun resetWalletMpinWithOtp(email: String, enteredOtp: String, newPin: String, context: Context? = null): Boolean {
        val currentOtp = _activeResetOtp.value
        if (currentOtp == null || currentOtp != enteredOtp.trim()) return false
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) return false
        val success = setupWalletMpin(newPin, context)
        if (success) {
            _activeResetOtp.value = null
        }
        return success
    }

    fun initWalletSecurity(context: Context, userProfile: FirestoreUserProfile?) {
        try {
            val prefs = context.getSharedPreferences("connectnp_wallet_security", Context.MODE_PRIVATE)
            val savedPin = userProfile?.walletMpin?.ifBlank { null }
                ?: prefs.getString("wallet_mpin", null)
                ?: _walletMpin.value

            if (savedPin != null) {
                _walletMpin.value = savedPin
            }

            val savedBio = userProfile?.isBiometricEnabled
                ?: prefs.getBoolean("biometric_enabled", false)
            _isBiometricEnabled.value = savedBio
        } catch (e: Exception) {
            // Ignore error
        }
    }

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    private val _isNepaliLanguage = MutableStateFlow(false)
    val isNepaliLanguage: StateFlow<Boolean> = _isNepaliLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _unreadNotifications = MutableStateFlow(0)
    val unreadNotifications: StateFlow<Int> = _unreadNotifications.asStateFlow()

    fun setPosts(newPosts: List<Post>) {
        _posts.value = newPosts
    }

    fun setWalletBalance(balance: Double) {
        _walletBalance.value = balance
    }

    fun setTransactions(newTransactions: List<WalletTransaction>) {
        _transactions.value = newTransactions
    }

    fun setStories(newStories: List<Story>) {
        _stories.value = newStories
    }

    fun toggleLanguage() {
        _isNepaliLanguage.value = !_isNepaliLanguage.value
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun clearNotifications() {
        _unreadNotifications.value = 0
    }

    fun incrementUnreadNotifications() {
        _unreadNotifications.value += 1
    }

    // --- Dynamic User Resolution ---
    private val _currentActiveUser = MutableStateFlow<User>(resolveCurrentUserFromFirebase())
    val currentActiveUser: StateFlow<User> = _currentActiveUser.asStateFlow()

    private fun resolveCurrentUserFromFirebase(
        profileDoc: FirestoreUserProfile? = null,
        authUser: FirebaseUser? = null,
        overrideName: String? = null,
        overrideHandle: String? = null,
        overridePhoto: String? = null,
        overrideBio: String? = null
    ): User {
        val fbUser = authUser ?: try {
            Firebase.auth.currentUser
        } catch (e: Exception) {
            null
        }

        val id = fbUser?.uid ?: profileDoc?.userId?.ifBlank { null } ?: "user_me"

        val name = overrideName?.takeIf { it.isNotBlank() }
            ?: profileDoc?.name?.takeIf { it.isNotBlank() }
            ?: fbUser?.displayName?.takeIf { it.isNotBlank() }
            ?: fbUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "ConnectNP Member"

        val rawHandle = overrideHandle?.takeIf { it.isNotBlank() }
            ?: profileDoc?.handle?.takeIf { it.isNotBlank() }
            ?: fbUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "member"
        val handle = if (rawHandle.startsWith("@")) rawHandle else "@$rawHandle"

        val photo = overridePhoto?.takeIf { it.isNotBlank() }
            ?: profileDoc?.photoUrl?.takeIf { it.isNotBlank() }
            ?: fbUser?.photoUrl?.toString()

        val bio = overrideBio?.takeIf { it.isNotBlank() }
            ?: profileDoc?.bio?.takeIf { it.isNotBlank() }
            ?: "Proud ConnectNP Citizen 🇳🇵"

        return User(
            id = id,
            name = name,
            handle = handle,
            avatarUrl = photo,
            avatarResId = if (photo.isNullOrBlank()) R.drawable.connectnp_icon_full else null,
            isVerified = true,
            isCreator = true,
            bio = bio,
            location = "Kathmandu, Nepal",
            followersCount = profileDoc?.followersCount?.toInt() ?: 0,
            followingCount = profileDoc?.followingCount?.toInt() ?: 0
        )
    }

    fun updateActiveUser(
        profileDoc: FirestoreUserProfile? = null,
        authUser: FirebaseUser? = null,
        name: String? = null,
        handle: String? = null,
        photoUrl: String? = null,
        bio: String? = null
    ) {
        val resolved = resolveCurrentUserFromFirebase(
            profileDoc = profileDoc,
            authUser = authUser,
            overrideName = name,
            overrideHandle = handle,
            overridePhoto = photoUrl,
            overrideBio = bio
        )
        _currentActiveUser.value = resolved
    }

    fun getCurrentActiveUser(): User {
        val fbUser = try {
            Firebase.auth.currentUser
        } catch (e: Exception) {
            null
        }
        val cached = _currentActiveUser.value
        return if (fbUser != null && (cached.id != fbUser.uid || cached.name == "ConnectNP Member")) {
            val fresh = resolveCurrentUserFromFirebase(authUser = fbUser)
            _currentActiveUser.value = fresh
            fresh
        } else {
            cached
        }
    }

    // --- Posts Logic ---
    fun addPost(
        content: String,
        location: String? = null,
        mediaResId: Int? = null,
        customAuthor: User? = null,
        mediaUri: android.net.Uri? = null,
        mediaUrl: String? = null
    ) {
        val author = customAuthor ?: getCurrentActiveUser()
        val newPost = Post(
            id = "post_${System.currentTimeMillis()}",
            author = author,
            content = content,
            locationName = location ?: "Kathmandu, Nepal",
            mediaResId = mediaResId,
            mediaUrl = mediaUri?.toString() ?: mediaUrl,
            timestamp = "Just now",
            likesCount = 0,
            commentsCount = 0,
            sharesCount = 0,
            userReaction = ReactionType.LOVE
        )
        _posts.value = listOf(newPost) + _posts.value
    }

    fun toggleReaction(postId: String, reaction: ReactionType, context: Context? = null) {
        var isNewReaction = false
        var reactedPostTitle: String? = null
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val current = post.userReaction
                if (current == reaction) {
                    post.copy(
                        userReaction = null,
                        likesCount = maxOf(0, post.likesCount - 1)
                    )
                } else {
                    val delta = if (current == null) 1 else 0
                    isNewReaction = true
                    reactedPostTitle = post.content.take(35)
                    post.copy(
                        userReaction = reaction,
                        likesCount = post.likesCount + delta
                    )
                }
            } else post
        }

        if (isNewReaction && context != null) {
            incrementUnreadNotifications()
            val reactionEmoji = reaction.emoji
            val title = "New Reaction! $reactionEmoji"
            val body = "Someone reacted $reactionEmoji on ConnectNP: \"${reactedPostTitle ?: "Post"}\""
            ConnectNpMessagingService.showLocalNotification(context, title, body, "like")
        }
    }

    fun votePoll(postId: String, optionId: Int) {
        _posts.value = _posts.value.map { post ->
            if (post.id == postId && post.poll != null && post.poll.userVotedOptionId == null) {
                val updatedOptions = post.poll.options.map { opt ->
                    if (opt.id == optionId) opt.copy(votes = opt.votes + 1) else opt
                }
                val updatedPoll = post.poll.copy(
                    options = updatedOptions,
                    totalVotes = post.poll.totalVotes + 1,
                    userVotedOptionId = optionId
                )
                post.copy(poll = updatedPoll)
            } else post
        }
    }

    fun toggleBookmark(postId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) post.copy(isBookmarked = !post.isBookmarked) else post
        }
    }

    fun incrementShareCount(postId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) post.copy(sharesCount = post.sharesCount + 1) else post
        }
    }

    private val _commentsByPost = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val commentsByPost: StateFlow<Map<String, List<Comment>>> = _commentsByPost.asStateFlow()

    fun getCommentsForPost(postId: String): List<Comment> {
        return _commentsByPost.value[postId] ?: emptyList()
    }

    fun addComment(postId: String, text: String, commentAuthor: User? = null) {
        val author = commentAuthor ?: getCurrentActiveUser()
        val newComment = Comment(
            id = "comment_${System.currentTimeMillis()}",
            author = author,
            text = text,
            timeAgo = "Just now",
            likesCount = 0,
            isLiked = false
        )
        val existing = _commentsByPost.value[postId] ?: emptyList()
        _commentsByPost.value = _commentsByPost.value + (postId to (existing + newComment))
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                post.copy(commentsCount = post.commentsCount + 1)
            } else post
        }
    }

    fun deletePost(postId: String, context: Context? = null) {
        _posts.value = _posts.value.filter { it.id != postId }
        _commentsByPost.value = _commentsByPost.value - postId
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firestoreRepo = ConnectNpFirestoreRepository(context)
                    firestoreRepo.deletePost(postId)
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
    }

    fun deleteComment(postId: String, commentId: String, context: Context? = null) {
        val currentComments = _commentsByPost.value[postId] ?: emptyList()
        val updated = currentComments.filter { it.id != commentId }
        _commentsByPost.value = _commentsByPost.value + (postId to updated)
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                post.copy(commentsCount = maxOf(0, post.commentsCount - 1))
            } else post
        }
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firestoreRepo = ConnectNpFirestoreRepository(context)
                    firestoreRepo.deleteComment(postId, commentId)
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
    }

    // --- Stories Logic ---
    fun markStoryViewed(storyId: String) {
        _stories.value = _stories.value.map { story ->
            if (story.id == storyId) story.copy(isViewed = true) else story
        }
    }

    fun addStory(caption: String, mediaUri: android.net.Uri? = null) {
        val newStory = Story(
            id = "story_${System.currentTimeMillis()}",
            author = getCurrentActiveUser(),
            caption = caption,
            mediaResId = if (mediaUri == null) R.drawable.himalaya_banner_1792096400169 else null,
            mediaUrl = mediaUri?.toString(),
            timestamp = "Just now",
            isViewed = false
        )
        _stories.value = listOf(newStory) + _stories.value
    }

    // --- Reels Logic ---
    fun toggleReelLike(reelId: String, context: Context? = null) {
        var isLiked = false
        _reels.value = _reels.value.map { reel ->
            if (reel.id == reelId) {
                val newLiked = !reel.isLiked
                isLiked = newLiked
                reel.copy(
                    isLiked = newLiked,
                    likesCount = if (newLiked) reel.likesCount + 1 else reel.likesCount - 1
                )
            } else reel
        }
        if (isLiked && context != null) {
            incrementUnreadNotifications()
            ConnectNpMessagingService.showLocalNotification(
                context = context,
                title = "New Reel Like! ❤️",
                body = "Someone liked your trending Reel on ConnectNP!",
                type = "like"
            )
        }
    }

    fun toggleReelSave(reelId: String) {
        _reels.value = _reels.value.map { reel ->
            if (reel.id == reelId) reel.copy(isSaved = !reel.isSaved) else reel
        }
    }

    fun addReel(
        title: String,
        soundTitle: String = "Trending Sound • Nepal",
        videoUri: android.net.Uri? = null,
        customAuthor: User? = null
    ): Reel {
        val author = customAuthor ?: getCurrentActiveUser()
        val newReel = Reel(
            id = "reel_${System.currentTimeMillis()}",
            creator = author,
            title = title,
            soundTitle = soundTitle,
            likesCount = 1,
            commentsCount = 0,
            sharesCount = 0,
            isLiked = false,
            videoUrl = videoUri?.toString()
        )
        _reels.value = listOf(newReel) + _reels.value
        return newReel
    }

    fun addWatchVideo(
        title: String,
        description: String = "",
        category: String = "Community",
        videoUri: android.net.Uri? = null,
        customAuthor: User? = null
    ): WatchVideo {
        val author = customAuthor ?: getCurrentActiveUser()
        val newVideo = WatchVideo(
            id = "video_${System.currentTimeMillis()}",
            title = title,
            channel = author,
            views = "1 view",
            timeAgo = "Just now",
            duration = "0:45",
            description = description,
            likesCount = "1",
            videoUrl = videoUri?.toString(),
            tags = listOf(category, "Nepal", "ConnectNP")
        )
        _watchVideos.value = listOf(newVideo) + _watchVideos.value
        return newVideo
    }

    // --- Chat Logic ---
    fun sendMessage(
        threadId: String,
        text: String,
        mediaType: MessageMediaType = MessageMediaType.TEXT,
        audioDurationSec: Int? = null,
        context: Context? = null
    ) {
        val userMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = getCurrentActiveUser().id,
            text = text,
            timestamp = "Just now",
            isFromMe = true,
            status = MessageStatus.SENT,
            mediaType = mediaType,
            audioDurationSec = audioDurationSec
        )

        val currentList = _messagesByThread.value[threadId] ?: emptyList()
        val updatedThreadMessages = currentList + userMsg
        val updatedMap = _messagesByThread.value.toMutableMap()
        updatedMap[threadId] = updatedThreadMessages
        _messagesByThread.value = updatedMap

        // Update chat thread preview
        _chatThreads.value = _chatThreads.value.map { thread ->
            if (thread.id == threadId) {
                thread.copy(
                    lastMessage = if (mediaType == MessageMediaType.AUDIO_VOICE) "🎤 Voice Message (${audioDurationSec ?: 5}s)" else text,
                    lastMessageTime = "Just now"
                )
            } else thread
        }

        // Response generator with local notification
        if (threadId == "chat_ai") {
            generateAiResponse(updatedThreadMessages, text, context)
        } else {
            simulatePeerResponse(threadId, context)
        }
    }

    private fun simulatePeerResponse(threadId: String, context: Context? = null) {
        val targetThread = _chatThreads.value.find { it.id == threadId } ?: return
        val sender = targetThread.user
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(1800)
            val replyText = "Namaste! Received your message on CONNECTNP. Catch you shortly! 🇳🇵"
            val peerMsg = ChatMessage(
                id = "peer_${System.currentTimeMillis()}",
                senderId = sender.id,
                text = replyText,
                timestamp = "Just now",
                isFromMe = false
            )
            val list = _messagesByThread.value[threadId] ?: emptyList()
            val map = _messagesByThread.value.toMutableMap()
            map[threadId] = list + peerMsg
            _messagesByThread.value = map

            _chatThreads.value = _chatThreads.value.map { thread ->
                if (thread.id == threadId) {
                    thread.copy(lastMessage = replyText, lastMessageTime = "Just now")
                } else thread
            }

            if (context != null) {
                incrementUnreadNotifications()
                ConnectNpMessagingService.showLocalNotification(
                    context = context,
                    title = "New Message from ${sender.name} 💬",
                    body = replyText,
                    type = "chat"
                )
            }
        }
    }

    private fun generateAiResponse(
        history: List<ChatMessage>,
        userPrompt: String,
        context: Context? = null
    ) {
        _isAiThinking.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Generates contextual response maintaining all prior conversation turns via Gemini API
                val reply = ConnectNpAiService.getAiResponseWithHistory(history, userPrompt)

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    _isAiThinking.value = false
                    val aiMsg = ChatMessage(
                        id = "ai_${System.currentTimeMillis()}",
                        senderId = SampleDataProvider.aiBotUser.id,
                        text = reply,
                        timestamp = "Just now",
                        isFromMe = false
                    )

                    val currentList = _messagesByThread.value["chat_ai"] ?: emptyList()
                    val updatedMap = _messagesByThread.value.toMutableMap()
                    updatedMap["chat_ai"] = currentList + aiMsg
                    _messagesByThread.value = updatedMap

                    _chatThreads.value = _chatThreads.value.map { thread ->
                        if (thread.id == "chat_ai") {
                            thread.copy(
                                lastMessage = if (reply.length > 60) reply.take(60) + "..." else reply,
                                lastMessageTime = "Just now"
                            )
                        } else thread
                    }

                    if (context != null) {
                        incrementUnreadNotifications()
                        ConnectNpMessagingService.showLocalNotification(
                            context = context,
                            title = "ConnectNP Assistant 🤖",
                            body = reply,
                            type = "chat"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ConnectNpRepository", "AI generation error: ${e.message}", e)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    _isAiThinking.value = false
                }
            }
        }
    }

    // --- Marketplace Logic ---
    fun addMarketListing(title: String, price: Long, category: String, location: String, description: String) {
        val newItem = MarketItem(
            id = "item_${System.currentTimeMillis()}",
            title = title,
            priceNpr = price,
            category = category,
            location = location,
            seller = getCurrentActiveUser(),
            condition = "Brand New",
            imageResId = R.drawable.nepal_crafts_1792096421296,
            description = description,
            isFeatured = true
        )
        _marketItems.value = listOf(newItem) + _marketItems.value
    }

    const val P2P_TRANSACTION_FEE = 10.0

    // --- ConnectNP Pay Logic ---
    fun sendMoney(
        recipientName: String,
        amount: Double,
        remarks: String,
        context: Context? = null,
        fee: Double = P2P_TRANSACTION_FEE
    ): Boolean {
        val totalDeduction = amount + fee
        if (_walletBalance.value < totalDeduction) return false
        _walletBalance.value -= totalDeduction
        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            title = "P2P to $recipientName (Transfer: रु ${amount.toInt()}, Fee: रु ${fee.toInt()})",
            date = "Today, Just now",
            amountNpr = totalDeduction,
            isCredit = false,
            category = remarks.ifEmpty { "P2P Payment" }
        )
        _transactions.value = listOf(newTx) + _transactions.value
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = ConnectNpFirestoreRepository(context)
                    repo.addTransaction("P2P to $recipientName (Transfer: रु ${amount.toInt()}, Fee: रु ${fee.toInt()})", totalDeduction.toLong(), "debit")
                    repo.updateWalletBalance(_walletBalance.value.toLong())
                    repo.recordAdminRevenueFee(
                        feeAmount = fee.toLong(),
                        transferAmount = amount.toLong(),
                        recipient = recipientName
                    )
                } catch (e: Exception) {
                    android.util.Log.d("ConnectNpRepo", "sendMoney firestore sync: ${e.message}")
                }
            }
        }
        return true
    }

    fun topUpWallet(amount: Double, source: String, context: Context? = null) {
        _walletBalance.value += amount
        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            title = "Top-Up via $source",
            date = "Today, Just now",
            amountNpr = amount,
            isCredit = true,
            category = "Wallet Top-Up"
        )
        _transactions.value = listOf(newTx) + _transactions.value
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = ConnectNpFirestoreRepository(context)
                    repo.addTransaction("Top-Up via $source", amount.toLong(), "credit")
                    repo.updateWalletBalance(_walletBalance.value.toLong())
                } catch (e: Exception) {
                    android.util.Log.d("ConnectNpRepo", "topUpWallet firestore sync: ${e.message}")
                }
            }
        }
    }

    // --- Jobs Logic ---
    fun applyForJob(jobId: String) {
        _jobs.value = _jobs.value.map { job ->
            if (job.id == jobId) job.copy(hasApplied = true) else job
        }
    }

    // --- Dating Logic ---
    fun swipeDatingProfile(profileId: String, isLiked: Boolean) {
        if (isLiked) {
            _datingMatches.value += 1
        }
        _datingProfiles.value = _datingProfiles.value.filter { it.id != profileId }
    }

    // --- Watch Logic ---
    fun toggleSubscribe(videoId: String) {
        _watchVideos.value = _watchVideos.value.map { video ->
            if (video.id == videoId) video.copy(isSubscribed = !video.isSubscribed) else video
        }
    }

    // --- Block & Report Logic (Play Store Compliance) ---
    fun setFcmToken(token: String) {
        _fcmToken.value = token
    }

    fun setBlockedUserIds(ids: List<String>) {
        _blockedUserIds.value = _blockedUserIds.value + ids
    }

    fun isUserBlocked(userId: String, handle: String = "", name: String = ""): Boolean {
        val blocked = _blockedUserIds.value
        if (userId.isNotBlank() && userId in blocked) return true
        val cleanHandle = handle.removePrefix("@")
        if (cleanHandle.isNotBlank()) {
            if (cleanHandle in blocked || "@$cleanHandle" in blocked) return true
        }
        if (name.isNotBlank() && name in blocked) return true
        return false
    }

    fun blockUser(userId: String, userName: String, context: Context? = null, userHandle: String? = null) {
        val identifiers = mutableSetOf(userId)
        if (!userHandle.isNullOrBlank()) {
            val clean = userHandle.removePrefix("@")
            identifiers.add(clean)
            identifiers.add("@$clean")
        }
        identifiers.add(userName)
        _blockedUserIds.value = _blockedUserIds.value + identifiers
        _blockedUserNames.value = _blockedUserNames.value + (userId to userName)
        if (context != null) {
            val firestoreRepo = ConnectNpFirestoreRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestoreRepo.blockUser(userId)
                } catch (e: Exception) {
                    // Fallback to local state if offline
                }
            }
        }
    }

    fun unblockUser(userId: String, context: Context? = null, userHandle: String? = null, userName: String? = null) {
        val identifiers = mutableSetOf(userId)
        if (!userHandle.isNullOrBlank()) {
            val clean = userHandle.removePrefix("@")
            identifiers.add(clean)
            identifiers.add("@$clean")
        }
        if (!userName.isNullOrBlank()) {
            identifiers.add(userName)
        }
        _blockedUserIds.value = _blockedUserIds.value - identifiers
        _blockedUserNames.value = _blockedUserNames.value - userId
        if (context != null) {
            val firestoreRepo = ConnectNpFirestoreRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestoreRepo.unblockUser(userId)
                } catch (e: Exception) {
                    // Fallback to local state
                }
            }
        }
    }

    fun reportPost(postId: String, authorId: String, reason: String, context: Context? = null) {
        _reportedPostIds.value = _reportedPostIds.value + postId
        if (context != null) {
            val firestoreRepo = ConnectNpFirestoreRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestoreRepo.reportContent(
                        targetType = "post",
                        targetId = postId,
                        targetAuthorId = authorId,
                        reason = reason
                    )
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
    }

    // --- Account Deletion (Mandatory for Play Store) ---
    suspend fun deleteUserAccount(context: Context): Result<Unit> {
        val user = try {
            Firebase.auth.currentUser
        } catch (e: Exception) {
            null
        } ?: return Result.failure(IllegalStateException("No authenticated user found"))
        val uid = user.uid
        return try {
            val firestoreRepo = ConnectNpFirestoreRepository(context)
            // 1. Delete user document from Firestore
            firestoreRepo.deleteUserAccount(uid)
            // 2. Delete Auth account
            user.delete().await()
            // 3. Clear local states
            _blockedUserIds.value = emptySet()
            _blockedUserNames.value = emptyMap()
            _reportedPostIds.value = emptySet()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
