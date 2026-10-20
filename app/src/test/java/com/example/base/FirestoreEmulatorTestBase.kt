package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

  protected lateinit var firestore: FirebaseFirestore
  protected lateinit var auth: FirebaseAuth
  protected lateinit var databaseId: String

  @Before
  open fun setUpFirebase() {
    val isEmulatorRunning = try {
      java.net.Socket().use { socket ->
        socket.connect(java.net.InetSocketAddress(EMULATOR_HOST, FIRESTORE_PORT), 300)
        true
      }
    } catch (e: Exception) {
      false
    }
    org.junit.Assume.assumeTrue("Firebase Firestore Emulator is not running on $EMULATOR_HOST:$FIRESTORE_PORT", isEmulatorRunning)

    val context = ApplicationProvider.getApplicationContext<Context>()
    databaseId = try {
      context.getString(com.example.R.string.firestore_database_id)
    } catch (e: Exception) {
      "ai-studio-android-connectn-709620ff-2875-40a5-af18-fbe92d0d50ec"
    }

    val app = if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setApplicationId("com.example.app")
          .setProjectId(PROJECT_ID)
          .setApiKey("fake-api-key-for-emulator")
          .build()
      )
    } else {
      FirebaseApp.getInstance()
    }

    val f = FirebaseFirestore.getInstance(app, databaseId)
    try {
      f.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
      f.firestoreSettings = FirebaseFirestoreSettings.Builder()
        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
        .build()
    } catch (e: IllegalStateException) {
      // useEmulator can only be called once per instance
    }

    val a = FirebaseAuth.getInstance(app)
    try {
      a.useEmulator(EMULATOR_HOST, AUTH_PORT)
    } catch (e: IllegalStateException) {
      // useEmulator can only be called once per instance
    }

    firestore = f
    auth = a
  }

  @After
  open fun tearDownFirebase() {
    if (::auth.isInitialized) {
      auth.signOut()
    }
  }

  protected suspend fun signInTestUser(email: String): String = withContext(Dispatchers.IO) {
    withTimeout(AUTH_TIMEOUT_MS) {
      val result = try {
        auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
      } catch (unused: FirebaseAuthInvalidUserException) {
        auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
      }
      checkNotNull(result.user?.uid) { "User auth failed" }
    }
  }

  protected companion object {
    const val EMULATOR_HOST = "127.0.0.1"
    const val FIRESTORE_PORT = 8085
    const val AUTH_PORT = 9099
    const val PROJECT_ID = "demo-no-project"
    const val DEFAULT_PASSWORD = "password123"
    const val AUTH_TIMEOUT_MS = 5000L
  }
}
