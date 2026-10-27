package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.window.DialogProperties
import com.example.model.LinkedPaymentMethod
import com.example.model.PaymentMethodType
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson

val NEPAL_BANKS = listOf(
    "Nabil Bank Limited",
    "Global IME Bank",
    "NIC Asia Bank",
    "Nepal Investment Mega Bank",
    "Rastriya Banijya Bank",
    "Sanima Bank",
    "Siddhartha Bank",
    "Everest Bank",
    "Himalayan Bank",
    "Nepal Bank Limited",
    "Standard Chartered Bank Nepal",
    "Machhapuchchhre Bank",
    "Laxmi Sunrise Bank",
    "Citizens Bank International",
    "Prabhu Bank",
    "Kumari Bank"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkPaymentMethodDialog(
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onSaveMethod: (LinkedPaymentMethod) -> Unit
) {
    var selectedType by remember { mutableStateOf(PaymentMethodType.ESEWA) }

    // eSewa state
    var esewaId by remember { mutableStateOf("") }
    var esewaHolderName by remember { mutableStateOf("") }

    // Khalti state
    var khaltiNumber by remember { mutableStateOf("") }
    var khaltiHolderName by remember { mutableStateOf("") }

    // Bank state
    var selectedBank by remember { mutableStateOf(NEPAL_BANKS.first()) }
    var isBankDropdownExpanded by remember { mutableStateOf(false) }
    var bankAccountNumber by remember { mutableStateOf("") }
    var bankAccountHolder by remember { mutableStateOf("") }
    var bankBranch by remember { mutableStateOf("Kathmandu") }

    var isDefault by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val canSave = when (selectedType) {
        PaymentMethodType.ESEWA -> esewaId.trim().length >= 10 && esewaHolderName.trim().isNotBlank()
        PaymentMethodType.KHALTI -> khaltiNumber.trim().length >= 10 && khaltiHolderName.trim().isNotBlank()
        PaymentMethodType.BANK -> selectedBank.isNotBlank() && bankAccountNumber.trim().length >= 6 && bankAccountHolder.trim().isNotBlank()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("link_payment_method_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
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
                                .background(ConnectNpBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = ConnectNpBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isNepali) "भुक्तानी माध्यम जोड्नुहोस्" else "Link Payment Method",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isNepali) "eSewa, Khalti वा बैंक खाता" else "eSewa, Khalti or Bank Account",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Method Type Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentTypeTab(
                        title = "eSewa",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color(0xFF60BB46),
                        isSelected = selectedType == PaymentMethodType.ESEWA,
                        modifier = Modifier.weight(1f).testTag("select_type_esewa"),
                        onClick = {
                            selectedType = PaymentMethodType.ESEWA
                            validationError = null
                        }
                    )

                    PaymentTypeTab(
                        title = "Khalti",
                        icon = Icons.Default.Wallet,
                        color = Color(0xFF5C2D91),
                        isSelected = selectedType == PaymentMethodType.KHALTI,
                        modifier = Modifier.weight(1f).testTag("select_type_khalti"),
                        onClick = {
                            selectedType = PaymentMethodType.KHALTI
                            validationError = null
                        }
                    )

                    PaymentTypeTab(
                        title = "Bank",
                        icon = Icons.Default.AccountBalance,
                        color = ConnectNpBlue,
                        isSelected = selectedType == PaymentMethodType.BANK,
                        modifier = Modifier.weight(1f).testTag("select_type_bank"),
                        onClick = {
                            selectedType = PaymentMethodType.BANK
                            validationError = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Form fields according to selected payment method
                when (selectedType) {
                    PaymentMethodType.ESEWA -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF60BB46).copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF60BB46))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isNepali) "eSewa ID (१० अङ्कको मोबाइल नम्बर)" else "Connect official eSewa Nepal account",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = esewaId,
                            onValueChange = {
                                if (it.length <= 10 && it.all { ch -> ch.isDigit() }) {
                                    esewaId = it
                                }
                            },
                            label = { Text(if (isNepali) "eSewa ID / मोबाइल नम्बर" else "eSewa ID (Mobile Number)") },
                            placeholder = { Text("e.g. 9841000000") },
                            leadingIcon = {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF60BB46))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_payment_esewa_id_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = esewaHolderName,
                            onValueChange = { esewaHolderName = it },
                            label = { Text(if (isNepali) "खातावालाको पूरा नाम" else "Account Holder Full Name") },
                            placeholder = { Text("As registered on eSewa KYC") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ConnectNpBlue)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_payment_esewa_name_input")
                        )
                    }

                    PaymentMethodType.KHALTI -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF5C2D91).copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF5C2D91))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isNepali) "Khalti नम्बर (१० अङ्कको मोबाइल नम्बर)" else "Connect official Khalti Digital Wallet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF5C2D91)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = khaltiNumber,
                            onValueChange = {
                                if (it.length <= 10 && it.all { ch -> ch.isDigit() }) {
                                    khaltiNumber = it
                                }
                            },
                            label = { Text(if (isNepali) "Khalti दर्ता नम्बर" else "Khalti Registered Number") },
                            placeholder = { Text("e.g. 9802000000") },
                            leadingIcon = {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF5C2D91))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_payment_khalti_number_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = khaltiHolderName,
                            onValueChange = { khaltiHolderName = it },
                            label = { Text(if (isNepali) "खातावालाको पूरा नाम" else "Account Holder Full Name") },
                            placeholder = { Text("As registered on Khalti KYC") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ConnectNpBlue)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_payment_khalti_name_input")
                        )
                    }

                    PaymentMethodType.BANK -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ConnectNpBlue.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ConnectNpBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isNepali) "नेपालका वाणिज्य बैंकहरू (NCHL / IPS)" else "Commercial Banks in Nepal (NCHL Direct IPS)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ConnectNpBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bank Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isBankDropdownExpanded,
                            onExpandedChange = { isBankDropdownExpanded = !isBankDropdownExpanded },
                            modifier = Modifier.fillMaxWidth().testTag("link_payment_bank_selector")
                        ) {
                            OutlinedTextField(
                                value = selectedBank,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isNepali) "बैंक छान्नुहोस्" else "Select Bank") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBankDropdownExpanded) },
                                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ConnectNpBlue) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isBankDropdownExpanded,
                                onDismissRequest = { isBankDropdownExpanded = false }
                            ) {
                                NEPAL_BANKS.forEach { bank ->
                                    DropdownMenuItem(
                                        text = { Text(bank) },
                                        onClick = {
                                            selectedBank = bank
                                            isBankDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = bankAccountNumber,
                            onValueChange = { bankAccountNumber = it },
                            label = { Text(if (isNepali) "बैंक खाता नम्बर" else "Bank Account Number") },
                            placeholder = { Text("e.g. 0102000000000") },
                            leadingIcon = {
                                Icon(Icons.Default.Numbers, contentDescription = null, tint = ConnectNpBlue)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_payment_bank_account_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = bankAccountHolder,
                            onValueChange = { bankAccountHolder = it },
                            label = { Text(if (isNepali) "खातावालाको पूरा नाम" else "Account Holder Full Name") },
                            placeholder = { Text("Exact name matching citizenship & bank passbook") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = ConnectNpBlue)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_payment_bank_holder_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = bankBranch,
                            onValueChange = { bankBranch = it },
                            label = { Text(if (isNepali) "शाखा (Branch Name)" else "Branch Name") },
                            placeholder = { Text("e.g. New Road / Pokhara") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = ConnectNpBlue)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_payment_bank_branch_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Default Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isDefault = !isDefault }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(checkedColor = ConnectNpBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isNepali) "यसलाई प्राथमिक भुक्तानी माध्यम बनाउनुहोस्" else "Set as default payment/withdrawal method",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        color = NepalCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Security Note
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ConnectNpBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNepali)
                                "🔒 २५६-बिट ईन्क्रिप्सनद्वारा सुरक्षित। विवरणहरू तपाईंको प्रोफाइलमा सुरक्षित राखिनेछ।"
                            else
                                "🔒 256-bit encrypted. Details will be saved securely under your profile in Firestore.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (!canSave) {
                                validationError = if (isNepali) "कृपया सबै आवश्यक विवरणहरू भर्नुहोस्" else "Please complete all required fields"
                                return@Button
                            }

                            val newMethod = when (selectedType) {
                                PaymentMethodType.ESEWA -> {
                                    LinkedPaymentMethod(
                                        id = "pm_esewa_${System.currentTimeMillis()}",
                                        type = PaymentMethodType.ESEWA,
                                        title = "eSewa Nepal",
                                        accountNumber = esewaId.trim(),
                                        accountHolderName = esewaHolderName.trim(),
                                        isDefault = isDefault,
                                        linkedAt = "Verified"
                                    )
                                }
                                PaymentMethodType.KHALTI -> {
                                    LinkedPaymentMethod(
                                        id = "pm_khalti_${System.currentTimeMillis()}",
                                        type = PaymentMethodType.KHALTI,
                                        title = "Khalti Digital Wallet",
                                        accountNumber = khaltiNumber.trim(),
                                        accountHolderName = khaltiHolderName.trim(),
                                        isDefault = isDefault,
                                        linkedAt = "Active"
                                    )
                                }
                                PaymentMethodType.BANK -> {
                                    LinkedPaymentMethod(
                                        id = "pm_bank_${System.currentTimeMillis()}",
                                        type = PaymentMethodType.BANK,
                                        title = selectedBank,
                                        accountNumber = bankAccountNumber.trim(),
                                        accountHolderName = bankAccountHolder.trim(),
                                        bankName = selectedBank,
                                        branch = bankBranch.trim(),
                                        isDefault = isDefault,
                                        linkedAt = "Verified"
                                    )
                                }
                            }
                            onSaveMethod(newMethod)
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("link_payment_method_save_button")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isNepali) "माध्यम जोड्नुहोस्" else "Link Method",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentTypeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
