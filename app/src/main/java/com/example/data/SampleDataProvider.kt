package com.example.data

import com.example.R
import com.example.model.*

object SampleDataProvider {

    val currentUser = User(
        id = "user_me",
        name = "ConnectNP Member",
        handle = "@member",
        avatarResId = R.drawable.connectnp_icon_full,
        isVerified = false,
        isCreator = false,
        bio = "Proud ConnectNP Citizen 🇳🇵",
        location = "Kathmandu, Nepal",
        followersCount = 0,
        followingCount = 0
    )

    val aiBotUser = User(
        id = "user_ai",
        name = "ConnectNP AI Assistant",
        handle = "@connectnp_ai",
        avatarResId = R.drawable.connectnp_icon_full,
        isVerified = true,
        isCreator = true,
        location = "ConnectNP Cloud",
        bio = "Your bilingual Nepali & English AI Companion"
    )

    fun getInitialStories(): List<Story> = emptyList()

    fun getInitialPosts(): List<Post> = emptyList()

    fun getInitialReels(): List<Reel> = emptyList()

    fun getInitialChatThreads(): List<ChatThread> = listOf(
        ChatThread(
            id = "chat_ai",
            user = aiBotUser,
            lastMessage = "नमस्ते! How can I help you today with ConnectNP features or Nepali travel?",
            lastMessageTime = "Just now",
            unreadCount = 0,
            isOnline = true,
            isAiAssistant = true
        )
    )

    fun getInitialMarketItems(): List<MarketItem> = emptyList()

    fun getInitialWatchVideos(): List<WatchVideo> = emptyList()

    fun getInitialJobs(): List<JobItem> = emptyList()

    fun getInitialDatingProfiles(): List<DatingProfile> = emptyList()

    fun getInitialWalletTransactions(): List<WalletTransaction> = emptyList()
}
