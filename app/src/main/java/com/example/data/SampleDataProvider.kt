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

    fun getInitialMarketItems(): List<MarketItem> {
        val sellerCrafts = User(
            id = "user_crafts",
            name = "Ramesh Shakya",
            handle = "@ramesh_crafts",
            avatarResId = R.drawable.connectnp_icon_full,
            isVerified = true,
            location = "Patan, Lalitpur",
            bio = "Traditional Newari artisan & craft preservationist"
        )
        val sellerFashion = User(
            id = "user_fashion",
            name = "Sunita Shrestha",
            handle = "@sunita_himalayan",
            avatarResId = R.drawable.connectnp_icon_full,
            isVerified = true,
            location = "Thamel, Kathmandu",
            bio = "Certified Himalayan Cashmere & Pashmina weaver"
        )
        val sellerElectronics = User(
            id = "user_tech",
            name = "Dipak Giri",
            handle = "@dipak_giri",
            avatarResId = R.drawable.connectnp_icon_full,
            isVerified = false,
            location = "New Road, Kathmandu",
            bio = "Gadget & photography hobbyist"
        )
        val sellerMotors = User(
            id = "user_motors",
            name = "Bikash Thapa",
            handle = "@bikash_motors",
            avatarResId = R.drawable.connectnp_icon_full,
            isVerified = true,
            location = "Lakeside, Pokhara",
            bio = "Bike touring guide & motorcycle enthusiast"
        )

        return listOf(
            MarketItem(
                id = "item_1",
                title = "Handmade Golden Thangka Painting (Wheel of Life)",
                priceNpr = 14500L,
                location = "Patan Durbar, Lalitpur",
                category = "Handicrafts",
                seller = sellerCrafts,
                condition = "Brand New",
                imageResId = R.drawable.nepal_crafts_1792096421296,
                description = "Masterpiece hand-painted Tibetan Buddhist Thangka on fine cotton canvas with 24K gold dust detailing. Crafted by hereditary Newari masters in Patan. Blessed and framed.",
                isFeatured = true
            ),
            MarketItem(
                id = "item_2",
                title = "100% Pure Himalayan Chyangra Cashmere Pashmina",
                priceNpr = 4800L,
                location = "Thamel, Kathmandu",
                category = "Fashion",
                seller = sellerFashion,
                condition = "Brand New",
                imageResId = R.drawable.himalaya_banner_1792096400169,
                description = "Ultra-soft grade-A Chyangra goat wool Pashmina shawl woven in Kathmandu valley. Certified authentic handmade tag with elegant maroon and gold border.",
                isFeatured = true
            ),
            MarketItem(
                id = "item_3",
                title = "Sony Alpha A7 III Full Frame Mirrorless Camera",
                priceNpr = 138000L,
                location = "New Road, Kathmandu",
                category = "Electronics",
                seller = sellerElectronics,
                condition = "Like New",
                imageResId = R.drawable.connectnp_icon_full,
                description = "Lightly used Sony A7 III with 28-70mm kit lens, 2 original Sony batteries, 64GB Sony Tough SD card, and camera bag. Shutter count under 9,500.",
                isFeatured = false
            ),
            MarketItem(
                id = "item_4",
                title = "Royal Enfield Classic 350 Stealth Black",
                priceNpr = 345000L,
                location = "Lakeside, Pokhara",
                category = "Vehicles",
                seller = sellerMotors,
                condition = "Gently Used",
                imageResId = R.drawable.connectnp_icon_full,
                description = "2022 BS 6 model in pristine condition. Single hand-driven for 14,000 km. Complete servicing history, tax cleared till 2082, genuine luggage racks included.",
                isFeatured = true
            ),
            MarketItem(
                id = "item_5",
                title = "Authentic 7-Metal Hand Hammered Tibetan Singing Bowl",
                priceNpr = 3200L,
                location = "Kathmandu, Nepal",
                category = "Handicrafts",
                seller = currentUser,
                condition = "Brand New",
                imageResId = R.drawable.nepal_crafts_1792096421296,
                description = "Traditional 7-metal alloy singing bowl tuned to the Heart Chakra (F Note). Comes with rosewood striker mallet and silk ring cushion.",
                isFeatured = true,
                isSold = false
            ),
            MarketItem(
                id = "item_6",
                title = "MacBook Air M2 (16GB RAM / 512GB SSD) Midnight",
                priceNpr = 142000L,
                location = "Baneshwor, Kathmandu",
                category = "Electronics",
                seller = currentUser,
                condition = "Like New",
                imageResId = R.drawable.connectnp_icon_full,
                description = "Purchased 8 months ago, 98% battery health, box and MagSafe cable included. Selling to upgrade to M3 Pro. Under official Apple warranty.",
                isFeatured = false,
                isSold = false
            )
        )
    }


    fun getInitialWatchVideos(): List<WatchVideo> = emptyList()

    fun getInitialJobs(): List<JobItem> = emptyList()

    fun getInitialDatingProfiles(): List<DatingProfile> = emptyList()

    fun getInitialWalletTransactions(): List<WalletTransaction> = emptyList()
}
