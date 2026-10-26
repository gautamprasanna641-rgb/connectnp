package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ConnectNpRepository
import com.example.model.WalletTransaction
import com.example.ui.components.ForgotMpinDialog
import com.example.ui.components.WalletMpinSetupDialog
import com.example.ui.components.WalletMpinVerifyDialog
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.ConnectNpSuccess
import com.example.ui.theme.NepalCrimson

@Composable
fun ConnectNpPayScreen(
    balance: Double,
    transactions: List<WalletTransaction>,
    isNepali: Boolean,
    isBalanceVisible: Boolean = true,
    onToggleBalanceVisibility: () -> Unit = {},
    onSendMoney: (recipient: String, amount: Double, remarks: String) -> Boolean,
    onTopUp: (amount: Double, source: String) -> Unit,
    onScanQr: () -> Unit
) {
    val isBiometricEnabled by ConnectNpRepository.isBiometricEnabled.collectAsState()

    var showSendDialog by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf(false) }
    var showMpinSetupDialog by remember { mutableStateOf(false) }
    var showForgotMpinDialog by remember { mutableStateOf(false) }

    var pendingSendTransfer by remember { mutableStateOf<Triple<String, Double, String>?>(null) }
    var pendingTopUp by remember { mutableStateOf<Pair<Double, String>?>(null) }
    var showVerifyDialogForSend by remember { mutableStateOf(false) }
    var showVerifyDialogForTopUp by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp)
            .testTag("pay_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Balance Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ConnectNpBlue, Color(0xFF0F3B82), NepalCrimson)
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNepali) "CONNECTNP पे वालेट" else "CONNECTNP PAY WALLET",
                            fontFamily = com.example.ui.theme.PoppinsFontFamily,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Instant P2P • Rs 10 Fee 🇳🇵",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val formattedAmount = if (balance <= 0.0) {
                            if (isNepali) "रु ०" else "Rs 0"
                        } else {
                            if (isNepali) "रु ${String.format("%,.2f", balance)}" else "Rs ${String.format("%,.2f", balance)}"
                        }
                        val displayBalance = if (isBalanceVisible) formattedAmount else (if (isNepali) "रु ••••••" else "Rs ••••••")

                        Text(
                            text = displayBalance,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.testTag("wallet_balance_display")
                        )

                        IconButton(
                            onClick = onToggleBalanceVisibility,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("wallet_balance_toggle_eye")
                        ) {
                            Icon(
                                imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isBalanceVisible) "Hide Wallet Balance" else "Show Wallet Balance",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (!isBalanceVisible) {
                            if (isNepali) "गोपनीयताका लागि मौज्दात लुकाइएको छ (हेर्न आँखा थिच्नुहोस्)" else "Balance hidden for privacy (tap eye icon to reveal)"
                        } else if (balance <= 0.0) {
                            if (isNepali) "तपाईंको वालेट मौज्दात रु ० छ" else "Your wallet balance is Rs 0"
                        } else {
                            "Linked with eSewa & Khalti • Protected by Escrow"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = if (balance <= 0.0) FontWeight.Medium else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3 Primary Actions: Send, Top Up, Scan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PayActionButton(
                            icon = Icons.Default.Send,
                            label = if (isNepali) "रकम पठाउनुहोस्" else "Send Money",
                            onClick = { showSendDialog = true }
                        )

                        PayActionButton(
                            icon = Icons.Default.AddCard,
                            label = if (isNepali) "टप-अप" else "Top-Up",
                            onClick = { showTopUpDialog = true }
                        )

                        PayActionButton(
                            icon = Icons.Default.QrCodeScanner,
                            label = if (isNepali) "QR स्क्यान" else "Scan & Pay",
                            onClick = onScanQr
                        )
                    }
                }
            }
        }

        // Status notification if any
        if (statusMessage != null) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ConnectNpSuccess.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage!!,
                        color = ConnectNpSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Wallet Security & MPIN Protection Card
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_security_status_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ConnectNpBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Security",
                                    tint = ConnectNpBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isNepali) "वालेट सुरक्षा (MPIN र बायोमेट्रिक)" else "Wallet Security & MPIN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isBiometricEnabled) "4-Digit MPIN • Biometric Bypass Active" else "4-Digit MPIN Protected",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ConnectNpSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "PROTECTED",
                                color = ConnectNpSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showMpinSetupDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("setup_mpin_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isNepali) "MPIN परिवर्तन" else "Change MPIN", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showForgotMpinDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("forgot_mpin_entry_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isNepali) "MPIN बिर्सनुभयो?" else "Forgot MPIN?", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Linked Gateways Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isNepali) "जोडिएका भुक्तानी माध्यमहरू" else "Linked Payment Methods",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    GatewayRow(name = "eSewa Nepal", id = "9841****** (Verified)", color = Color(0xFF60BB46))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    GatewayRow(name = "Khalti Digital Wallet", id = "9802****** (Active)", color = Color(0xFF5C2D91))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    GatewayRow(name = "FonePay & Nepal Clearing House (NCHL)", id = "CONNECTNP Direct IPS", color = ConnectNpBlue)
                }
            }
        }

        // Transaction History Header
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = if (isNepali) "हालैका कारोबारहरू (Recent Transactions)" else "Recent Transactions",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Transactions List or Empty State
        if (transactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("transactions_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isNepali) "कुनै कारोबार भएको छैन" else "No transactions yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isNepali) "तपाईंको वालेट मौज्दात रु ० छ।" else "Your wallet balance is Rs 0.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tx.isCredit) ConnectNpSuccess.copy(alpha = 0.15f)
                                    else NepalCrimson.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tx.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (tx.isCredit) ConnectNpSuccess else NepalCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tx.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "${tx.date} • ${tx.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = "${if (tx.isCredit) "+ " else "- "}रु ${tx.amountNpr.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (tx.isCredit) ConnectNpSuccess else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Send Money Dialog (Breakdown confirmation leads to MPIN verification)
    if (showSendDialog) {
        SendMoneyDialog(
            balance = balance,
            isNepali = isNepali,
            onDismiss = { showSendDialog = false },
            onConfirm = { recipient, amount, remarks ->
                pendingSendTransfer = Triple(recipient, amount, remarks)
                showSendDialog = false
                showVerifyDialogForSend = true
            }
        )
    }

    // Top Up Dialog (Leads to MPIN verification)
    if (showTopUpDialog) {
        TopUpDialog(
            isNepali = isNepali,
            onDismiss = { showTopUpDialog = false },
            onConfirm = { amount, method ->
                pendingTopUp = Pair(amount, method)
                showTopUpDialog = false
                showVerifyDialogForTopUp = true
            }
        )
    }

    // MPIN Verification for Send Money (Includes Rs 10 transfer fee in authorization)
    if (showVerifyDialogForSend && pendingSendTransfer != null) {
        val pending = pendingSendTransfer!!
        val totalToAuthorize = pending.second + 10.0
        WalletMpinVerifyDialog(
            amountToAuthorize = totalToAuthorize,
            recipientName = pending.first,
            isNepali = isNepali,
            onDismiss = {
                showVerifyDialogForSend = false
                pendingSendTransfer = null
            },
            onVerifiedSuccess = {
                showVerifyDialogForSend = false
                val success = onSendMoney(pending.first, pending.second, pending.third)
                if (success) {
                    statusMessage = if (isNepali)
                        "सफलतापूर्वक ${pending.first} लाई रु ${pending.second} पठाइयो! (शुल्क: रु १०, जम्मा कटौती: रु $totalToAuthorize)"
                    else
                        "Successfully sent Rs ${pending.second} to ${pending.first}! (Fee: Rs 10, Total deducted: Rs $totalToAuthorize)"
                } else {
                    statusMessage = if (isNepali)
                        "मौज्दात अपुग छ। कृपया आफ्नो वालेट टप-अप गर्नुहोस् (रु १० शुल्क सहित)।"
                    else
                        "Insufficient balance. Please top up your wallet (including Rs 10 transfer fee)."
                }
                pendingSendTransfer = null
            }
        )
    }

    // MPIN Verification for Wallet Top Up
    if (showVerifyDialogForTopUp && pendingTopUp != null) {
        val pending = pendingTopUp!!
        WalletMpinVerifyDialog(
            amountToAuthorize = pending.first,
            recipientName = null,
            isNepali = isNepali,
            onDismiss = {
                showVerifyDialogForTopUp = false
                pendingTopUp = null
            },
            onVerifiedSuccess = {
                showVerifyDialogForTopUp = false
                onTopUp(pending.first, pending.second)
                statusMessage = if (isNepali)
                    "सफलतापूर्वक रु ${pending.first} टप-अप भयो (${pending.second})!"
                else
                    "Successfully topped up रु ${pending.first} via ${pending.second}!"
                pendingTopUp = null
            }
        )
    }

    // MPIN Setup Dialog for New Users or MPIN Change
    if (showMpinSetupDialog) {
        WalletMpinSetupDialog(
            isNepali = isNepali,
            onDismiss = { showMpinSetupDialog = false },
            onMpinSetupComplete = {
                showMpinSetupDialog = false
                statusMessage = if (isNepali) "४-अङ्कको MPIN सफलतापूर्वक सुरक्षित गरियो!" else "4-digit MPIN successfully set up and active!"
            }
        )
    }

    // Forgot MPIN Dialog (Sends 6-digit OTP to user's registered email)
    if (showForgotMpinDialog) {
        ForgotMpinDialog(
            isNepali = isNepali,
            onDismiss = { showForgotMpinDialog = false },
            onMpinResetSuccess = {
                showForgotMpinDialog = false
                statusMessage = if (isNepali) "नयाँ MPIN सफलतापूर्वक सेट भयो!" else "MPIN reset successfully! Use your new MPIN for future transactions."
            }
        )
    }
}

@Composable
fun PayActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GatewayRow(name: String, id: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = id, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SendMoneyDialog(
    balance: Double = Double.MAX_VALUE,
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (recipient: String, amount: Double, remarks: String) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var showBreakdownConfirmation by remember { mutableStateOf(false) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val fee = 10.0
    val totalDeducted = parsedAmount + fee
    val canProceed = recipient.isNotBlank() && parsedAmount > 0.0
    val hasSufficientBalance = balance >= totalDeducted

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("send_money_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                if (!showBreakdownConfirmation) {
                    // Step 1: Input Transfer Details
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = ConnectNpBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNepali) "रकम पठाउनुहोस् (Instant P2P)" else "Send Money (Instant P2P)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text("Recipient Name / Phone / @Handle") },
                        placeholder = { Text("e.g. 9841000000 or username") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_money_recipient_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Transfer Amount (रु NPR)") },
                        placeholder = { Text("e.g. 500") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_money_amount_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks (Optional)") },
                        placeholder = { Text("e.g. Tea & snacks") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_money_remarks_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notice of Rs 10 flat transaction fee
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isNepali) "प्रत्येक स्थानान्तरणमा रु १० को निश्चित कारोबार शुल्क लाग्नेछ।" else "A flat fee of Rs 10 applies to each transfer.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (canProceed) {
                                    showBreakdownConfirmation = true
                                }
                            },
                            enabled = canProceed,
                            colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("send_money_proceed_button")
                        ) {
                            Text(if (isNepali) "अघि बढ्नुहोस्" else "Review Transfer", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Step 2: Confirmation Breakdown Dialog
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = ConnectNpBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNepali) "स्थानान्तरण विवरण पुष्टि गर्नुहोस्" else "Confirm Transfer Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Recipient Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isNepali) "प्राप्तकर्ता:" else "Recipient:",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = recipient,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.testTag("breakdown_recipient")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Transfer Amount Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isNepali) "स्थानान्तरण रकम:" else "Transfer Amount:",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "रु ${String.format("%,.2f", parsedAmount)}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.testTag("breakdown_transfer_amount")
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Transaction Fee Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isNepali) "कारोबार शुल्क (Fee):" else "Fee (Flat):",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "रु 10.00",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = NepalCrimson,
                                    modifier = Modifier.testTag("breakdown_fee_amount")
                                )
                            }

                            if (remarks.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isNepali) "कैफियत:" else "Remarks:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = remarks,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Total Deducted Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isNepali) "जम्मा कटौती:" else "Total Deducted:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "रु ${String.format("%,.2f", totalDeducted)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = ConnectNpBlue,
                                    modifier = Modifier.testTag("breakdown_total_deducted")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!hasSufficientBalance) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NepalCrimson.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isNepali)
                                    "अपर्याप्त मौज्दात! आवश्यक: रु ${String.format("%,.2f", totalDeducted)}, उपलब्ध: रु ${String.format("%,.2f", balance)}"
                                else
                                    "Insufficient balance! Required: Rs ${String.format("%,.2f", totalDeducted)}, Available: Rs ${String.format("%,.2f", balance)}",
                                color = NepalCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showBreakdownConfirmation = false },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_money_back_button")
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                onConfirm(recipient.trim(), parsedAmount, remarks.trim())
                            },
                            enabled = hasSufficientBalance,
                            colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("send_money_confirm_button")
                        ) {
                            Text(if (isNepali) "पुष्टि गरी पठाउनुहोस्" else "Confirm & Send", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopUpDialog(
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("eSewa") }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val canSubmit = parsedAmount > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "वालेट टप-अप गर्नुहोस्" else "Top-Up Wallet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (रु NPR)") },
                    placeholder = { Text("e.g. 1000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Payment Gateway:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                listOf("eSewa", "Khalti", "Nepal Bank IPS").forEach { method ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMethod = method }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = selectedMethod == method, onClick = { selectedMethod = method })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(method, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (canSubmit) {
                            onConfirm(parsedAmount, selectedMethod)
                        }
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isNepali) "टप-अप गर्नुहोस्" else "Top Up Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
