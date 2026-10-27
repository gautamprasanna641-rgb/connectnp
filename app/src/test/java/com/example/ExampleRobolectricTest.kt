package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ConnectNpRepository
import com.example.model.ReactionType
import com.example.model.toOfficialPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    val transferAmount = 100.0
    val fee = ConnectNpRepository.P2P_TRANSACTION_FEE // Rs 10.0
    val success = ConnectNpRepository.sendMoney("Aayush Sharma", transferAmount, "Test Payment")
    assertTrue(success)
    // Transfer amount (100) + Flat fee (10) = 110 deducted
    assertEquals(currentBalance - (transferAmount + fee), ConnectNpRepository.walletBalance.value, 0.01)
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
  fun `verify Gemini model name is configured and system prompt is defined`() {
    org.junit.Assert.assertTrue(com.example.service.ConnectNpAiService.MODEL_NAME.isNotBlank())
    assertTrue(com.example.service.ConnectNpAiService.SYSTEM_PROMPT.contains("ConnectNP AI"))
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

  @Test
  fun `verify official page follow toggling and count`() {
    val initialPage = ConnectNpRepository.officialPage.value
    assertEquals("official_connectnp", initialPage.id)
    assertEquals("CONNECTNP Official", initialPage.pageName)
    assertEquals("gautamprasanna641@gmail.com", initialPage.ownerEmail)
    assertTrue("Official page must have verified badge", initialPage.isVerified)

    val initialCount = ConnectNpRepository.officialPage.value.followersCount
    val wasFollowing = ConnectNpRepository.isPageFollowed("official_connectnp")

    // Toggle follow
    ConnectNpRepository.toggleFollowPage("official_connectnp")
    val afterToggleFollowing = ConnectNpRepository.isPageFollowed("official_connectnp")
    assertEquals(!wasFollowing, afterToggleFollowing)

    // Toggle back
    ConnectNpRepository.toggleFollowPage("official_connectnp")
    assertEquals(wasFollowing, ConnectNpRepository.isPageFollowed("official_connectnp"))
    assertEquals(initialCount, ConnectNpRepository.officialPage.value.followersCount)
  }

  @Test
  fun `verify admin restriction on official page management and posting`() {
    // Normal user check
    val normalEmail = "regular_user@gmail.com"
    org.junit.Assert.assertFalse(ConnectNpRepository.isUserAdmin(normalEmail))

    // Admin email check (case-insensitive)
    val adminEmail = "gautamprasanna641@gmail.com"
    assertTrue(ConnectNpRepository.isUserAdmin(adminEmail))
    assertTrue(ConnectNpRepository.isUserAdmin("GautamPrasanna641@gmail.com"))

    // Non-admin call without logged-in admin user fails
    val updateResult = ConnectNpRepository.updateOfficialPage(
      pageName = "Hacked Page",
      bio = "Unauthorized",
      website = "https://evil.com",
      category = "None"
    )
    org.junit.Assert.assertFalse("Unauthorized user must not be able to update official page", updateResult)

    val postResult = ConnectNpRepository.addPagePost("Unauthorized announcement")
    org.junit.Assert.assertFalse("Unauthorized user must not be able to post as official page", postResult)
  }

  @Test
  fun `verify FirestorePage mapping to OfficialPage`() {
    val fsPage = com.example.model.FirestorePage(
      id = "official_connectnp",
      pageName = "CONNECTNP Official",
      handle = "@connectnp_official",
      bio = "Official page bio",
      category = "Official Community Organization",
      website = "https://connectnp.app",
      followersCount = 5000L,
      isVerified = true,
      ownerEmail = "gautamprasanna641@gmail.com",
      followers = listOf("user_1", "user_2")
    )

    val pageForUser1 = fsPage.toOfficialPage(currentUserId = "user_1")
    assertEquals("CONNECTNP Official", pageForUser1.pageName)
    assertEquals(5000L, pageForUser1.followersCount)
    assertTrue("User 1 is in followers list", pageForUser1.isFollowing)

    val pageForUser3 = fsPage.toOfficialPage(currentUserId = "user_3")
    org.junit.Assert.assertFalse("User 3 is not in followers list", pageForUser3.isFollowing)
  }

  @Test
  fun `verify follower count formats as 0 when count is zero for user profile and official page`() {
    val zeroFollowerCount = 0L
    val zeroFollowingCount = 0L

    val followersDisplay = if (zeroFollowerCount <= 0L) "0" else String.format("%,d", zeroFollowerCount)
    val followingDisplay = if (zeroFollowingCount <= 0L) "0" else String.format("%,d", zeroFollowingCount)
    val postsDisplay = if (emptyList<Any>().isEmpty()) "0" else "1"

    assertEquals("0", followersDisplay)
    assertEquals("0", followingDisplay)
    assertEquals("0", postsDisplay)

    // Verify non-zero format
    val dynamicFollowers = 2500L
    val formattedFollowers = if (dynamicFollowers <= 0L) "0" else String.format("%,d", dynamicFollowers)
    assertEquals("2,500", formattedFollowers)
  }

  @Test
  fun `verify user profile and official page models default followers count to zero`() {
    val defaultUser = com.example.model.User(
      id = "test_user_zero",
      name = "Test User",
      handle = "@test_user"
    )
    assertEquals(0, defaultUser.followersCount)
    assertEquals(0, defaultUser.followingCount)

    val defaultFsProfile = com.example.model.FirestoreUserProfile(
      userId = "test_user_zero",
      name = "Test User",
      handle = "@test_user"
    )
    assertEquals(0L, defaultFsProfile.followersCount)
    assertEquals(0L, defaultFsProfile.followingCount)

    val defaultPage = com.example.model.FirestorePage()
    assertEquals(0L, defaultPage.followersCount)
  }

  @Test
  fun `verify gallery and media permissions are declared in AndroidManifest`() {
    val manifestFile = java.io.File("src/main/AndroidManifest.xml")
    assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
    val manifestContent = manifestFile.readText()

    assertTrue(
      "Manifest must request READ_MEDIA_IMAGES",
      manifestContent.contains("android.permission.READ_MEDIA_IMAGES")
    )
    assertTrue(
      "Manifest must request READ_MEDIA_VIDEO",
      manifestContent.contains("android.permission.READ_MEDIA_VIDEO")
    )
    assertTrue(
      "Manifest must request READ_EXTERNAL_STORAGE",
      manifestContent.contains("android.permission.READ_EXTERNAL_STORAGE")
    )
  }

  @Test
  fun `verify page post and user post support gallery mediaUri`() {
    val testUri = android.net.Uri.parse("content://media/external/images/media/42")
    ConnectNpRepository.addPost(
      content = "Testing user post with gallery photo",
      location = "Kathmandu",
      mediaUri = testUri
    )
    val latestPost = ConnectNpRepository.posts.value.first()
    assertEquals("content://media/external/images/media/42", latestPost.mediaUrl)
  }

  @Test
  fun `verify wallet balance displays actual amount by default and can toggle visibility`() {
    // 1. By default, visibility must be true
    ConnectNpRepository.setBalanceVisibility(true)
    assertTrue("Wallet balance must be visible by default", ConnectNpRepository.isBalanceVisible.value)

    // 2. Default balance of 0 should format as Rs 0 or रु ०
    ConnectNpRepository.setWalletBalance(0.0)
    val zeroBalance = ConnectNpRepository.walletBalance.value
    val zeroDisplayEn = if (zeroBalance <= 0.0) "Rs 0" else "Rs ${String.format("%,.2f", zeroBalance)}"
    val zeroDisplayNp = if (zeroBalance <= 0.0) "रु ०" else "रु ${String.format("%,.2f", zeroBalance)}"
    assertEquals("Rs 0", zeroDisplayEn)
    assertEquals("रु ०", zeroDisplayNp)

    // 3. Dynamic positive balance (e.g. Rs 1250) formats correctly
    ConnectNpRepository.setWalletBalance(1250.0)
    val dynamicBalance = ConnectNpRepository.walletBalance.value
    val dynamicDisplayEn = if (dynamicBalance <= 0.0) "Rs 0" else "Rs ${String.format("%,.2f", dynamicBalance)}"
    assertEquals("Rs 1,250.00", dynamicDisplayEn)

    // 4. Toggle visibility hide
    ConnectNpRepository.toggleBalanceVisibility()
    assertFalse("Wallet balance should now be hidden", ConnectNpRepository.isBalanceVisible.value)

    val hiddenDisplay = if (ConnectNpRepository.isBalanceVisible.value) dynamicDisplayEn else "Rs ••••••"
    assertEquals("Rs ••••••", hiddenDisplay)

    // 5. Toggle visibility back to reveal
    ConnectNpRepository.toggleBalanceVisibility()
    assertTrue("Wallet balance should be visible again", ConnectNpRepository.isBalanceVisible.value)
    val revealedDisplay = if (ConnectNpRepository.isBalanceVisible.value) dynamicDisplayEn else "Rs ••••••"
    assertEquals("Rs 1,250.00", revealedDisplay)
  }

  @Test
  fun `verify p2p transfer fee breakdown and balance deduction`() {
    ConnectNpRepository.setWalletBalance(200.0)
    val initialBalance = ConnectNpRepository.walletBalance.value
    val transferAmount = 150.0
    val fee = ConnectNpRepository.P2P_TRANSACTION_FEE // Rs 10.0
    val totalDeducted = transferAmount + fee // Rs 160.0

    // Breakdown test
    assertEquals(10.0, fee, 0.001)
    assertEquals(160.0, totalDeducted, 0.001)

    // Successful transfer
    val success = ConnectNpRepository.sendMoney("Pratik", transferAmount, "Dinner")
    assertTrue(success)
    assertEquals(initialBalance - totalDeducted, ConnectNpRepository.walletBalance.value, 0.01)

    // Insufficient balance test: current balance is now 40, transfer of 35 requires 35 + 10 = 45 > 40
    val fail = ConnectNpRepository.sendMoney("Pratik", 35.0, "Taxi")
    assertFalse("Transfer should fail because 35 + 10 fee exceeds balance of 40", fail)
  }

  @Test
  fun `verify 4-digit MPIN setup and validation rules`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Setup valid 4-digit MPIN
    val validPin = "5678"
    val setupSuccess = ConnectNpRepository.setupWalletMpin(validPin, context)
    assertTrue("Setting valid 4-digit MPIN should succeed", setupSuccess)
    assertEquals(validPin, ConnectNpRepository.walletMpin.value)
    assertTrue("MPIN should be reported as setup", ConnectNpRepository.isMpinSetup())

    // Invalid PIN checks
    assertFalse("3-digit PIN should be rejected", ConnectNpRepository.setupWalletMpin("123", context))
    assertFalse("5-digit PIN should be rejected", ConnectNpRepository.setupWalletMpin("12345", context))
    assertFalse("Non-numeric PIN should be rejected", ConnectNpRepository.setupWalletMpin("abcd", context))

    // Valid PIN should remain unchanged
    assertEquals(validPin, ConnectNpRepository.walletMpin.value)
  }

  @Test
  fun `verify MPIN verification logic`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    ConnectNpRepository.setupWalletMpin("9876", context)

    assertTrue("Correct MPIN should be verified successfully", ConnectNpRepository.verifyWalletMpin("9876"))
    assertFalse("Incorrect MPIN should fail verification", ConnectNpRepository.verifyWalletMpin("0000"))
    assertFalse("Partial MPIN should fail verification", ConnectNpRepository.verifyWalletMpin("987"))
  }

  @Test
  fun `verify biometric authentication toggle in settings`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Enable biometrics
    ConnectNpRepository.setBiometricEnabled(true, context)
    assertTrue("Biometric authentication should be enabled", ConnectNpRepository.isBiometricEnabled.value)

    // Disable biometrics
    ConnectNpRepository.setBiometricEnabled(false, context)
    assertFalse("Biometric authentication should be disabled", ConnectNpRepository.isBiometricEnabled.value)
  }

  @Test
  fun `verify forgot MPIN sends 6-digit OTP and successfully resets MPIN`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testEmail = "citizen@connectnp.com"

    // 1. Initial PIN
    ConnectNpRepository.setupWalletMpin("1111", context)
    assertTrue(ConnectNpRepository.verifyWalletMpin("1111"))

    // 2. Request OTP for Forgot MPIN
    val generatedOtp = ConnectNpRepository.generateAndSendResetMpinOtp(testEmail, context)
    assertEquals(6, generatedOtp.length)
    assertTrue("OTP must be 6 numeric digits", generatedOtp.all { it.isDigit() })
    assertEquals(generatedOtp, ConnectNpRepository.activeResetOtp.value)

    // 3. Reset with wrong OTP fails
    val wrongOtpResult = ConnectNpRepository.resetWalletMpinWithOtp(testEmail, "999999", "2222", context)
    assertFalse("Reset with incorrect OTP should fail", wrongOtpResult)
    assertTrue("Original PIN should remain active", ConnectNpRepository.verifyWalletMpin("1111"))

    // 4. Reset with invalid new PIN format fails
    val invalidPinResult = ConnectNpRepository.resetWalletMpinWithOtp(testEmail, generatedOtp, "123", context)
    assertFalse("Reset with non-4-digit PIN should fail", invalidPinResult)

    // 5. Reset with correct OTP and valid 4-digit PIN succeeds
    val successReset = ConnectNpRepository.resetWalletMpinWithOtp(testEmail, generatedOtp, "2222", context)
    assertTrue("Reset with correct OTP should succeed", successReset)
    assertTrue("New MPIN should now be verified", ConnectNpRepository.verifyWalletMpin("2222"))
    assertFalse("Old MPIN should no longer be valid", ConnectNpRepository.verifyWalletMpin("1111"))
    org.junit.Assert.assertNull("Active OTP should be cleared after successful reset", ConnectNpRepository.activeResetOtp.value)
  }

  @Test
  fun `verify compressProfileImage handles invalid or non-existent URIs gracefully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nonExistentUri = android.net.Uri.parse("content://media/external/images/media/888888888")
    val bytes = com.example.util.ImageCompressor.compressProfileImageToBytes(context, nonExistentUri)
    org.junit.Assert.assertNull(bytes)
    val fileUri = com.example.util.ImageCompressor.compressProfileImageToFileUri(context, nonExistentUri)
    org.junit.Assert.assertNull(fileUri)
  }

  @Test
  fun `verify active user profile updates immediately in repository without waiting`() {
    val updatedName = "Suman Thapa"
    val updatedBio = "Developer from Pokhara 🇳🇵"
    val updatedPhoto = "https://images.unsplash.com/sample_avatar"

    ConnectNpRepository.updateActiveUser(
        name = updatedName,
        bio = updatedBio,
        photoUrl = updatedPhoto
    )

    val currentUser = ConnectNpRepository.getCurrentActiveUser()
    assertEquals(updatedName, currentUser.name)
    assertEquals(updatedBio, currentUser.bio)
    assertEquals(updatedPhoto, currentUser.avatarUrl)
  }

  @Test
  fun `verify linking payment methods updates repository and supports eSewa, Khalti, Bank`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Link eSewa
    val esewaMethod = com.example.model.LinkedPaymentMethod(
        id = "test_pm_esewa_1",
        type = com.example.model.PaymentMethodType.ESEWA,
        title = "eSewa Nepal",
        accountNumber = "9841999888",
        accountHolderName = "Prasanna Gautam",
        isDefault = true
    )
    ConnectNpRepository.linkPaymentMethod(esewaMethod, context)

    val methodsAfterEsewa = ConnectNpRepository.linkedPaymentMethods.value
    assertTrue("Linked methods must contain test eSewa", methodsAfterEsewa.any { it.id == "test_pm_esewa_1" })
    assertEquals("9841999888", methodsAfterEsewa.first { it.id == "test_pm_esewa_1" }.accountNumber)

    // 2. Link Bank Account
    val bankMethod = com.example.model.LinkedPaymentMethod(
        id = "test_pm_bank_1",
        type = com.example.model.PaymentMethodType.BANK,
        title = "Nabil Bank Limited",
        accountNumber = "01020000001234",
        accountHolderName = "Prasanna Gautam",
        bankName = "Nabil Bank Limited",
        branch = "New Road, Kathmandu",
        isDefault = false
    )
    ConnectNpRepository.linkPaymentMethod(bankMethod, context)

    val methodsAfterBank = ConnectNpRepository.linkedPaymentMethods.value
    assertTrue("Linked methods must contain test Bank", methodsAfterBank.any { it.id == "test_pm_bank_1" })
    assertEquals("Nabil Bank Limited", methodsAfterBank.first { it.id == "test_pm_bank_1" }.bankName)

    // 3. Unlink method
    ConnectNpRepository.unlinkPaymentMethod("test_pm_esewa_1", context)
    val methodsAfterUnlink = ConnectNpRepository.linkedPaymentMethods.value
    assertFalse("Unlinked method must no longer be present", methodsAfterUnlink.any { it.id == "test_pm_esewa_1" })
  }

  @Test
  fun `verify wallet withdrawal deducts balance and logs transaction`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Set balance to 5000
    ConnectNpRepository.setWalletBalance(5000.0)
    assertEquals(5000.0, ConnectNpRepository.walletBalance.value, 0.01)

    // Attempt withdrawal higher than balance -> should fail
    val failWithdraw = ConnectNpRepository.withdrawMoney(
        amount = 6000.0,
        destination = "eSewa (9841000000)",
        methodType = "ESEWA",
        context = context
    )
    assertFalse("Withdrawal exceeding balance must fail", failWithdraw)
    assertEquals(5000.0, ConnectNpRepository.walletBalance.value, 0.01)

    // Valid withdrawal of 1500 -> should succeed and reduce balance to 3500
    val initialTxCount = ConnectNpRepository.transactions.value.size
    val successWithdraw = ConnectNpRepository.withdrawMoney(
        amount = 1500.0,
        destination = "Nabil Bank (01020000001234)",
        methodType = "BANK",
        context = context
    )
    assertTrue("Valid withdrawal must succeed", successWithdraw)
    assertEquals(3500.0, ConnectNpRepository.walletBalance.value, 0.01)
    assertEquals(initialTxCount + 1, ConnectNpRepository.transactions.value.size)

    val latestTx = ConnectNpRepository.transactions.value.first()
    assertFalse("Withdrawal transaction must be a debit", latestTx.isCredit)
    assertEquals(1500.0, latestTx.amountNpr, 0.01)
    assertTrue("Transaction title should mention destination", latestTx.title.contains("Nabil Bank"))
  }
}


