package com.example.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.ConnectNpFirestoreRepository
import com.example.data.ConnectNpRepository
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.PoppinsFontFamily
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Clean sign out function clearing Firebase and credential state.
 */
fun signOutUser(
    context: Context,
    credentialManager: CredentialManager? = null,
    onSignOutComplete: () -> Unit = {},
    scope: CoroutineScope
) {
    try {
        Firebase.auth.signOut()
    } catch (e: Exception) {
        Log.e("Auth", "Error signing out of Firebase", e)
    }
    scope.launch {
        try {
            credentialManager?.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.d("Auth", "Credential state cleanup ignored: ${e.message}")
        } finally {
            onSignOutComplete()
        }
    }
}

/**
 * Backwards compatibility alias for existing callers.
 */
@Composable
fun GoogleSignInScreen(
    onAuthSuccess: () -> Unit
) {
    AuthScreen(onAuthSuccess = onAuthSuccess)
}

enum class AuthMode {
    LOGIN,
    REGISTER
}

/**
 * Clean Email/Password Authentication Screen for CONNECTNP.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070B14),
                        Color(0xFF0F1B35),
                        Color(0xFF05070D)
                    )
                )
            )
            .testTag("auth_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Branding Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, ConnectNpBlue.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.connectnp_icon_full),
                    contentDescription = "CONNECTNP Emblem",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CONNECTNP",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Text(
                text = "CONNECT. SHARE. BELONG. EARN.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = ConnectNpBlue,
                letterSpacing = 1.6.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tab Selector: Login vs Register
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    // Sign In Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (authMode == AuthMode.LOGIN) ConnectNpBlue else Color.Transparent
                            )
                            .clickable { authMode = AuthMode.LOGIN }
                            .testTag("login_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (authMode == AuthMode.LOGIN) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Register Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (authMode == AuthMode.REGISTER) ConnectNpBlue else Color.Transparent
                            )
                            .clickable { authMode = AuthMode.REGISTER }
                            .testTag("register_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Account",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (authMode == AuthMode.REGISTER) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Form Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    if (authMode == AuthMode.LOGIN) {
                        LoginContent(
                            onAuthSuccess = onAuthSuccess,
                            onForgotPasswordClick = { showForgotPasswordDialog = true },
                            onSwitchToRegister = { authMode = AuthMode.REGISTER }
                        )
                    } else {
                        RegisterContent(
                            onAuthSuccess = onAuthSuccess,
                            onSwitchToLogin = { authMode = AuthMode.LOGIN }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer note
            Text(
                text = "Protected by End-to-End Encryption & Secure Sign-In 🇳🇵",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPasswordDialog = false }
        )
    }
}

/**
 * Clean Login Screen Content
 */
