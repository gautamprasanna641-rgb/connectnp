package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.PoppinsFontFamily
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Exact Web Client ID configured in Google Cloud Console.
 * Used for both requestIdToken (GoogleSignInOptions) and serverClientId (CredentialManager).
 */
const val WEB_CLIENT_ID = "147110426467-euo2aqf3lrs9upit1n49va75h3a48ij.apps.googleusercontent.com"

fun getResolvedWebClientId(context: Context): String {
    return try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val resVal = context.getString(resId)
            if (resVal.isNotBlank()) resVal else WEB_CLIENT_ID
        } else {
            WEB_CLIENT_ID
        }
    } catch (e: Exception) {
        WEB_CLIENT_ID
    }
}

/**
 * Builds GoogleSignInOptions configured with requestIdToken using the exact Web Client ID.
 */
fun getGoogleSignInOptions(context: Context): GoogleSignInOptions {
    val clientId = getResolvedWebClientId(context)
    return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(clientId)
        .requestEmail()
        .build()
}

/**
 * Creates GoogleSignInClient for launching the Google Sign-In intent.
 */
fun getGoogleSignInClient(context: Context): GoogleSignInClient {
    return GoogleSignIn.getClient(context, getGoogleSignInOptions(context))
}

/**
 * Silent / auto-sign-in attempt for already authenticated users.
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }

    val clientId = getResolvedWebClientId(context)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Handles signing in: first attempts Credential Manager with serverClientId;
 * if Credential Manager is unsupported or fails, falls back directly to the Google Sign-In Intent.
 */
fun performGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit,
    scope: CoroutineScope
) {
    val clientId = getResolvedWebClientId(context)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activity = context as? Activity
            if (activity != null) {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                    return@launch
                }
            }
            // If credential was not resolved via CredentialManager, fallback to Intent
            val client = getGoogleSignInClient(context)
            googleSignInLauncher.launch(client.signInIntent)
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Credential Manager sign-in cancelled: ${e.message}")
            onAuthCancelled()
        } catch (e: GetCredentialException) {
            Log.i("Auth", "Credential Manager unavailable or failed (${e.message}); falling back to Google Sign-In Intent")
            try {
                val client = getGoogleSignInClient(context)
                googleSignInLauncher.launch(client.signInIntent)
            } catch (intentErr: Exception) {
                Log.e("Auth", "Failed to launch Google Sign-In Intent", intentErr)
                onAuthError("Failed to start Google Sign-In: ${intentErr.localizedMessage ?: intentErr.message}")
            }
        } catch (e: Exception) {
            Log.i("Auth", "Credential Manager encountered exception, launching Google Sign-In Intent: ${e.message}")
            try {
                val client = getGoogleSignInClient(context)
                googleSignInLauncher.launch(client.signInIntent)
            } catch (intentErr: Exception) {
                Log.e("Auth", "Failed to start Google Sign-In intent", intentErr)
                onAuthError(intentErr.localizedMessage ?: "Sign in failed")
            }
        }
    }
}

/**
 * Complete sign out clearing Firebase, GoogleSignInClient, and CredentialManager state.
 */
fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    try {
        Firebase.auth.signOut()
    } catch (e: Exception) {
        Log.e("Auth", "Error signing out of Firebase", e)
    }
    try {
        getGoogleSignInClient(context).signOut()
    } catch (e: Exception) {
        Log.e("Auth", "Error signing out of GoogleSignInClient", e)
    }
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun GoogleSignInScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Register ActivityResultLauncher to handle the Google Sign-In intent properly
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account?.idToken
                    if (!idToken.isNullOrEmpty()) {
                        coroutineScope.launch {
                            try {
                                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                                Firebase.auth.signInWithCredential(authCredential).await()
                                isLoading = false
                                onAuthSuccess()
                            } catch (e: Exception) {
                                Log.e("Auth", "Firebase auth failed with token", e)
                                isLoading = false
                                errorMessage = "Firebase Sign-In failed: ${e.localizedMessage ?: e.message}"
                            }
                        }
                    } else {
                        isLoading = false
                        errorMessage = "Failed to retrieve Google ID token. Please verify Web Client ID."
                    }
                } catch (e: ApiException) {
                    isLoading = false
                    Log.e("Auth", "Google Sign-In Intent ApiException: ${e.statusCode}", e)
                    val msg = when (e.statusCode) {
                        12500 -> "Sign-in failed (Error 12500). Please verify SHA-1 fingerprint and Web Client ID."
                        12501 -> null // User cancelled selection
                        12502 -> "Sign-in already in progress."
                        CommonStatusCodes.NETWORK_ERROR, 7 -> "Network error. Please check your internet connection."
                        else -> "Google Sign-In error (code ${e.statusCode}): ${e.localizedMessage ?: e.message}"
                    }
                    if (msg != null) {
                        errorMessage = msg
                    }
                } catch (e: Exception) {
                    isLoading = false
                    Log.e("Auth", "Unexpected error handling Google Sign-In intent", e)
                    errorMessage = e.localizedMessage ?: "Unexpected error during Google Sign-In"
                }
            } else {
                isLoading = false
                errorMessage = "No data returned from Google Sign-In"
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            isLoading = false
            Log.d("Auth", "Google Sign-In was cancelled by user")
        } else {
            isLoading = false
            errorMessage = "Google Sign-In failed with result code: ${result.resultCode}"
        }
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0F1D),
                        Color(0xFF0F1B35),
                        Color(0xFF05070D)
                    )
                )
            )
            .testTag("google_sign_in_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.connectnp_icon_full),
                        contentDescription = "ConnectNP Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CONNECTNP",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = ConnectNpBlue,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Connect. Share. Belong. Earn.",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text(
                    text = "Sign in to synchronize your posts, messaging, marketplace, and ConnectNP Pay wallet securely with Firebase Cloud Firestore.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        performGoogleSignIn(
                            context = context,
                            credentialManager = credentialManager,
                            googleSignInLauncher = googleSignInLauncher,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            onAuthCancelled = {
                                isLoading = false
                            },
                            scope = coroutineScope
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ConnectNpBlue,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Zero-trust verified • End-to-end encrypted 🇳🇵",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
