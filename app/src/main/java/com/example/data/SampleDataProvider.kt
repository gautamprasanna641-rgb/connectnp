package com.example.data

import com.example.R
import com.example.model.*

object SampleDataProvider {

    val currentUser = User(
        id = "user_me",
        name = "Prasanna Gautam",
        handle = "@prasanna_np",
        avatarResId = R.drawable.connectnp_icon_full,
        isVerified = true,
        isCreator = true,
        bio = "Software Engineer & Builder 🇳🇵 | Exploring the Himalayas & building ConnectNP",
        location = "Kathmandu, Nepal",
        followersCount = 4280,
        followingCount = 385
    )

    val aayushUser = User(
        id = "user_aayush",
        name = "Aayush Sharma",
        handle = "@aayush_dev",
        isVerified = true,
        location = "Lalitpur, Nepal",
        followersCount = 1840,
        followingCount = 210
    )

    val poojaUser = User(
        id = "user_pooja",
        name = "Pooja Shrestha",
        handle = "@pooja_travels",
        isVerified = true,
        isCreator = true,
        location = "Pokhara, Nepal",
        followersCount = 12900,
        followingCount = 420
    )

    val officialUser = User(
        id = "user_connectnp",
        name = "ConnectNP Official",
        handle = "@connectnp",
        avatarResId = R.drawable.connectnp_icon_full,
        isVerified = true,
        isCreator = true,
        location = "Global / Kathmandu",
        followersCount = 152000,
        followingCount = 12
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

    fun getInitialStories(): List<Story> = listOf(
        Story(
            id = "story_me",
            author = currentUser,
            caption = "Add to your 24h story",
            timestamp = "You",
            isViewed = false
        ),
        Story(
            id = "story_1",
            author = poojaUser,
            mediaResId = R.drawable.himalaya_banner_1792096400169,
            caption = "Breathtaking sunrise above the clouds in Annapurna circuit! 🏔️✨",
            timestamp = "1h ago",
            isViewed = false,
            isCloseFriends = true
        ),
        Story(
            id = "story_2",
            author = officialUser,
            mediaResId = R.drawable.connectnp_icon_full,
            caption = "Welcome to ConnectNP Super App v2.0! Namaste world! 🇳🇵🙏",
            timestamp = "3h ago",
            isViewed = false
        ),
        Story(
            id = "story_3",
            author = aayushUser,
            mediaResId = R.drawable.nepal_crafts_1792096421296,
            caption = "Visiting Kathmandu Durbar Square artisans today. The craftsmanship is unreal! 🎨",
            timestamp = "5h ago",
            isViewed = true
        )
    )

    fun getInitialPosts(): List<Post> = listOf(
        Post(
            id = "post_1",
            author = poojaUser,
            content = "Golden rays touching the highest peaks on Earth. Waking up to the majestic Himalayas will never get old. Nepal's serenity is a feeling words cannot describe. Who wants to join our next community trek? 🏔️🇳🇵 #ConnectNP #VisitNepal #Everest #Himalayas",
            mediaResId = R.drawable.himalaya_banner_1792096400169,
            timestamp = "2 hours ago",
            likesCount = 384,
            commentsCount = 42,
            sharesCount = 18,
            userReaction = ReactionType.NAMASTE,
            locationName = "Sagarmatha National Park, Solukhumbu",
            tags = listOf("Himalayas", "Trekking", "VisitNepal2026"),
            poll = PollData(
                question = "Which trek route should ConnectNP Trek Club host next month?",
                options = listOf(
                    PollOption(1, "Annapurna Circuit & Tilicho Lake", 142),
                    PollOption(2, "Langtang Valley & Gosaikunda", 98),
                    PollOption(3, "Manaslu Circuit Offbeat", 64)
                ),
                totalVotes = 304,
                userVotedOptionId = 1
            )
        ),
        Post(
            id = "post_2",
            author = officialUser,
            content = "🎉 Happy milestone, family! ConnectNP has officially crossed 500,000 active members uniting creators, entrepreneurs, and friends across Nepal and the global Nepali diaspora!\n\nFrom seamless P2P payments with ConnectNP Pay to TikTok-style vertical reels and hyper-local marketplace, we are shaping the future of social technology. Thank you for belonging! 🇳🇵✨",
            mediaResId = R.drawable.connectnp_icon_full,
            timestamp = "4 hours ago",
            likesCount = 1250,
            commentsCount = 189,
            sharesCount = 95,
            userReaction = ReactionType.LOVE,
            locationName = "Kathmandu Headquarters",
            tags = listOf("ConnectNP", "SuperApp", "NepalTech", "OneApp")
        ),
        Post(
            id = "post_3",
            author = aayushUser,
            content = "Exploring the artisan alleys of Bhaktapur & Patan today. Every singing bowl, handmade pashmina, and intricate woodwork tells generations of Nepali heritage. Listing some verified authentic pieces directly on ConnectNP Marketplace with escrow protection! 🛍️✨",
            mediaResId = R.drawable.nepal_crafts_1792096421296,
            timestamp = "6 hours ago",
            likesCount = 215,
            commentsCount = 28,
            sharesCount = 11,
            userReaction = null,
            locationName = "Patan Durbar Square, Lalitpur",
            tags = listOf("Marketplace", "Handicrafts", "SupportLocal")
        ),
        Post(
            id = "post_4",
            author = currentUser,
            content = "Building the all-in-one super-app for our nation: Feeds, Reels, Chats with Voice Notes, Marketplace, Dating, Jobs, and ConnectNP Pay Wallet in one cohesive experience! Built with pure Jetpack Compose & Kotlin. 💻🚀 What feature do you use the most?",
            timestamp = "1 day ago",
            likesCount = 512,
            commentsCount = 67,
            sharesCount = 33,
            userReaction = ReactionType.LIKE,
            locationName = "Kathmandu Tech Park",
            tags = listOf("AndroidDev", "Kotlin", "Compose", "ConnectNP")
        )
    )

    fun getInitialReels(): List<Reel> = listOf(
        Reel(
            id = "reel_1",
            creator = poojaUser,
            title = "Morning clouds floating through Pokhara valley with views of Machhapuchhre (Fishtail) 🌄🕊️",
            soundTitle = "Original Sound - Nepali Folk Instrumental Flute",
            likesCount = 14200,
            commentsCount = 890,
            sharesCount = 1450,
            isLiked = true,
            videoGradientColors = listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364)
        ),
        Reel(
            id = "reel_2",
            creator = aayushUser,
            title = "Testing out the new high-speed ConnectNP Pay instant P2P transfer! Zero fees anywhere in Nepal 🇳🇵💳",
            soundTitle = "ConnectNP Beat - Kathmandu Wave #TechNepal",
            likesCount = 8720,
            commentsCount = 340,
            sharesCount = 520,
            isLiked = false,
            videoGradientColors = listOf(0xFF1877F2, 0xFF142C64, 0xFF0A1024)
        ),
        Reel(
            id = "reel_3",
            creator = officialUser,
            title = "Meet ConnectNP AI: Ask anything about places to visit, Nepali cultural history, or translate in real-time! 🤖✨",
            soundTitle = "Futuristic Ambient - ConnectNP Studio",
            likesCount = 23900,
            commentsCount = 1200,
            sharesCount = 3100,
            isLiked = true,
            videoGradientColors = listOf(0xFFDC143C, 0xFF650B1D, 0xFF18191A)
        )
    )

