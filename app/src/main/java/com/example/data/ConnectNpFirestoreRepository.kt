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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.example.util.ImageCompressor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
                "followersCount" to 0L,
                "followingCount" to 0L,
                "followers" to emptyList<String>(),
                "following" to emptyList<String>(),
                "postsCount" to 0L,
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
        val uid = userId ?: auth.currentUser?.uid
        if (uid == null) {
            emit(null)
            return@flow
        }
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
                    emit(null)
                }
        )
    }

    suspend fun getWalletBalance(userId: String? = null): Double {
        val uid = userId ?: auth.currentUser?.uid ?: return 0.0
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(FirestoreUserProfile::class.java)
                (profile?.walletBalance ?: 0L).toDouble()
            } else {
                0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    fun observeWalletBalance(userId: String? = null): Flow<Double> = flow {
        val uid = userId ?: auth.currentUser?.uid
        if (uid == null) {
            emit(0.0)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        val profile = snapshot.toObject(FirestoreUserProfile::class.java)
                        (profile?.walletBalance ?: 0L).toDouble()
                    } else {
                        0.0
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    emit(0.0)
                }
        )
    }

    suspend fun createPost(
        content: String,
        location: String = "Kathmandu, Nepal",
        authorName: String,
        authorHandle: String,
        mediaUrl: String? = null
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
            if (!mediaUrl.isNullOrBlank()) {
                payload["mediaUrl"] = mediaUrl
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Uploads photos, reels, or videos to Firebase Storage with automatic compression and fallback.
     */
    suspend fun uploadMediaFile(
        mediaUri: Uri,
        folder: String = "posts",
        context: Context? = null
    ): Result<String> {
        val uid = requireUserId()
        return try {
            val fileName = "${folder}_${uid}_${System.currentTimeMillis()}"
            val extension = if (folder == "reels" || folder == "videos") "mp4" else "jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child("$folder/$fileName.$extension")

            val compressedBytes = if (extension == "jpg" && context != null) {
                when (folder) {
                    "marketplace" -> ImageCompressor.compressMarketplaceImageToBytes(context, mediaUri)
                    "profile_pictures" -> ImageCompressor.compressProfileImageToBytes(context, mediaUri)
                    else -> ImageCompressor.compressPostImageToBytes(context, mediaUri)
                } ?: ImageCompressor.compressImageUriToBytes(context, mediaUri, 1080, 1080, 80)
            } else null

            val contentType = if (extension == "mp4") "video/mp4" else "image/jpeg"
            val metadata = StorageMetadata.Builder()
                .setContentType(contentType)
                .build()

            withTimeout(15000L) {
                if (compressedBytes != null && compressedBytes.isNotEmpty()) {
                    storageRef.putBytes(compressedBytes, metadata).await()
                } else {
                    storageRef.putFile(mediaUri, metadata).await()
                }
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Result.success(downloadUrl)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("ConnectNpFirestore", "Firebase Storage uploadMediaFile: ${e.message}, falling back to URI")
            Result.success(mediaUri.toString())
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

    private var lastVisiblePostSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null

    /**
     * Optimized real-time Feed query: ordered by createdAt descending and paginated (15 items initially).
     * Uses automatic single-field indexing in Firestore and bounds query to avoid downloading entire collection.
     */
    fun observeAllPosts(pageSize: Long = 15L): Flow<List<FirestorePost>> = flow {
        val path = "posts"
        emitAll(
            db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.isEmpty) {
                        lastVisiblePostSnapshot = snapshot.documents.lastOrNull()
                    }
                    snapshot.toObjects(FirestorePost::class.java)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    emitAll(
                        db.collection(path)
                            .limit(pageSize)
                            .snapshots()
                            .map { snapshot ->
                                snapshot.toObjects(FirestorePost::class.java).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                            }
                    )
                }
        )
    }

    /**
     * Loads the next page of 15 posts for smooth infinite scroll pagination.
     */
    suspend fun fetchNextPagePosts(pageSize: Long = 15L): Result<List<FirestorePost>> {
        val path = "posts"
        return try {
            var query = db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize)

            val cursor = lastVisiblePostSnapshot
            if (cursor != null) {
                query = query.startAfter(cursor)
            }

            val snapshot = query.get().await()
            if (!snapshot.isEmpty) {
                lastVisiblePostSnapshot = snapshot.documents.lastOrNull()
            }
            val posts = snapshot.toObjects(FirestorePost::class.java)
            Result.success(posts)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Updates mediaUrl of a post after background Storage upload completes.
     */
    suspend fun updatePostMediaUrl(postId: String, mediaUrl: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            db.collection("posts").document(postId).update(
                "mediaUrl", mediaUrl,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateWalletBalance(newBalance: Long): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val payload = hashMapOf<String, Any>(
                "walletBalance" to newBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateWalletMpin(newMpin: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val payload = hashMapOf<String, Any>(
                "walletMpin" to newMpin,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBiometricEnabled(enabled: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val payload = hashMapOf<String, Any>(
                "isBiometricEnabled" to enabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveLinkedPaymentMethod(
        type: String,
        accountNumber: String,
        accountHolderName: String,
        bankName: String = "",
        branch: String = "",
        isDefault: Boolean = false
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        val methodId = "pm_${type.lowercase()}_${System.currentTimeMillis()}"
        return try {
            val methodMap = hashMapOf<String, String>(
                "id" to methodId,
                "type" to type.uppercase(),
                "accountNumber" to accountNumber,
                "accountHolderName" to accountHolderName,
                "bankName" to bankName,
                "branch" to branch,
                "isDefault" to isDefault.toString()
            )

            val updatePayload = hashMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp(),
                "linkedPaymentMethods" to FieldValue.arrayUnion(methodMap)
            )

            when (type.uppercase()) {
                "ESEWA" -> updatePayload["esewaId"] = accountNumber
                "KHALTI" -> updatePayload["khaltiNumber"] = accountNumber
                "BANK" -> {
                    updatePayload["bankAccountNumber"] = accountNumber
                    updatePayload["bankName"] = bankName
                    updatePayload["bankAccountHolder"] = accountHolderName
                    updatePayload["bankBranch"] = branch
                }
            }

            db.collection("users").document(uid).set(updatePayload, SetOptions.merge()).await()

            val titleStr = when (type.uppercase()) {
                "ESEWA" -> "eSewa ($accountNumber)"
                "KHALTI" -> "Khalti ($accountNumber)"
                else -> if (bankName.isNotBlank()) "$bankName ($accountNumber)" else "Bank ($accountNumber)"
            }
            val subDoc = hashMapOf<String, Any>(
                "id" to methodId,
                "userId" to uid,
                "type" to type.uppercase(),
                "title" to titleStr,
                "accountNumber" to accountNumber,
                "accountHolderName" to accountHolderName,
                "bankName" to bankName,
                "branch" to branch,
                "isDefault" to isDefault,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("payment_methods").document(methodId).set(subDoc).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeLinkedPaymentMethod(methodMap: Map<String, String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        val methodId = methodMap["id"] ?: ""
        return try {
            val updatePayload = hashMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp(),
                "linkedPaymentMethods" to FieldValue.arrayRemove(methodMap)
            )
            db.collection("users").document(uid).set(updatePayload, SetOptions.merge()).await()
            if (methodId.isNotBlank()) {
                db.collection("users").document(uid).collection("payment_methods").document(methodId).delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordAdminRevenueFee(
        feeAmount: Long = 10L,
        transferAmount: Long,
        recipient: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "anonymous_user"
        val path = "admin_revenue/summary"
        return try {
            val summaryRef = db.collection("admin_revenue").document("summary")
            val payload = hashMapOf<String, Any>(
                "id" to "summary",
                "accountName" to "Admin Revenue",
                "totalRevenueNpr" to FieldValue.increment(feeAmount),
                "totalTransactionsCount" to FieldValue.increment(1),
                "lastFeeNpr" to feeAmount,
                "lastTransferNpr" to transferAmount,
                "lastRecipient" to recipient,
                "lastSenderUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            summaryRef.set(payload, SetOptions.merge()).await()

            val feeDocRef = summaryRef.collection("fees").document()
            val feeEntry = hashMapOf<String, Any>(
                "id" to feeDocRef.id,
                "senderUid" to uid,
                "recipient" to recipient,
                "transferAmount" to transferAmount,
                "feeAmount" to feeAmount,
                "totalDeducted" to (transferAmount + feeAmount),
                "type" to "p2p_transfer_fee",
                "createdAt" to FieldValue.serverTimestamp()
            )
            feeDocRef.set(feeEntry).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeAdminRevenue(): Flow<Long> = flow {
        emitAll(
            db.collection("admin_revenue").document("summary")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.getLong("totalRevenueNpr") ?: 0L
                    } else {
                        0L
                    }
                }
                .catch { emit(0L) }
        )
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
            // Compress profile picture to optimized 384x384 JPEG bytes (~25-45 KB)
            val compressedBytes = context?.let { ImageCompressor.compressProfileImageToBytes(it, imageUri) }
                ?: context?.let { ImageCompressor.compressImageUriToBytes(it, imageUri) }

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            // Strict 5-second timeout to prevent hangs
            withTimeout(5000L) {
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

    /**
     * Fast, optimized Firestore user profile update using SetOptions.merge().
     * Asynchronously syncs Firebase Auth profile to prevent blocking UI.
     */
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
            // Use set with SetOptions.merge() for fast, idempotent document writes
            db.collection("users").document(uid).set(updates, SetOptions.merge()).await()

            // Non-blocking background sync of Auth profile
            CoroutineScope(Dispatchers.IO).launch {
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
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Asynchronously updates only the user photoUrl once background Storage upload finishes.
     */
    suspend fun updateProfilePhotoUrl(photoUrl: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val updates = mapOf(
                "photoUrl" to photoUrl,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(updates, SetOptions.merge()).await()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(Uri.parse(photoUrl))
                        .build()
                    auth.currentUser?.updateProfile(profileUpdates)?.await()
                } catch (_: Exception) {}
            }

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

    suspend fun followUser(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        if (uid == targetUserId) return Result.failure(IllegalArgumentException("Cannot follow yourself"))
        val targetPath = "users/$targetUserId"
        return try {
            val batch = db.batch()
            val targetRef = db.collection("users").document(targetUserId)
            val myRef = db.collection("users").document(uid)

            batch.update(targetRef, mapOf(
                "followers" to FieldValue.arrayUnion(uid),
                "followersCount" to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.update(myRef, mapOf(
                "following" to FieldValue.arrayUnion(targetUserId),
                "followingCount" to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, targetPath)
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        if (uid == targetUserId) return Result.failure(IllegalArgumentException("Cannot unfollow yourself"))
        val targetPath = "users/$targetUserId"
        return try {
            val batch = db.batch()
            val targetRef = db.collection("users").document(targetUserId)
            val myRef = db.collection("users").document(uid)

            batch.update(targetRef, mapOf(
                "followers" to FieldValue.arrayRemove(uid),
                "followersCount" to FieldValue.increment(-1),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.update(myRef, mapOf(
                "following" to FieldValue.arrayRemove(targetUserId),
                "followingCount" to FieldValue.increment(-1),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, targetPath)
            Result.failure(e)
        }
    }
}
