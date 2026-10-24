package com.example.data

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.model.FirestoreComment
import com.example.model.FirestoreMarketItem
import com.example.model.FirestorePage
import com.example.model.FirestorePost
import com.example.model.FirestoreUserProfile
import com.example.model.FirestoreWalletTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.example.util.ImageCompressor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ConnectNpFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance(db.app)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    suspend fun saveUserProfile(
        name: String,
        handle: String,
        bio: String = "",
        walletBalance: Long = 0L
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "name" to name,
                "handle" to handle,
                "bio" to bio,
                "walletBalance" to walletBalance,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String? = null): Flow<FirestoreUserProfile?> = flow {
        val uid = userId ?: requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(FirestoreUserProfile::class.java)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun createPost(
        content: String,
        location: String = "Kathmandu, Nepal",
        authorName: String,
        authorHandle: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "posts"
        val colRef = db.collection(path)
        val docRef = colRef.document()
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "userId" to uid,
                "authorName" to authorName,
                "authorHandle" to authorHandle,
                "content" to content,
                "location" to location,
                "likesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePost(
        postId: String,
        content: String,
        location: String = "Kathmandu, Nepal"
    ): Result<Unit> {
        requireUserId()
        val path = "posts/$postId"
        return try {
            val docRef = db.collection("posts").document(postId)
            docRef.update(
                mapOf(
                    "content" to content,
                    "location" to location,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeUserPosts(userId: String? = null): Flow<List<FirestorePost>> = flow {
        val uid = userId ?: requireUserId()
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestorePost::class.java)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllPosts(): Flow<List<FirestorePost>> = flow {
        val path = "posts"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestorePost::class.java).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun updateWalletBalance(newBalance: Long): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                "walletBalance", newBalance,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createMarketItem(
        title: String,
        priceNpr: Long,
        category: String,
        location: String,
        description: String,
        sellerName: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "market_items"
        val colRef = db.collection(path)
        val docRef = colRef.document()
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "userId" to uid,
                "sellerName" to sellerName,
                "title" to title,
                "priceNpr" to priceNpr,
                "category" to category,
                "location" to location,
                "description" to description,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeUserMarketItems(userId: String? = null): Flow<List<FirestoreMarketItem>> = flow {
        val uid = userId ?: requireUserId()
        val path = "market_items"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestoreMarketItem::class.java)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addTransaction(
        title: String,
        amount: Long,
        type: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        val colRef = db.collection("users").document(uid).collection("transactions")
        val docRef = colRef.document()
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "userId" to uid,
                "title" to title,
                "amount" to amount,
                "type" to type,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeTransactions(userId: String? = null): Flow<List<FirestoreWalletTransaction>> = flow {
        val uid = userId ?: requireUserId()
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestoreWalletTransaction::class.java)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun deleteUserAccount(userId: String): Result<Unit> {
        val uid = requireUserId()
        if (uid != userId) {
            return Result.failure(IllegalArgumentException("Cannot delete account for another user"))
        }
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun blockUser(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                "blockedUsers", FieldValue.arrayUnion(targetUserId),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                "blockedUsers", FieldValue.arrayRemove(targetUserId),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reportContent(
        targetType: String,
        targetId: String,
        targetAuthorId: String,
        reason: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "reports"
        val docRef = db.collection(path).document()
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "reportedBy" to uid,
                "targetType" to targetType,
                "targetId" to targetId,
                "targetAuthorId" to targetAuthorId,
                "reason" to reason,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                "fcmToken", token,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePicture(imageUri: Uri, context: Context? = null): Result<String> {
        val uid = requireUserId()
        return try {
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$uid.jpg")
            val compressedBytes = context?.let { ImageCompressor.compressImageUriToBytes(it, imageUri) }

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            // Strict 6-second timeout to prevent UI hangs on slow networks or storage issues
            withTimeout(6000L) {
                if (compressedBytes != null && compressedBytes.isNotEmpty()) {
                    storageRef.putBytes(compressedBytes, metadata).await()
                } else {
                    storageRef.putFile(imageUri, metadata).await()
                }
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Result.success(downloadUrl)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("ConnectNpFirestore", "Firebase Storage upload: ${e.message}, falling back to URI")
            Result.success(imageUri.toString())
        }
    }

    suspend fun updateUserProfile(name: String, bio: String, photoUrl: String? = null): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "bio" to bio,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!photoUrl.isNullOrBlank()) {
                updates["photoUrl"] = photoUrl
            }
            db.collection("users").document(uid).update(updates).await()

            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .apply {
                        if (!photoUrl.isNullOrBlank()) {
                            setPhotoUri(Uri.parse(photoUrl))
                        }
                    }
                    .build()
                auth.currentUser?.updateProfile(profileUpdates)?.await()
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<FirestoreUserProfile>> {
        val path = "users"
        return try {
            val snapshot = db.collection(path).get().await()
            val allUsers = snapshot.toObjects(FirestoreUserProfile::class.java)
            val trimmed = query.trim()
            val filtered = if (trimmed.isBlank()) {
                allUsers.take(15)
            } else {
                allUsers.filter { user ->
                    user.name.contains(trimmed, ignoreCase = true) ||
                    user.handle.contains(trimmed, ignoreCase = true)
                }
            }
            Result.success(filtered)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(
        postId: String,
        text: String,
        authorName: String,
        authorHandle: String = "",
        authorAvatarUrl: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        val colRef = db.collection("posts").document(postId).collection("comments")
        val docRef = colRef.document()
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "postId" to postId,
                "userId" to uid,
                "authorName" to authorName,
                "authorHandle" to authorHandle,
                "authorAvatarUrl" to authorAvatarUrl,
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            try {
                db.collection("posts").document(postId).update(
                    "commentsCount", FieldValue.increment(1),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (_: Exception) {}
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeComments(postId: String): Flow<List<FirestoreComment>> = flow {
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestoreComment::class.java)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        val path = "posts/$postId/comments/$commentId"
        return try {
            db.collection("posts").document(postId).collection("comments").document(commentId).delete().await()
            try {
                db.collection("posts").document(postId).update(
                    "commentsCount", FieldValue.increment(-1),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setUserVerification(userId: String, isVerified: Boolean): Result<Unit> {
        val path = "users/$userId"
        return try {
            db.collection("users").document(userId).update(
                "isVerified", isVerified,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<FirestoreUserProfile>> {
        val path = "users"
        return try {
            val snapshot = db.collection(path).get().await()
            val users = snapshot.toObjects(FirestoreUserProfile::class.java)
            Result.success(users)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    fun observeOfficialPage(pageId: String = "official_connectnp"): Flow<FirestorePage?> = flow {
        val path = "pages/$pageId"
        emitAll(
            db.collection("pages").document(pageId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(FirestorePage::class.java)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveOfficialPage(page: com.example.model.OfficialPage): Result<Unit> {
        val uid = requireUserId()
        val path = "pages/${page.id}"
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to page.id,
                "pageName" to page.pageName,
                "handle" to page.handle,
                "bio" to page.bio,
                "category" to page.category,
                "website" to page.website,
                "followersCount" to page.followersCount,
                "isVerified" to page.isVerified,
                "ownerEmail" to page.ownerEmail,
                "ownerId" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("pages").document(page.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setPageFollowStatus(pageId: String, isFollowing: Boolean): Result<Unit> {
        val uid = requireUserId()
        val path = "pages/$pageId"
        return try {
            val docRef = db.collection("pages").document(pageId)
            val updatePayload = if (isFollowing) {
                mapOf(
                    "followers" to FieldValue.arrayUnion(uid),
                    "followersCount" to FieldValue.increment(1)
                )
            } else {
                mapOf(
                    "followers" to FieldValue.arrayRemove(uid),
                    "followersCount" to FieldValue.increment(-1)
                )
            }
            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createPagePost(
        content: String,
        location: String = "Kathmandu, Nepal",
        pageName: String = "CONNECTNP Official",
        pageHandle: String = "@connectnp_official",
        pageId: String = "official_connectnp"
    ): Result<String> {
        val uid = requireUserId()
        val path = "posts"
        val colRef = db.collection(path)
        val docRef = colRef.document()
        return try {
            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "userId" to uid,
                "authorName" to pageName,
                "authorHandle" to pageHandle,
                "content" to content,
                "location" to location,
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "isPagePost" to true,
                "pageId" to pageId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