@Composable
private fun LoginContent(
    onAuthSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Welcome Back",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Sign in to your CONNECTNP account",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Error message banner
        if (errorMessage != null) {
            Surface(
                color = NepalCrimson.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NepalCrimson.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = NepalCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email Address") },
            placeholder = { Text("example@domain.com") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = ConnectNpBlue)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            placeholder = { Text("••••••••") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ConnectNpBlue)
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Forgot Password link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onForgotPasswordClick,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier.testTag("login_forgot_password_button")
            ) {
                Text(
                    text = "Forgot Password?",
                    fontSize = 12.sp,
                    color = ConnectNpBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sign In Button
        Button(
            onClick = {
                val cleanEmail = email.trim()
                if (cleanEmail.isEmpty()) {
                    errorMessage = "Please enter your email address"
                    return@Button
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                    errorMessage = "Please enter a valid email address"
                    return@Button
                }
                if (password.isEmpty()) {
                    errorMessage = "Please enter your password"
                    return@Button
                }

                isLoading = true
                errorMessage = null
                focusManager.clearFocus()

                coroutineScope.launch {
                    try {
                        Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
                        Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                        onAuthSuccess()
                    } catch (e: Exception) {
                        Log.e("Auth", "Login error: ${e.message}", e)
                        errorMessage = formatAuthError(e)
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("login_submit_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Signing In...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            } else {
                Text(
                    text = "Sign In",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Switch to Register
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account?",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Sign Up",
                fontSize = 12.sp,
                color = ConnectNpBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSwitchToRegister() }
                    .padding(4.dp)
                    .testTag("switch_to_register_button")
            )
        }
    }
}

/**
 * Clean Registration Screen Content
 */
@Composable
private fun RegisterContent(
    onAuthSuccess: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Create Account",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Join CONNECTNP community and start sharing",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Error message banner
        if (errorMessage != null) {
            Surface(
                color = NepalCrimson.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NepalCrimson.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = NepalCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Full Name Field
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = null
            },
            label = { Text("Full Name") },
            placeholder = { Text("e.g. Ram Sharma") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = ConnectNpBlue)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_name_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email Address") },
            placeholder = { Text("example@domain.com") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = ConnectNpBlue)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_email_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password (min 6 characters)") },
            placeholder = { Text("••••••••") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ConnectNpBlue)
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_password_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Confirm Password Field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            label = { Text("Confirm Password") },
            placeholder = { Text("••••••••") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ConnectNpBlue)
            },
            trailingIcon = {
                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                    Icon(
                        imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_confirm_password_input")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Create Account Button
        Button(
            onClick = {
                val cleanName = name.trim()
                val cleanEmail = email.trim()

                if (cleanName.isEmpty()) {
                    errorMessage = "Please enter your full name"
                    return@Button
                }
                if (cleanEmail.isEmpty()) {
                    errorMessage = "Please enter your email address"
                    return@Button
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                    errorMessage = "Please enter a valid email address"
                    return@Button
                }
                if (password.length < 6) {
                    errorMessage = "Password must be at least 6 characters"
                    return@Button
                }
                if (password != confirmPassword) {
                    errorMessage = "Passwords do not match"
                    return@Button
                }

                isLoading = true
                errorMessage = null
                focusManager.clearFocus()

                coroutineScope.launch {
                    try {
                        val authResult = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                        val user = authResult.user

                        // Update display name in Firebase Auth
                        user?.let {
                            try {
                                val profileUpdate = UserProfileChangeRequest.Builder()
                                    .setDisplayName(cleanName)
                                    .build()
                                it.updateProfile(profileUpdate).await()
                            } catch (e: Exception) {
                                Log.w("Auth", "Failed to set display name in Auth: ${e.message}")
                            }

                            // Initialize user document in Cloud Firestore
                            try {
                                val cleanHandle = "@" + cleanName.lowercase().replace("[^a-z0-9_]".toRegex(), "") + "_" + (100..999).random()
                                ConnectNpFirestoreRepository(context).saveUserProfile(
                                    name = cleanName,
                                    handle = cleanHandle,
                                    bio = "Namaste! CONNECTNP Member 🇳🇵",
                                    walletBalance = 100L
                                )
                            } catch (e: Exception) {
                                Log.w("Auth", "Failed to save initial Firestore profile: ${e.message}")
                            }

                            // Send verification email to new user immediately (Requirement #1)
                            try {
                                it.sendEmailVerification().await()
                                Log.d("Auth", "Verification email sent to $cleanEmail")
                            } catch (e: Exception) {
                                Log.w("Auth", "Failed to send verification email: ${e.message}")
                            }
                        }

                        Toast.makeText(context, "Account created! Verification email sent to $cleanEmail", Toast.LENGTH_LONG).show()
                        onAuthSuccess()
                    } catch (e: Exception) {
                        Log.e("Auth", "Registration error: ${e.message}", e)
                        errorMessage = formatAuthError(e)
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("register_submit_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Creating Account...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            } else {
                Text(
                    text = "Create Account",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Switch to Login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account?",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Sign In",
                fontSize = 12.sp,
                color = ConnectNpBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSwitchToLogin() }
                    .padding(4.dp)
                    .testTag("switch_to_login_button")
            )
        }
    }
}

/**
 * Clean Password Reset Dialog
 */
@Composable
private fun ForgotPasswordDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var resetEmail by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reset Password",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your registered email address and we'll send you a password reset link.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (statusMessage != null) {
                    Surface(
                        color = if (isSuccess) Color(0xFF2E7D32).copy(alpha = 0.12f) else NepalCrimson.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 12.sp,
                            color = if (isSuccess) Color(0xFF2E7D32) else NepalCrimson,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (!isSuccess) {
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            statusMessage = null
                        },
                        label = { Text("Email Address") },
                        placeholder = { Text("example@domain.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = ConnectNpBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val cleanEmail = resetEmail.trim()
                            if (cleanEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                                statusMessage = "Please enter a valid email address"
                                isSuccess = false
                                return@Button
                            }

                            isSending = true
                            statusMessage = null
                            coroutineScope.launch {
                                try {
                                    Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
                                    isSuccess = true
                                    statusMessage = "Password reset email sent! Please check your inbox or spam folder."
                                } catch (e: Exception) {
                                    isSuccess = false
                                    statusMessage = formatAuthError(e)
                                } finally {
                                    isSending = false
                                }
                            }
                        },
                        enabled = !isSending,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending...", color = Color.White)
                        } else {
                            Text("Send Reset Link", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Format Firebase Auth errors into clear, human-readable explanations.
 */
private fun formatAuthError(exception: Exception): String {
    return when (exception) {
        is FirebaseAuthInvalidUserException -> "No account found with this email. Please check your spelling or create an account."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password. Please try again."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please sign in instead."
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
        is FirebaseAuthException -> exception.localizedMessage ?: "Authentication failed (${exception.errorCode})"
        else -> exception.localizedMessage ?: "An error occurred during authentication. Please check your connection."
    }
}
