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
import com.example.model.WalletTransaction
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
    var showSendDialog by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf(false) }
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
                                text = "Zero-Fee P2P 🇳🇵",
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

    // Send Money Dialog
    if (showSendDialog) {
        SendMoneyDialog(
            isNepali = isNepali,
            onDismiss = { showSendDialog = false },
            onConfirm = { recipient, amount, remarks ->
                val success = onSendMoney(recipient, amount, remarks)
                if (success) {
                    statusMessage = "Successfully sent रु $amount to $recipient!"
                } else {
                    statusMessage = "Insufficient balance. Please top up your wallet."
                }
                showSendDialog = false
            }
        )
    }

    // Top Up Dialog
    if (showTopUpDialog) {
        TopUpDialog(
            isNepali = isNepali,
            onDismiss = { showTopUpDialog = false },
            onConfirm = { amount, method ->
                onTopUp(amount, method)
                statusMessage = "Successfully topped up रु $amount via $method!"
                showTopUpDialog = false
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
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (recipient: String, amount: Double, remarks: String) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val canSubmit = recipient.isNotBlank() && parsedAmount > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNepali) "रकम पठाउनुहोस् (Instant P2P)" else "Send Money (Instant P2P)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Recipient Name / Phone / @Handle") },
                    placeholder = { Text("e.g. 9841000000 or username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (रु NPR)") },
                    placeholder = { Text("e.g. 500") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    placeholder = { Text("e.g. Tea & snacks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (canSubmit) {
                            onConfirm(recipient.trim(), parsedAmount, remarks.trim())
                        }
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isNepali) "रकम पठाउन पुष्टि गर्नुहोस्" else "Confirm Transfer", fontWeight = FontWeight.Bold)
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
