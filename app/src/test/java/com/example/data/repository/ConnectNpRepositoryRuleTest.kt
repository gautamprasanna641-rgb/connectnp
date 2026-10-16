package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.ConnectNpFirestoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ConnectNpRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveUserProfile_validPayload_createsDocumentAndReadsIt() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = ConnectNpFirestoreRepository(firestore)

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveUserProfile(
        name = "Prasanna Gautam",
        handle = "@prasanna_np",
        bio = "Namaste Nepal 🇳🇵",
        walletBalance = 14850L
      )
    }
    assertTrue("saveUserProfile should succeed", saveResult.isSuccess)

    val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.observeUserProfile(uid).first { it != null }
    }
    assertNotNull(profile)
    assertEquals(uid, profile?.userId)
    assertEquals("Prasanna Gautam", profile?.name)
    assertEquals(14850L, profile?.walletBalance)
  }

  @Test
  fun createPost_validPayload_createsDocumentAndReturnsId() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = ConnectNpFirestoreRepository(firestore)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createPost(
        content = "Connecting Nepal with ConnectNP!",
        location = "Kathmandu",
        authorName = "Prasanna Gautam",
        authorHandle = "@prasanna_np"
      )
    }
    assertTrue("createPost should succeed", createResult.isSuccess)
    val postId = createResult.getOrThrow()
    assertTrue(postId.isNotEmpty())
  }

  @Test
  fun observeUserPosts_authenticatedOwner_emitsPosts() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = ConnectNpFirestoreRepository(firestore)

    val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createPost(
        content = "Stunning morning in Pokhara Lake 🏔️",
        location = "Pokhara",
        authorName = "Prasanna Gautam",
        authorHandle = "@prasanna_np"
      ).getOrThrow()
    }

    val emittedPosts = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserPosts(uid).first { list -> list.any { it.id == postId } }
    }
    assertTrue(emittedPosts.any { it.id == postId })
  }

  @Test
  fun observeUserPosts_unauthenticated_failsWithIllegalStateOrPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = ConnectNpFirestoreRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserPosts().first()
      }
      fail("Expected exception for unauthenticated call")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("User must be signed in") == true)
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }


  @Test
  fun addTransaction_authenticatedOwner_createsLedgerEntry() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = ConnectNpFirestoreRepository(firestore)

    val txResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.addTransaction(
        title = "Top-up via Khalti",
        amount = 5000L,
        type = "credit"
      )
    }
    assertTrue("addTransaction should succeed", txResult.isSuccess)

    val txs = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTransactions(uid).first { list -> list.isNotEmpty() }
    }
    assertTrue(txs.any { it.title == "Top-up via Khalti" })
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
