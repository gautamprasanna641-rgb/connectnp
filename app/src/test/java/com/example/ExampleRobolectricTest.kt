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
@Config(sdk = [36])
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
  fun `verify wallet transaction`() {
    val currentBalance = ConnectNpRepository.walletBalance.value
    val success = ConnectNpRepository.sendMoney("Aayush Sharma", 100.0, "Test Payment")
    assertTrue(success)
    assertEquals(currentBalance - 100.0, ConnectNpRepository.walletBalance.value, 0.01)
  }
}
