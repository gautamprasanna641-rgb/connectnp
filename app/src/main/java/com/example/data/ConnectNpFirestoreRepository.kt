package com.example.data

import android.content.Context
import com.example.R
import com.example.model.FirestoreMarketItem
import com.example.model.FirestorePost
import com.example.model.FirestoreUserProfile
import com.example.model.FirestoreWalletTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
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

    suspend fun updateUserProfile(name: String, bio: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                "name", name,
                "bio", bio,
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
}