    fun getInitialChatThreads(): List<ChatThread> = listOf(
        ChatThread(
            id = "chat_ai",
            user = aiBotUser,
            lastMessage = "नमस्ते! How can I help you today with ConnectNP features or Nepali travel?",
            lastMessageTime = "Just now",
            unreadCount = 1,
            isOnline = true,
            isAiAssistant = true
        ),
        ChatThread(
            id = "chat_aayush",
            user = aayushUser,
            lastMessage = "Sent you the escrow payment of रु 4,500 on ConnectNP Pay! 👍",
            lastMessageTime = "12:45 PM",
            unreadCount = 0,
            isOnline = true
        ),
        ChatThread(
            id = "chat_pooja",
            user = poojaUser,
            lastMessage = "Can't wait for our Annapurna trek next month! 🏔️",
            lastMessageTime = "Yesterday",
            unreadCount = 2,
            isOnline = false
        ),
        ChatThread(
            id = "chat_official",
            user = officialUser,
            lastMessage = "Your Creator Verification Badge has been approved! 🌟",
            lastMessageTime = "Oct 14",
            unreadCount = 0,
            isOnline = true
        )
    )

    fun getInitialMarketItems(): List<MarketItem> = listOf(
        MarketItem(
            id = "item_1",
            title = "Handcrafted 7-Metal Singing Bowl (Full Meditation Set)",
            priceNpr = 4500,
            location = "Patan, Lalitpur",
            category = "Handicrafts",
            seller = aayushUser,
            condition = "Brand New",
            imageResId = R.drawable.nepal_crafts_1792096421296,
            description = "Authentic handmade Tibetan/Nepali singing bowl crafted in Patan workshop. Includes wooden mallet, suede striker, and embroidered silk cushion. Pure rich resonant tones.",
            isFeatured = true
        ),
        MarketItem(
            id = "item_2",
            title = "100% Pure Himalayan Cashmere Pashmina Shawl",
            priceNpr = 7800,
            location = "Thamel, Kathmandu",
            category = "Fashion",
            seller = poojaUser,
            condition = "Brand New",
            imageResId = R.drawable.nepal_crafts_1792096421296,
            description = "Ultra soft premium grade Changthangi cashmere woven by experienced valley artisans. Light crimson and midnight blue pattern.",
            isFeatured = true
        ),
        MarketItem(
            id = "item_3",
            title = "MacBook Pro 14 M3 (16GB RAM / 512GB SSD) - Space Black",
            priceNpr = 215000,
            location = "New Road, Kathmandu",
            category = "Electronics",
            seller = aayushUser,
            condition = "Like New (99% battery)",
            description = "Purchased 4 months ago with full official warranty. Complete with original MagSafe cable and 70W brick. Ideal for Android & iOS mobile app development.",
            isFeatured = false
        ),
        MarketItem(
            id = "item_4",
            title = "Royal Enfield Classic 350 Reborn - Matte Bronze",
            priceNpr = 410000,
            location = "Lakeside, Pokhara",
            category = "Vehicles",
            seller = poojaUser,
            condition = "Used - 8,400 KM",
            description = "Impeccably maintained touring motorcycle. Serviced only at authorized center. Dual-channel ABS, touring seat, and crash guard installed.",
            isFeatured = false
        )
    )

