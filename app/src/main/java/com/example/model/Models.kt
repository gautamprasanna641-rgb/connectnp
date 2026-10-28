package com.example.model

enum class ReactionType(val emoji: String, val label: String) {
    LIKE("👍", "Like"),
    LOVE("❤️", "Love"),
    NAMASTE("🙏", "Namaste"),
    HAHA("😆", "Haha"),
    WOW("😮", "Wow"),
    SAD("😢", "Sad"),
    ANGRY("😡", "Angry")
}

data class User(
    val id: String,
    val name: String,
    val handle: String,
    val avatarUrl: String? = null,
    val avatarResId: Int? = null,
    val isVerified: Boolean = false,
    val isCreator: Boolean = false,
    val bio: String = "",
    val location: String = "Kathmandu, Nepal",
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

data class PollOption(
    val id: Int,
    val text: String,
    val votes: Int
)

data class PollData(
    val question: String,
    val options: List<PollOption>,
    val totalVotes: Int,
    val userVotedOptionId: Int? = null
)

data class Comment(
    val id: String,
    val author: User,
    val text: String,
    val timeAgo: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val replies: List<Comment> = emptyList()
)

data class Post(
    val id: String,
    val author: User,
    val content: String,
    val mediaResId: Int? = null,
    val mediaUrl: String? = null,
    val timestamp: String = "Just now",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val userReaction: ReactionType? = null,
    val poll: PollData? = null,
    val tags: List<String> = emptyList(),
    val locationName: String? = null,
    val isBookmarked: Boolean = false,
    val isPagePost: Boolean = false,
    val pageId: String? = null
)

data class OfficialPage(
    val id: String = "official_connectnp",
    val pageName: String = "CONNECTNP Official",
    val handle: String = "@connectnp_official",
    val bio: String = "Official page of ConnectNP - Nepal's Premier Social, Community & Payments Super-App 🇳🇵",
    val profilePhoto: String? = null,
    val coverPhoto: String? = null,
    val category: String = "Official Organization",
    val website: String = "https://connectnp.app",
    val followersCount: Long = 0L,
    val isVerified: Boolean = true,
    val ownerEmail: String = "gautamprasanna641@gmail.com",
    val ownerId: String = "",
    val isFollowing: Boolean = false
)

data class Story(
    val id: String,
    val author: User,
    val mediaResId: Int? = null,
    val mediaUrl: String? = null,
    val caption: String = "",
    val timestamp: String = "2h ago",
    val isViewed: Boolean = false,
    val isCloseFriends: Boolean = false
)

data class Reel(
    val id: String,
    val creator: User,
    val title: String,
    val soundTitle: String,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val videoUrl: String? = null,
    val videoGradientColors: List<Long> = listOf(0xFF1877F2, 0xFFDC143C)
)

enum class MessageStatus { SENT, DELIVERED, READ }
enum class MessageMediaType { TEXT, AUDIO_VOICE, IMAGE }

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val status: MessageStatus = MessageStatus.READ,
    val mediaType: MessageMediaType = MessageMediaType.TEXT,
    val audioDurationSec: Int? = null,
    val reactionEmoji: String? = null
)

data class ChatThread(
    val id: String,
    val user: User,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isAiAssistant: Boolean = false
)

data class MarketItem(
    val id: String,
    val title: String,
    val priceNpr: Long,
    val location: String,
    val category: String,
    val seller: User,
    val condition: String,
    val imageResId: Int? = null,
    val imageUrl: String? = null,
    val description: String,
    val isFeatured: Boolean = false,
    val isSold: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)


data class WatchVideo(
    val id: String,
    val title: String,
    val channel: User,
    val views: String,
    val timeAgo: String,
    val duration: String,
    val description: String,
    val likesCount: String,
    val isSubscribed: Boolean = false,
    val imageResId: Int? = null,
    val videoUrl: String? = null,
    val tags: List<String> = emptyList()
)

data class JobItem(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val salaryRange: String,
    val type: String, // Full-time, Remote, etc.
    val postedTime: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val hasApplied: Boolean = false
)

data class DatingProfile(
    val id: String,
    val name: String,
    val age: Int,
    val profession: String,
    val city: String,
    val bio: String,
    val interests: List<String>,
    val distanceKm: Int
)

data class WalletTransaction(
    val id: String,
    val title: String,
    val date: String,
    val amountNpr: Double,
    val isCredit: Boolean,
    val category: String,
    val status: String = "Completed"
)

enum class PaymentMethodType(val displayName: String, val code: String) {
    ESEWA("eSewa ID", "esewa"),
    KHALTI("Khalti Number", "khalti"),
    BANK("Bank Account", "bank")
}

data class LinkedPaymentMethod(
    val id: String = "",
    val type: PaymentMethodType = PaymentMethodType.ESEWA,
    val title: String = "",
    val accountNumber: String = "",
    val accountHolderName: String = "",
    val bankName: String = "",
    val branch: String = "",
    val isDefault: Boolean = false,
    val linkedAt: String = "Active"
)

