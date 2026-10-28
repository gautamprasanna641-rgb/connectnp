package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.PoppinsFontFamily
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Screen displayed when a user's email is unverified, blocking access to the Feed
 * until email verification is confirmed.
 */
@Composable
fun EmailVerificationScreen(
    user: FirebaseUser?,
    isNepali: Boolean = false,
    onEmailVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isCheckingStatus by remember { mutableStateOf(false) }
    var isResendingEmail by remember { mutableStateOf(false) }
    var resendCooldownSec by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    val userEmail = user?.email ?: "your email address"

    // Cooldown countdown timer for resending email
    LaunchedEffect(resendCooldownSec) {
        if (resendCooldownSec > 0) {
            delay(1000L)
            resendCooldownSec -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("email_verification_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ConnectNpBlue.copy(alpha = 0.2f),
                                ConnectNpBlue.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(2.dp, ConnectNpBlue.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = "Unverified Email",
                    tint = ConnectNpBlue,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = if (isNepali) "इमेल प्रमाणीकरण आवश्यक छ" else "Verify Your Email",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = if (isNepali)
                    "फिड तथा समुदायका पोस्टहरू हेर्नका लागि तपाईंको इमेल प्रमाणीकरण गर्नुपर्नेछ।"
                else
                    "Access to the CONNECTNP Feed is locked until your email address is verified.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Email Display Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = ConnectNpBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = userEmail,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isNepali) "फिड पहुँच लक गरिएको छ" else "Feed Access Locked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status or Info Message
            AnimatedVisibility(visible = statusMessage != null) {
                Surface(
                    color = if (isStatusError) NepalCrimson.copy(alpha = 0.12f) else ConnectNpBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isStatusError) NepalCrimson.copy(alpha = 0.4f) else ConnectNpBlue.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isStatusError) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isStatusError) NepalCrimson else ConnectNpBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Instructions text
            Text(
                text = if (isNepali)
                    "कृपया तपाईंको इनबक्स वा स्पाम (Spam) फोल्डर जाँच गर्नुहोस् र पठाइएको लिङ्कमा क्लिक गर्नुहोस्।"
                else
                    "Please check your inbox or spam folder for the verification link sent by CONNECTNP.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action 1: "I've Verified / Refresh Status"
            Button(
                onClick = {
                    isCheckingStatus = true
                    statusMessage = null
                    coroutineScope.launch {
                        try {
                            val currentUser = Firebase.auth.currentUser
                            currentUser?.reload()?.await()
                            if (currentUser?.isEmailVerified == true) {
                                Toast.makeText(
                                    context,
                                    if (isNepali) "इमेल सफलतापूर्वक प्रमाणीकरण भयो! फिडमा स्वागत छ 🇳🇵"
                                    else "Email verified! Welcome to CONNECTNP Feed 🇳🇵",
                                    Toast.LENGTH_LONG
                                ).show()
                                onEmailVerified()
                            } else {
                                isStatusError = true
                                statusMessage = if (isNepali)
                                    "इमेल अझै प्रमाणीकरण भएको छैन। कृपया इनबक्समा रहेको लिङ्कमा क्लिक गर्नुहोस्।"
                                else
                                    "Your email is not verified yet. Please click the link in your email, then tap below."
                            }
                        } catch (e: Exception) {
                            isStatusError = true
                            statusMessage = "Could not refresh verification status: ${e.message}"
                        } finally {
                            isCheckingStatus = false
                        }
                    }
                },
                enabled = !isCheckingStatus && !isResendingEmail,
                colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("check_verification_status_button")
            ) {
                if (isCheckingStatus) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking Status...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isNepali) "मैले प्रमाणीकरण गरिसकेँ (फिड खोल्नुहोस्)" else "I've Verified (Unlock Feed)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 2: "Resend Email" Button (Requirement #3)
            OutlinedButton(
                onClick = {
                    isResendingEmail = true
                    statusMessage = null
                    coroutineScope.launch {
                        try {
                            val currentUser = Firebase.auth.currentUser
                            currentUser?.sendEmailVerification()?.await()
                            resendCooldownSec = 60
                            isStatusError = false
                            statusMessage = if (isNepali)
                                "प्रमाणीकरण इमेल पुनः पठाइयो! कृपया तपाईंको इनबक्स जाँच गर्नुहोस्।"
                            else
                                "Verification email resent to $userEmail. Please check your inbox and spam folder."
                            Toast.makeText(
                                context,
                                if (isNepali) "इमेल पुनः पठाइयो!" else "Verification email resent!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            isStatusError = true
                            statusMessage = "Failed to resend verification email: ${e.message}"
                        } finally {
                            isResendingEmail = false
                        }
                    }
                },
                enabled = !isResendingEmail && !isCheckingStatus && resendCooldownSec == 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("resend_verification_email_button")
            ) {
                if (isResendingEmail) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending Email...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (resendCooldownSec > 0) {
                            if (isNepali) "पुनः पठाउनुहोस् (${resendCooldownSec} सेकेन्ड)"
                            else "Resend Email (${resendCooldownSec}s)"
                        } else {
                            if (isNepali) "प्रमाणीकरण इमेल पुनः पठाउनुहोस्"
                            else "Resend Verification Email"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action 3: Sign out / switch account
            TextButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("verification_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = NepalCrimson,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isNepali) "बाहिरिनुहोस् / अर्को खाता प्रयोग गर्नुहोस्" else "Sign Out / Use Another Account",
                    color = NepalCrimson,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