    fun getInitialWatchVideos(): List<WatchVideo> = listOf(
        WatchVideo(
            id = "watch_1",
            title = "Everest Base Camp Trek 4K: Complete Cinematic Route Guide & Tips (2026)",
            channel = poojaUser,
            views = "142K views",
            timeAgo = "3 days ago",
            duration = "18:42",
            likesCount = "12K",
            isSubscribed = true,
            imageResId = R.drawable.himalaya_banner_1792096400169,
            description = "Join me on an epic 14-day trek through Lukla, Namche Bazaar, Tengboche Monastery, and Kala Patthar overlooking Mount Everest (8,848m)!"
        ),
        WatchVideo(
            id = "watch_2",
            title = "Discovering Kathmandu's Oldest Artisan Workshops in Durbar Square",
            channel = aayushUser,
            views = "89K views",
            timeAgo = "1 week ago",
            duration = "12:15",
            likesCount = "6.4K",
            isSubscribed = false,
            imageResId = R.drawable.nepal_crafts_1792096421296,
            description = "Behind the scenes with 5th generation metal-smiths, wood-carvers, and thangka painters in the heart of the Kathmandu Valley."
        ),
        WatchVideo(
            id = "watch_3",
            title = "ConnectNP Keynote: The Architecture Behind Nepal's #1 Super-App",
            channel = officialUser,
            views = "320K views",
            timeAgo = "2 weeks ago",
            duration = "24:50",
            likesCount = "28K",
            isSubscribed = true,
            imageResId = R.drawable.connectnp_icon_full,
            description = "A deep dive into our vision, real-time messaging, P2P payments, reels engine, and community empowerment."
        )
    )

