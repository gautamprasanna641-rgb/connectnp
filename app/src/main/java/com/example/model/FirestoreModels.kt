package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val handle: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val walletBalance: Long = 0L,
    val blockedUsers: List<String> = emptyList(),
    val fcmToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreComment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestorePost(
    val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val content: String = "",
    val location: String = "",
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreMarketItem(
    val id: String = "",
    val userId: String = "",
    val sellerName: String = "",
    val title: String = "",
    val priceNpr: Long = 0L,
    val category: String = "",
    val location: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreWalletTransaction(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val type: String = "credit",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreReport(
    val id: String = "",
    val reportedBy: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val targetAuthorId: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

fun FirestorePost.toPost(): Post {
    val authorUser = User(
        id = userId,
        name = authorName.ifBlank { "ConnectNP User" },
        handle = if (authorHandle.startsWith("@")) authorHandle else if (authorHandle.isNotBlank()) "@$authorHandle" else "@user"
    )
    val timeAgoStr = createdAt?.toDate()?.let { date ->
        val diffMs = System.currentTimeMillis() - date.time
        val mins = diffMs / (1000 * 60)
        when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            mins < 1440 -> "${mins / 60}h ago"
            else -> "${mins / 1440}d ago"
        }
    } ?: "Just now"

    return Post(
        id = id,
        author = authorUser,
        content = content,
        locationName = location.ifBlank { null },
        likesCount = likesCount.toInt(),
        timestamp = timeAgoStr
    )
}

fun FirestoreWalletTransaction.toWalletTransaction(): WalletTransaction {
    val dateStr = createdAt?.toDate()?.let { date ->
        val diffMs = System.currentTimeMillis() - date.time
        val mins = diffMs / (1000 * 60)
        when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            mins < 1440 -> "Today"
            else -> "${mins / 1440}d ago"
        }
    } ?: "Today, Just now"

    return WalletTransaction(
        id = id,
        title = title,
        date = dateStr,
        amountNpr = amount.toDouble(),
        isCredit = type == "credit",
        category = if (type == "credit") "Wallet Top-Up" else "P2P Payment"
    )
}
