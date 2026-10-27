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
    val isVerified: Boolean = false,
    val walletBalance: Long = 0L,
    val walletMpin: String = "",
    val isBiometricEnabled: Boolean = false,
    val esewaId: String = "",
    val khaltiNumber: String = "",
    val bankAccountNumber: String = "",
    val bankName: String = "",
    val bankAccountHolder: String = "",
    val bankBranch: String = "",
    val linkedPaymentMethods: List<Map<String, String>> = emptyList(),
    val blockedUsers: List<String> = emptyList(),
    val fcmToken: String = "",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val postsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestorePaymentMethod(
    val id: String = "",
    val userId: String = "",
    val type: String = "ESEWA",
    val title: String = "",
    val accountNumber: String = "",
    val accountHolderName: String = "",
    val bankName: String = "",
    val branch: String = "",
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
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
    val mediaUrl: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val isPagePost: Boolean = false,
    val pageId: String = "",
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

@IgnoreExtraProperties
data class FirestorePage(
    val id: String = "official_connectnp",
    val pageName: String = "CONNECTNP Official",
    val handle: String = "@connectnp_official",
    val bio: String = "Official page of ConnectNP - Nepal's Premier Social, Community & Payments Super-App 🇳🇵",
    val profilePhoto: String = "",
    val coverPhoto: String = "",
    val category: String = "Official Organization",
    val website: String = "https://connectnp.app",
    val followersCount: Long = 0L,
    val isVerified: Boolean = true,
    val ownerEmail: String = "gautamprasanna641@gmail.com",
    val ownerId: String = "",
    val followers: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun FirestorePage.toOfficialPage(currentUserId: String = ""): OfficialPage {
    return OfficialPage(
        id = id,
        pageName = pageName,
        handle = handle,
        bio = bio,
        profilePhoto = profilePhoto.ifBlank { null },
        coverPhoto = coverPhoto.ifBlank { null },
        category = category,
        website = website,
        followersCount = followersCount,
        isVerified = isVerified,
        ownerEmail = ownerEmail,
        ownerId = ownerId,
        isFollowing = if (currentUserId.isNotBlank()) currentUserId in followers else com.example.data.ConnectNpRepository.isPageFollowed(id)
    )
}

fun FirestorePost.toPost(): Post {
    val isOfficialPage = isPagePost || pageId == "official_connectnp"
    val authorUser = if (isOfficialPage) {
        User(
            id = "official_connectnp",
            name = authorName.ifBlank { "CONNECTNP Official" },
            handle = if (authorHandle.startsWith("@")) authorHandle else "@connectnp_official",
            isVerified = true
        )
    } else {
        User(
            id = userId,
            name = authorName.ifBlank { "ConnectNP User" },
            handle = if (authorHandle.startsWith("@")) authorHandle else if (authorHandle.isNotBlank()) "@$authorHandle" else "@user",
            isVerified = com.example.data.ConnectNpRepository.isUserVerified(userId)
        )
    }
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
        mediaUrl = mediaUrl.ifBlank { null },
        likesCount = likesCount.toInt(),
        commentsCount = commentsCount.toInt(),
        timestamp = timeAgoStr,
        isPagePost = isOfficialPage,
        pageId = if (isOfficialPage) "official_connectnp" else pageId.ifBlank { null }
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