    fun getInitialJobs(): List<JobItem> = listOf(
        JobItem(
            id = "job_1",
            title = "Senior Android Engineer (Jetpack Compose)",
            company = "ConnectNP Technologies",
            location = "Kathmandu (Hybrid / Remote)",
            salaryRange = "रु 160,000 - रु 240,000 / month",
            type = "Full-time",
            postedTime = "1 day ago",
            description = "Join our mobile core team to scale ConnectNP features across 1 billion devices. Deep expertise in Compose, Kotlin Coroutines, and MVVM required.",
            tags = listOf("Kotlin", "Compose", "Android", "Senior")
        ),
        JobItem(
            id = "job_2",
            title = "Product Designer & UX Researcher",
            company = "Himalaya Digital Labs",
            location = "Lalitpur, Nepal",
            salaryRange = "रु 110,000 - रु 170,000 / month",
            type = "Full-time",
            postedTime = "3 days ago",
            description = "Design intuitive experiences for emerging digital banking and e-commerce across urban and rural communities in Nepal.",
            tags = listOf("Figma", "UI/UX", "User Research", "M3")
        ),
        JobItem(
            id = "job_3",
            title = "Backend Systems Engineer (Kotlin / Go / Postgres)",
            company = "FinTech Nepal Global",
            location = "Pokhara / Remote",
            salaryRange = "रु 140,000 - रु 200,000 / month",
            type = "Remote",
            postedTime = "5 days ago",
            description = "Build resilient transaction processing pipelines, escrow wallets, and real-time message routing at scale.",
            tags = listOf("PostgreSQL", "Go", "Distributed Systems")
        )
    )

    fun getInitialDatingProfiles(): List<DatingProfile> = listOf(
        DatingProfile(
            id = "date_1",
            name = "Sneha Adhikari",
            age = 25,
            profession = "Architect & Coffee Lover",
            city = "Jhamsikhel, Lalitpur",
            bio = "Designing eco-friendly homes by day, watercoloring Newari heritage facades by evening. Love hiking around Shivapuri! ☕🌿",
            interests = listOf("Hiking", "Architecture", "Coffee", "Indie Music", "Photography"),
            distanceKm = 4
        ),
        DatingProfile(
            id = "date_2",
            name = "Rohan Shrestha",
            age = 27,
            profession = "Adventure Guide & Filmmaker",
            city = "Lakeside, Pokhara",
            bio = "Paragliding over Phewa lake, strumming acoustic guitar around campfires, and chasing sunsets in the Annapurnas. 🎸🪂",
            interests = listOf("Paragliding", "Guitar", "Camping", "Travel", "Dogs"),
            distanceKm = 12
        ),
        DatingProfile(
            id = "date_3",
            name = "Kritika Thapa",
            age = 24,
            profession = "Fintech Product Manager",
            city = "Baneshwor, Kathmandu",
            bio = "Building digital Nepal. Passionate about literature, spicy Buff Momo, and road trips to Mustang! 📚🥟🏍️",
            interests = listOf("Tech", "Momo Lover", "Books", "Road Trips", "Yoga"),
            distanceKm = 6
        )
    )

    fun getInitialWalletTransactions(): List<WalletTransaction> = listOf(
        WalletTransaction(
            id = "tx_1",
            title = "Escrow Payment Received - Singing Bowl",
            date = "Today, 1:20 PM",
            amountNpr = 4500.0,
            isCredit = true,
            category = "Marketplace Escrow"
        ),
        WalletTransaction(
            id = "tx_2",
            title = "P2P Transfer to Aayush Sharma",
            date = "Yesterday, 6:40 PM",
            amountNpr = 1250.0,
            isCredit = false,
            category = "P2P Transfer"
        ),
        WalletTransaction(
            id = "tx_3",
            title = "Wallet Top-up via eSewa",
            date = "Oct 13, 2026",
            amountNpr = 10000.0,
            isCredit = true,
            category = "Bank / Wallet Top-Up"
        ),
        WalletTransaction(
            id = "tx_4",
            title = "Creator Tip from Live Stream",
            date = "Oct 11, 2026",
            amountNpr = 600.0,
            isCredit = true,
            category = "Creator Earnings"
        )
    )
}
