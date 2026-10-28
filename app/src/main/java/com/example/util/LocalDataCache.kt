package com.example.util

import android.content.Context
import android.util.Log
import com.example.model.Post
import com.example.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object LocalDataCache {

    private const val TAG = "LocalDataCache"
    private const val FEED_CACHE_FILE = "connectnp_feed_cache.json"
    private const val PROFILE_CACHE_FILE = "connectnp_profile_cache.json"

    /**
     * Caches feed posts locally to disk so the feed renders instantly on cold start.
     */
    fun saveCachedFeed(context: Context, posts: List<Post>) {
        try {
            val jsonArray = JSONArray()
            // Cache up to 30 most recent posts
            posts.take(30).forEach { post ->
                val obj = JSONObject().apply {
                    put("id", post.id)
                    put("content", post.content)
                    put("location", post.locationName ?: "")
                    put("mediaUrl", post.mediaUrl ?: "")
                    put("timestamp", post.timestamp)
                    put("likesCount", post.likesCount)
                    put("commentsCount", post.commentsCount)
                    put("sharesCount", post.sharesCount)
                    put("isPagePost", post.isPagePost)
                    put("pageId", post.pageId ?: "")

                    // Author
                    val authorObj = JSONObject().apply {
                        put("id", post.author.id)
                        put("name", post.author.name)
                        put("handle", post.author.handle)
                        put("avatarUrl", post.author.avatarUrl ?: "")
                        put("isVerified", post.author.isVerified)
                        put("isCreator", post.author.isCreator)
                    }
                    put("author", authorObj)
                }
                jsonArray.put(obj)
            }

            val file = File(context.cacheDir, FEED_CACHE_FILE)
            file.writeText(jsonArray.toString())
            Log.d(TAG, "Cached ${posts.size} posts locally (${file.length() / 1024} KB)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache feed: ${e.message}")
        }
    }

    /**
     * Loads locally cached feed posts. Returns empty list if no cache is present.
     */
    fun loadCachedFeed(context: Context): List<Post> {
        return try {
            val file = File(context.cacheDir, FEED_CACHE_FILE)
            if (!file.exists()) return emptyList()

            val text = file.readText()
            val jsonArray = JSONArray(text)
            val posts = mutableListOf<Post>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val authorObj = obj.optJSONObject("author")
                val author = User(
                    id = authorObj?.optString("id") ?: "user",
                    name = authorObj?.optString("name") ?: "ConnectNP User",
                    handle = authorObj?.optString("handle") ?: "@user",
                    avatarUrl = authorObj?.optString("avatarUrl")?.ifBlank { null },
                    isVerified = authorObj?.optBoolean("isVerified") ?: false,
                    isCreator = authorObj?.optBoolean("isCreator") ?: false
                )

                posts.add(
                    Post(
                        id = obj.getString("id"),
                        author = author,
                        content = obj.optString("content"),
                        locationName = obj.optString("location").ifBlank { null },
                        mediaUrl = obj.optString("mediaUrl").ifBlank { null },
                        timestamp = obj.optString("timestamp", "Recently"),
                        likesCount = obj.optInt("likesCount", 0),
                        commentsCount = obj.optInt("commentsCount", 0),
                        sharesCount = obj.optInt("sharesCount", 0),
                        isPagePost = obj.optBoolean("isPagePost", false),
                        pageId = obj.optString("pageId").ifBlank { null }
                    )
                )
            }
            Log.d(TAG, "Loaded ${posts.size} posts from local cache")
            posts
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cached feed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Caches user profile for 0ms profile screen rendering.
     */
    fun saveCachedUserProfile(context: Context, user: User) {
        try {
            val obj = JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("handle", user.handle)
                put("avatarUrl", user.avatarUrl ?: "")
                put("isVerified", user.isVerified)
                put("isCreator", user.isCreator)
                put("bio", user.bio)
                put("location", user.location)
                put("followersCount", user.followersCount)
                put("followingCount", user.followingCount)
            }
            val file = File(context.cacheDir, PROFILE_CACHE_FILE)
            file.writeText(obj.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache user profile: ${e.message}")
        }
    }

    /**
     * Loads cached user profile.
     */
    fun loadCachedUserProfile(context: Context): User? {
        return try {
            val file = File(context.cacheDir, PROFILE_CACHE_FILE)
            if (!file.exists()) return null

            val obj = JSONObject(file.readText())
            User(
                id = obj.getString("id"),
                name = obj.getString("name"),
                handle = obj.getString("handle"),
                avatarUrl = obj.optString("avatarUrl").ifBlank { null },
                isVerified = obj.optBoolean("isVerified", false),
                isCreator = obj.optBoolean("isCreator", false),
                bio = obj.optString("bio", ""),
                location = obj.optString("location", "Kathmandu, Nepal"),
                followersCount = obj.optInt("followersCount", 0),
                followingCount = obj.optInt("followingCount", 0)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cached profile: ${e.message}")
            null
        }
    }
}
