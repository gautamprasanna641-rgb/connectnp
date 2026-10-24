package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ConnectNpRepository
import com.example.model.ReactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CONNECTNP", appName)
  }

  @Test
  fun `verify repository state and post creation`() {
    val initialCount = ConnectNpRepository.posts.value.size
    ConnectNpRepository.addPost("Testing ConnectNP from Robolectric!", "Kathmandu, Nepal")
    assertEquals(initialCount + 1, ConnectNpRepository.posts.value.size)
  }

  @Test
  fun `verify block user logic filters posts and users`() {
    val blockedId = "test_user_blocked_1"
    val blockedHandle = "@spammer_np"
    val blockedName = "Spam Account"

    // Verify initial state is not blocked
    assertEquals(false, ConnectNpRepository.isUserBlocked(blockedId, blockedHandle, blockedName))

    // Block the user
    ConnectNpRepository.blockUser(
        userId = blockedId,
        userName = blockedName,
        userHandle = blockedHandle
    )

    // User should now be blocked by ID, clean handle, with-@ handle, and name
    assertTrue(ConnectNpRepository.isUserBlocked(blockedId))
    assertTrue(ConnectNpRepository.isUserBlocked(userId = "", handle = blockedHandle))
    assertTrue(ConnectNpRepository.isUserBlocked(userId = "", handle = "spammer_np"))
    assertTrue(ConnectNpRepository.isUserBlocked(userId = "", name = blockedName))

    // Unblock the user
    ConnectNpRepository.unblockUser(
        userId = blockedId,
        userHandle = blockedHandle,
        userName = blockedName
    )
    assertEquals(false, ConnectNpRepository.isUserBlocked(blockedId, blockedHandle, blockedName))
  }

  @Test
  fun `verify notifications increment and clear`() {
    val initialCount = ConnectNpRepository.unreadNotifications.value
    ConnectNpRepository.incrementUnreadNotifications()
    assertEquals(initialCount + 1, ConnectNpRepository.unreadNotifications.value)

    ConnectNpRepository.clearNotifications()
    assertEquals(0, ConnectNpRepository.unreadNotifications.value)
  }

  @Test
  fun `verify dynamic user profile resolution and post authorship`() {
    val newUserName = "Kiran Shrestha"
    val newUserHandle = "@kiran_shrestha"
    val newUserPhoto = "https://images.unsplash.com/photo-kiran-avatar"

    // Simulate newly logged-in user profile update
    ConnectNpRepository.updateActiveUser(
        name = newUserName,
        handle = newUserHandle,
        photoUrl = newUserPhoto
    )

    val activeUser = ConnectNpRepository.getCurrentActiveUser()
    assertEquals(newUserName, activeUser.name)
    assertEquals(newUserHandle, activeUser.handle)
    assertEquals(newUserPhoto, activeUser.avatarUrl)

    // Create a new post
    ConnectNpRepository.addPost("Namaste world from my new account!", "Pokhara, Nepal")

    val latestPost = ConnectNpRepository.posts.value.first()
    assertEquals(newUserName, latestPost.author.name)
    assertEquals(newUserHandle, latestPost.author.handle)
    assertEquals(newUserPhoto, latestPost.author.avatarUrl)
    assertTrue("Author name must not be hardcoded Prasanna Gautam", latestPost.author.name != "Prasanna Gautam")
  }

  @Test
  fun `verify wallet transaction`() {
    ConnectNpRepository.topUpWallet(500.0, "eSewa")
    val currentBalance = ConnectNpRepository.walletBalance.value
    val success = ConnectNpRepository.sendMoney("Aayush Sharma", 100.0, "Test Payment")
    assertTrue(success)
    assertEquals(currentBalance - 100.0, ConnectNpRepository.walletBalance.value, 0.01)
  }

  @Test
  fun `verify ImageCompressor handles invalid or empty URI gracefully without crashing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nonExistentUri = android.net.Uri.parse("content://media/external/images/media/999999999")
    val result = com.example.util.ImageCompressor.compressImageUriToBytes(context, nonExistentUri)
    // Should return null and not throw an uncaught exception
    org.junit.Assert.assertNull(result)
  }

  @Test
  fun `verify ConnectNpAiService preserves conversation history across turns`() {
    // Turn 1: User asks about Momo food
    val msg1 = com.example.model.ChatMessage(id = "1", senderId = "me", text = "Tell me about Momo", isFromMe = true, timestamp = "1m")
    val historyTurn1 = listOf(msg1)

    // Turn 2: User asks "How much does it cost?" without explicitly repeating momo
    val response = com.example.service.ConnectNpAiService.generateContextAwareFallback(historyTurn1, "How much does it cost?")
    assertTrue("AI should recognize food/momo context from history", response.contains("Momo") || response.contains("NPR") || response.contains("रु"))

    // Trekking conversation history test
    val msgTrek = com.example.model.ChatMessage(id = "2", senderId = "me", text = "I want to do Annapurna trek", isFromMe = true, timestamp = "2m")
    val historyTrek = listOf(msgTrek)
    val responseTrekCost = com.example.service.ConnectNpAiService.generateContextAwareFallback(historyTrek, "what is the price?")
    assertTrue("AI should recognize trekking context from history for cost", responseTrekCost.contains("trekking") || responseTrekCost.contains("Permit") || responseTrekCost.contains("ACAP"))

    val responseTrekDuration = com.example.service.ConnectNpAiService.generateContextAwareFallback(historyTrek, "how long does it take?")
    assertTrue("AI should recognize trekking duration context", responseTrekDuration.contains("trek") || responseTrekDuration.contains("days"))
  }

  @Test
  fun `verify chat repository has AI assistant thread and tracks messages`() {
    val aiThread = ConnectNpRepository.chatThreads.value.find { it.id == "chat_ai" }
    assertTrue("chat_ai thread should exist", aiThread != null)
    assertTrue("chat_ai should be marked as AI assistant", aiThread?.isAiAssistant == true)

    val initialMessages = ConnectNpRepository.messagesByThread.value["chat_ai"] ?: emptyList()
    assertTrue("chat_ai should have initial welcome message", initialMessages.isNotEmpty())

    ConnectNpRepository.sendMessage("chat_ai", "Where can I eat momo in Kathmandu?")
    val updatedMessages = ConnectNpRepository.messagesByThread.value["chat_ai"] ?: emptyList()
    assertTrue("Messages should include the sent message", updatedMessages.any { it.text == "Where can I eat momo in Kathmandu?" })
  }

  @Test
  fun `verify admin authorization strictly restricted to owner email`() {
    val ownerEmail = "gautamprasanna641@gmail.com"
    val ownerEmailUppercase = "GAUTAMPRASANNA641@GMAIL.COM"
    val normalUserEmail = "regular_user@example.com"

    assertTrue("Owner email must be recognized as admin", ConnectNpRepository.isUserAdmin(ownerEmail))
    assertTrue("Owner email case-insensitive match", ConnectNpRepository.isUserAdmin(ownerEmailUppercase))
    org.junit.Assert.assertFalse("Regular user must NOT be admin", ConnectNpRepository.isUserAdmin(normalUserEmail))
    org.junit.Assert.assertFalse("Null email must NOT be admin", ConnectNpRepository.isUserAdmin(null))
    org.junit.Assert.assertFalse("Empty email must NOT be admin", ConnectNpRepository.isUserAdmin(""))
  }

  @Test
  fun `verify admin can grant and revoke user verified blue tick badge`() {
    val testUserId = "target_user_42"
    val testAuthor = com.example.model.User(
      id = testUserId,
      name = "Subash Karki",
      handle = "@subash",
      isVerified = false
    )

    // Add post with unverified user
    ConnectNpRepository.addPost(
      content = "Exploring Pokhara lakeside!",
      location = "Pokhara",
      customAuthor = testAuthor
    )

    val postBefore = ConnectNpRepository.posts.value.first { it.author.id == testUserId }
    org.junit.Assert.assertFalse("User should start unverified", postBefore.author.isVerified)

    // Admin grants Blue Tick
    ConnectNpRepository.toggleUserVerification(testUserId, true)
    assertTrue("isUserVerified should be true", ConnectNpRepository.isUserVerified(testUserId))
    val postAfterGrant = ConnectNpRepository.posts.value.first { it.author.id == testUserId }
    assertTrue("Post author should reflect verified Blue Tick badge", postAfterGrant.author.isVerified)

    // Admin revokes Blue Tick
    ConnectNpRepository.toggleUserVerification(testUserId, false)
    org.junit.Assert.assertFalse("isUserVerified should be false", ConnectNpRepository.isUserVerified(testUserId))
    val postAfterRevoke = ConnectNpRepository.posts.value.first { it.author.id == testUserId }
    org.junit.Assert.assertFalse("Post author should reflect revoked badge", postAfterRevoke.author.isVerified)
  }

  @Test
  fun `verify admin can delete any post and comment`() {
    // Create a test post
    ConnectNpRepository.addPost("Post to be moderated by admin", "Kathmandu")
    val createdPost = ConnectNpRepository.posts.value.first()
    val postId = createdPost.id

    // Add a comment to that post
    ConnectNpRepository.addComment(postId, "Comment to be moderated by admin")
    val comments = ConnectNpRepository.getCommentsForPost(postId)
    assertEquals(1, comments.size)
    val commentId = comments.first().id

    // Admin deletes comment
    ConnectNpRepository.deleteComment(postId, commentId)
    val commentsAfterDelete = ConnectNpRepository.getCommentsForPost(postId)
    assertTrue("Comment should be deleted", commentsAfterDelete.isEmpty())

    // Admin deletes post
    ConnectNpRepository.deletePost(postId)
    org.junit.Assert.assertFalse(
      "Post should be permanently removed from repository",
      ConnectNpRepository.posts.value.any { it.id == postId }
    )
  }
}

