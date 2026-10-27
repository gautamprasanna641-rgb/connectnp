package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.LinkedPaymentMethod
import com.example.model.PaymentMethodType
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.ConnectNpSuccess
import com.example.ui.theme.NepalCrimson

@Composable
fun WithdrawMoneyDialog(
    balance: Double,
    linkedMethods: List<LinkedPaymentMethod>,
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onLinkNewMethod: () -> Unit,
    onConfirmWithdraw: (amount: Double, destination: String, methodType: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethodId by remember {
        mutableStateOf(linkedMethods.find { it.isDefault }?.id ?: linkedMethods.firstOrNull()?.id ?: "")
    }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val selectedMethod = linkedMethods.find { it.id == selectedMethodId }
    val hasEnoughBalance = balance >= parsedAmount && parsedAmount > 0.0
    val canProceed = hasEnoughBalance && selectedMethod != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("withdraw_money_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NepalCrimson.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = NepalCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isNepali) "रकम निकाल्नुहोस् (Withdraw)" else "Withdraw to Bank / Wallet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isNepali) "उपलब्ध मौज्दात: रु ${String.format("%,.2f", balance)}" else "Available: Rs ${String.format("%,.2f", balance)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (isNepali) "निकाल्ने रकम (रु NPR)" else "Withdraw Amount (Rs NPR)") },
                    placeholder = { Text("e.g. 1000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(500, 1000, 2500, 5000).forEach { amt ->
                        SuggestionChip(
                            onClick = { amountText = amt.toString() },
                            label = { Text("रु $amt", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Destination Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNepali) "गन्तव्य भुक्तानी माध्यम छान्नुहोस्:" else "Select Destination Method:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    TextButton(
                        onClick = onLinkNewMethod,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isNepali) "+ नयाँ जोड्नुहोस्" else "+ Link New", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (linkedMethods.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isNepali) "कुनै भुक्तानी माध्यम जोडिएको छैन" else "No payment methods linked yet",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onLinkNewMethod,
                                colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isNepali) "eSewa / Khalti / बैंक जोड्नुहोस्" else "Link eSewa / Khalti / Bank", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    linkedMethods.forEach { method ->
                        val isSelected = method.id == selectedMethodId
                        val methodColor = when (method.type) {
                            PaymentMethodType.ESEWA -> Color(0xFF60BB46)
                            PaymentMethodType.KHALTI -> Color(0xFF5C2D91)
                            PaymentMethodType.BANK -> ConnectNpBlue
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) methodColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, methodColor) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMethodId = method.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedMethodId = method.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = methodColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = method.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (method.isDefault) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ConnectNpSuccess.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "DEFAULT",
                                                    color = ConnectNpSuccess,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${method.accountNumber} • ${method.accountHolderName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown note
                if (selectedMethod != null && parsedAmount > 0.0) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Withdrawal Amount:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("रु ${String.format("%,.2f", parsedAmount)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Processing Fee:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("रु ०.०० (Free)", fontSize = 11.sp, color = ConnectNpSuccess, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Destination:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${selectedMethod.type.displayName} (${selectedMethod.accountNumber})", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                if (parsedAmount > balance) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isNepali) "अपर्याप्त मौज्दात! तपाईंको मौज्दात भन्दा बढी रकम निकाल्न मिल्दैन।" else "Insufficient balance for this withdrawal amount.",
                        color = NepalCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isNepali) "रद्द" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (canProceed && selectedMethod != null) {
                                onConfirmWithdraw(
                                    parsedAmount,
                                    "${selectedMethod.title} (${selectedMethod.accountNumber})",
                                    selectedMethod.type.name
                                )
                            }
                        },
                        enabled = canProceed,
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("withdraw_proceed_button")
                    ) {
                        Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isNepali) "निकाल्नुहोस्" else "Withdraw Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
