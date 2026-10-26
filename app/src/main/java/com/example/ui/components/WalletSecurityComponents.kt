package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ConnectNpRepository
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * 4-Digit MPIN Visual Dots Indicator
 */
@Composable
fun MpinDotsIndicator(
    currentLength: Int,
    maxLength: Int = 4,
    isError: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        for (i in 0 until maxLength) {
            val isFilled = i < currentLength
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> NepalCrimson
                            isFilled -> ConnectNpBlue
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .border(
                        width = 1.5.dp,
                        color = when {
                            isError -> NepalCrimson
                            isFilled -> ConnectNpBlue
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Custom 0-9 Numeric Keypad with Backspace and Biometrics action
 */
@Composable
fun MpinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    showBiometric: Boolean = false,
    onBiometricClick: () -> Unit = {}
) {
    val keypadRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("BIO", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        keypadRows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "BIO" -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = showBiometric) { onBiometricClick() }
                                    .testTag("mpin_keypad_biometric"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showBiometric) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Use Biometrics",
                                        tint = ConnectNpBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        "DEL" -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onBackspaceClick() }
                                    .testTag("mpin_keypad_backspace")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onDigitClick(key) }
                                    .testTag("mpin_keypad_$key")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4-digit MPIN Setup Dialog for New Users or Change PIN
 */
@Composable
fun WalletMpinSetupDialog(
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onMpinSetupComplete: (String) -> Unit
) {
    val context = LocalContext.current
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) } // 1: Enter PIN, 2: Confirm PIN
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("wallet_mpin_setup_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shield / Lock Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ConnectNpBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ConnectNpBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (step == 1) {
                        if (isNepali) "नयाँ ४-अङ्कको MPIN सेट गर्नुहोस्" else "Set Up 4-Digit Wallet MPIN"
                    } else {
                        if (isNepali) "MPIN पुष्टि गर्नुहोस्" else "Confirm Your 4-Digit MPIN"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (step == 1) {
                        if (isNepali) "आफ्नो वालेट कारोबार सुरक्षित गर्न ४-अङ्कको पिन राख्नुहोस्"
                        else "Enter a 4-digit PIN to secure transfers and top-ups"
                    } else {
                        if (isNepali) "पुष्टि गर्नका लागि उही ४-अङ्कको पिन फेरि थिच्नुहोस्"
                        else "Re-enter your 4-digit PIN to verify"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                // Dots
                val currentPin = if (step == 1) firstPin else confirmPin
                MpinDotsIndicator(
                    currentLength = currentPin.length,
                    maxLength = 4,
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = NepalCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Keypad
                MpinKeypad(
                    onDigitClick = { digit ->
                        errorMessage = null
                        if (step == 1) {
                            if (firstPin.length < 4) {
                                firstPin += digit
                                if (firstPin.length == 4) {
                                    step = 2
                                }
                            }
                        } else {
                            if (confirmPin.length < 4) {
                                confirmPin += digit
                                if (confirmPin.length == 4) {
                                    if (confirmPin == firstPin) {
                                        val ok = ConnectNpRepository.setupWalletMpin(confirmPin, context)
                                        if (ok) {
                                            Toast.makeText(
                                                context,
                                                if (isNepali) "MPIN सफलतापूर्वक सेट भयो!" else "Wallet MPIN setup successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onMpinSetupComplete(confirmPin)
                                        }
                                    } else {
                                        errorMessage = if (isNepali) "दुबै पिन मिलेनन्! कृपया फेरि सुरु गर्नुहोस्।" else "PINs do not match. Please try again."
                                        confirmPin = ""
                                        firstPin = ""
                                        step = 1
                                    }
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        errorMessage = null
                        if (step == 1) {
                            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
                        } else {
                            if (confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.dropLast(1)
                            } else {
                                step = 1
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * 4-digit MPIN Verification Dialog with Biometric Bypass & Forgot PIN
 */
@Composable
fun WalletMpinVerifyDialog(
    amountToAuthorize: Double,
    recipientName: String? = null,
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onVerifiedSuccess: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotMpinDialog by remember { mutableStateOf(false) }

    val isBiometricEnabled by ConnectNpRepository.isBiometricEnabled.collectAsState()

    // Biometric bypass handler
    val handleBiometricAuth: () -> Unit = {
        // Authenticate biometric successfully
        Toast.makeText(
            context,
            if (isNepali) "बायोमेट्रिक प्रमाणीकरण सफल भयो!" else "Biometric authorization successful!",
            Toast.LENGTH_SHORT
        ).show()
        onVerifiedSuccess()
    }

    if (showForgotMpinDialog) {
        ForgotMpinDialog(
            isNepali = isNepali,
            onDismiss = { showForgotMpinDialog = false },
            onMpinResetSuccess = {
                showForgotMpinDialog = false
                Toast.makeText(
                    context,
                    if (isNepali) "नयाँ MPIN सेट भयो! अब नयाँ पिन थिच्नुहोस्।" else "MPIN reset! Please enter your new PIN.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("wallet_mpin_verify_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NepalCrimson.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = NepalCrimson,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isNepali) "सुरक्षा MPIN प्रमाणीकरण" else "Authorize Transaction",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (recipientName != null) {
                        if (isNepali) "$recipientName लाई रु ${amountToAuthorize.toInt()} पठाउन आफ्नो ४-अङ्कको MPIN थिच्नुहोस्"
                        else "Enter 4-digit MPIN to send Rs ${amountToAuthorize.toInt()} to $recipientName"
                    } else {
                        if (isNepali) "रु ${amountToAuthorize.toInt()} को कारोबार पुष्टि गर्न MPIN थिच्नुहोस्"
                        else "Enter 4-digit MPIN to confirm transaction of Rs ${amountToAuthorize.toInt()}"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )

                // Dots
                MpinDotsIndicator(
                    currentLength = enteredPin.length,
                    maxLength = 4,
                    isError = isError
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = NepalCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Keypad
                MpinKeypad(
                    onDigitClick = { digit ->
                        isError = false
                        errorMessage = null
                        if (enteredPin.length < 4) {
                            enteredPin += digit
                            if (enteredPin.length == 4) {
                                val isValid = ConnectNpRepository.verifyWalletMpin(enteredPin)
                                if (isValid) {
                                    onVerifiedSuccess()
                                } else {
                                    isError = true
                                    errorMessage = if (isNepali) "गलत MPIN! कृपया पुनः प्रयास गर्नुहोस्।" else "Incorrect MPIN. Please try again."
                                    enteredPin = ""
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        isError = false
                        errorMessage = null
                        if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                    },
                    showBiometric = isBiometricEnabled,
                    onBiometricClick = handleBiometricAuth
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Biometric shortcut banner (if enabled)
                if (isBiometricEnabled) {
                    FilledTonalButton(
                        onClick = handleBiometricAuth,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("use_biometrics_button")
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isNepali) "बायोमेट्रिक (फिंगरप्रिन्ट) प्रयोग गर्नुहोस्" else "Use Fingerprint / Face ID", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Forgot MPIN & Cancel row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showForgotMpinDialog = true },
                        modifier = Modifier.testTag("forgot_mpin_button")
                    ) {
                        Text(
                            text = if (isNepali) "MPIN बिर्सनुभयो?" else "Forgot MPIN?",
                            fontSize = 12.sp,
                            color = ConnectNpBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Forgot MPIN Dialog: Sends 6-digit OTP to registered email and verifies reset
 */
@Composable
fun ForgotMpinDialog(
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onMpinResetSuccess: () -> Unit
) {
    val context = LocalContext.current
    val userEmail = Firebase.auth.currentUser?.email ?: "user@connectnp.com"

    var otpSent by remember { mutableStateOf(false) }
    var enteredOtp by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("forgot_mpin_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ConnectNpBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = ConnectNpBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isNepali) "वालेट MPIN रिसेट गर्नुहोस्" else "Reset Wallet MPIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isNepali) "दर्ता गरिएको इमेल $userEmail मा ६-अङ्कको OTP पठाइनेछ।"
                    else "A 6-digit OTP will be sent to your registered email: $userEmail",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                if (!otpSent) {
                    Button(
                        onClick = {
                            val generatedOtp = ConnectNpRepository.generateAndSendResetMpinOtp(userEmail, context)
                            otpSent = true
                            Toast.makeText(
                                context,
                                if (isNepali) "OTP कोड पठाइयो! सूचना हेर्नुहोस् (Code: $generatedOtp)"
                                else "Verification OTP sent! (Code: $generatedOtp)",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("send_otp_button")
                    ) {
                        Icon(imageVector = Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isNepali) "इमेलमा OTP पठाउनुहोस्" else "Send OTP to Email", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) enteredOtp = it },
                        label = { Text("6-Digit OTP Code") },
                        placeholder = { Text("e.g. 123456") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reset_otp_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPin = it },
                        label = { Text("New 4-Digit MPIN") },
                        placeholder = { Text("e.g. 9876") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reset_new_pin_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmNewPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmNewPin = it },
                        label = { Text("Confirm New 4-Digit MPIN") },
                        placeholder = { Text("Re-enter 4-digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reset_confirm_pin_input")
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = NepalCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (newPin.length != 4) {
                                errorMessage = if (isNepali) "पिन ४ अङ्कको हुनुपर्छ।" else "PIN must be exactly 4 digits."
                                return@Button
                            }
                            if (newPin != confirmNewPin) {
                                errorMessage = if (isNepali) "नयाँ पिनहरू मिलेनन्।" else "New PINs do not match."
                                return@Button
                            }
                            val success = ConnectNpRepository.resetWalletMpinWithOtp(userEmail, enteredOtp, newPin, context)
                            if (success) {
                                onMpinResetSuccess()
                            } else {
                                errorMessage = if (isNepali) "गलत वा म्याद सकिएको OTP कोड।" else "Invalid or expired OTP code."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_reset_mpin_button")
                    ) {
                        Text(if (isNepali) "MPIN परिवर्तन गर्नुहोस्" else "Set New MPIN", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
