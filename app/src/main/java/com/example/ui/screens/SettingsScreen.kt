package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.data.ConnectNpFirestoreRepository
import com.example.data.ConnectNpRepository
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.ForgotMpinDialog
import com.example.ui.components.WalletMpinSetupDialog
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.PoppinsFontFamily
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    isNepali: Boolean,
    isDarkMode: Boolean,
    onToggleLanguage: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onAccountDeleted: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser = Firebase.auth.currentUser

    val blockedUserIds by ConnectNpRepository.blockedUserIds.collectAsState()
    val blockedUserNames by ConnectNpRepository.blockedUserNames.collectAsState()

    val isBiometricEnabled by ConnectNpRepository.isBiometricEnabled.collectAsState()
    val walletMpin by ConnectNpRepository.walletMpin.collectAsState()
    var showMpinSetupDialog by remember { mutableStateOf(false) }
    var showForgotMpinDialog by remember { mutableStateOf(false) }

    val firestoreRepo = remember { ConnectNpFirestoreRepository(context) }
    val userProfileDoc by firestoreRepo.observeUserProfile().collectAsState(initial = null)

    var currentDisplayName by remember(currentUser, userProfileDoc) {
        mutableStateOf(userProfileDoc?.name ?: currentUser?.displayName ?: "ConnectNP User")
    }
    var currentBio by remember(userProfileDoc) {
        mutableStateOf(userProfileDoc?.bio ?: "Proud ConnectNP Citizen 🇳🇵")
    }
    var currentPhotoUrl by remember(currentUser, userProfileDoc) {
        mutableStateOf(userProfileDoc?.photoUrl?.ifBlank { null } ?: currentUser?.photoUrl?.toString())
    }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var showOfficialPage by remember { mutableStateOf(false) }

    val isAdmin = ConnectNpRepository.isUserAdmin(currentUser?.email)

    if (showOfficialPage) {
        OfficialPageScreen(onBack = { showOfficialPage = false })
        return
    }

    if (showAdminPanel) {
        AdminPanelScreen(onBack = { showAdminPanel = false })
        return
    }

    // Notification toggles
    var notifyChats by remember { mutableStateOf(true) }
    var notifyLikes by remember { mutableStateOf(true) }
    var notifyCommunity by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Screen Header
        item {
            Text(
                text = if (isNepali) "सेटिङ र गोपनीयता" else "Settings & Privacy",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isNepali) "तपाईंको खाता र एप प्राथमिकताहरू प्रबन्ध गर्नुहोस्" else "Manage your account and app preferences",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Account Information Card (TASK 1: Profile Edit)
        item {
            SettingsCategoryHeader(title = if (isNepali) "खाता जानकारी" else "Account Information")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ConnectNpBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentPhotoUrl,
                                    contentDescription = currentDisplayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = currentDisplayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNepali) "सक्रिय खाता" else "Active Account",
                                fontSize = 11.sp,
                                color = ConnectNpBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentDisplayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.email ?: "No email provided",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentBio.isNotBlank()) {
                                Text(
                                    text = currentBio,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Edit Profile Button (TASK 1)
                    Button(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_edit_profile_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isNepali) "प्रोफाइल र फोटो सम्पादन गर्नुहोस्" else "Edit Profile & Photo", fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Hidden Admin Panel Access (Exclusive to gautamprasanna641@gmail.com)
        if (isAdmin) {
            item {
                SettingsCategoryHeader(title = if (isNepali) "प्रशासक नियन्त्रण (Admin Access)" else "Administrator Controls")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ConnectNpBlue.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_panel_entry_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(ConnectNpBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Admin Control Panel",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = NepalCrimson
                                    ) {
                                        Text(
                                            text = "OWNER",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Manage registered users, toggle Blue Ticks, and delete posts/comments",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAdminPanel = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_admin_panel_button")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isNepali) "एडमिन प्यानल खोल्नुहोस्" else "Open Admin Panel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showOfficialPage = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnectNpBlue),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ConnectNpBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manage_official_page_button")
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp), tint = ConnectNpBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isNepali) "आधिकारिक पृष्ठ व्यवस्थापन (CONNECTNP Official)" else "Official Page Manager (CONNECTNP)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // App Preferences (Language & Dark Mode)
        item {
            SettingsCategoryHeader(title = if (isNepali) "एप प्राथमिकताहरू" else "App Preferences")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    // Language
                    SettingsToggleRow(
                        icon = Icons.Default.Language,
                        title = if (isNepali) "भाषा (Language)" else "Language / भाषा",
                        subtitle = if (isNepali) "नेपाली (Nepali)" else "English",
                        checked = isNepali,
                        onCheckedChange = { onToggleLanguage() }
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    // Dark Mode
                    SettingsToggleRow(
                        icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        title = if (isNepali) "डार्क मोड (Dark Theme)" else "Dark Theme",
                        subtitle = if (isDarkMode) "Enabled" else "Disabled",
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Wallet Security & Biometric Authentication Section
        item {
            SettingsCategoryHeader(title = if (isNepali) "वालेट र सुरक्षा (Wallet & Security)" else "Wallet Security & MPIN")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_security_section")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Biometric Authentication Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ConnectNpBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric",
                                tint = ConnectNpBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNepali) "बायोमेट्रिक प्रमाणीकरण" else "Biometric Authentication",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isNepali)
                                    "पिन थिच्नुको सट्टा फिंगरप्रिन्ट वा फेस आईडी प्रयोग गरी कारोबार गर्नुहोस्"
                                else
                                    "Use fingerprint / face ID to authorize transactions instead of typing PIN",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { isChecked ->
                                ConnectNpRepository.setBiometricEnabled(isChecked, context)
                                Toast.makeText(
                                    context,
                                    if (isChecked) {
                                        if (isNepali) "बायोमेट्रिक प्रमाणीकरण सक्रिय भयो" else "Biometric authentication enabled"
                                    } else {
                                        if (isNepali) "बायोमेट्रिक प्रमाणीकरण निष्क्रिय भयो" else "Biometric authentication disabled"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("biometric_auth_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // 4-Digit MPIN Management Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NepalCrimson.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "MPIN",
                                tint = NepalCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNepali) "४-अङ्कको वालेट MPIN" else "4-Digit Wallet MPIN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (walletMpin.isNullOrBlank()) {
                                    if (isNepali) "सेट गरिएको छैन" else "Not configured"
                                } else {
                                    if (isNepali) "सुरक्षा सक्रिय (••••)" else "Protection Active (••••)"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { showMpinSetupDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("settings_manage_mpin_button")
                        ) {
                            Text(
                                text = if (walletMpin.isNullOrBlank()) {
                                    if (isNepali) "सेट गर्नुहोस्" else "Set MPIN"
                                } else {
                                    if (isNepali) "परिवर्तन" else "Change"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Forgot MPIN / Reset via OTP Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = "Reset MPIN",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNepali) "MPIN बिर्सनुभयो?" else "Forgot MPIN?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isNepali) "इमेलमा ६-अङ्कको OTP पठाई पुनः सेट गर्नुहोस्" else "Reset MPIN using 6-digit OTP sent to registered email",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { showForgotMpinDialog = true },
                            modifier = Modifier.testTag("settings_forgot_mpin_button")
                        ) {
                            Text(if (isNepali) "OTP पठाउनुहोस्" else "Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ConnectNpBlue)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Push Notifications
        item {
            SettingsCategoryHeader(title = if (isNepali) "पुश सूचनाहरू (Notifications)" else "Push Notifications")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Outlined.Chat,
                        title = if (isNepali) "च्याट सूचनाहरू" else "Chat & Messages",
                        subtitle = "Instant alert for new personal & group messages",
                        checked = notifyChats,
                        onCheckedChange = { notifyChats = it }
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsToggleRow(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = if (isNepali) "लाइक र प्रतिक्रिया" else "Likes & Reactions",
                        subtitle = "Notifications when someone reacts to your posts",
                        checked = notifyLikes,
                        onCheckedChange = { notifyLikes = it }
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsToggleRow(
                        icon = Icons.Outlined.Campaign,
                        title = if (isNepali) "समुदाय अपडेटहरू" else "Community Updates",
                        subtitle = "Himalayan trends and marketplace alerts",
                        checked = notifyCommunity,
                        onCheckedChange = { notifyCommunity = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Blocked Users
        item {
            SettingsCategoryHeader(title = if (isNepali) "अवरुद्ध प्रयोगकर्ताहरू" else "Blocked Users")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val displayBlockedUsers = remember(blockedUserIds, blockedUserNames) {
                        if (blockedUserNames.isNotEmpty()) {
                            blockedUserNames.toList()
                        } else {
                            blockedUserIds.filter { !it.startsWith("@") }.distinct().map { it to (blockedUserNames[it] ?: "User ($it)") }
                        }
                    }

                    if (displayBlockedUsers.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ConnectNpBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isNepali) "कुनै अवरुद्ध प्रयोगकर्ता छैन" else "No blocked users. Your feed is clean.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        displayBlockedUsers.forEach { (userId, userName) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Block, contentDescription = null, tint = NepalCrimson, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = userName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { ConnectNpRepository.unblockUser(userId, context, userName = userName) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (isNepali) "अनब्लक" else "Unblock", fontSize = 11.sp)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Official CONNECTNP Page (Available for all users to view and follow)
        item {
            SettingsCategoryHeader(title = if (isNepali) "आधिकारिक समुदाय" else "Official Community")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOfficialPage = true }
                    .testTag("view_official_page_row")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ConnectNpBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CONNECTNP Official Page",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = ConnectNpBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = if (isNepali) "आधिकारिक घोषणाहरू र नयाँ सुविधाहरू हेर्नुहोस्" else "Follow for official updates, new features & announcements",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Legal & Policies (Privacy Policy)
        item {
            SettingsCategoryHeader(title = if (isNepali) "कानूनी र नीतिहरू" else "Legal & Policies")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    SettingsClickableRow(
                        icon = Icons.Outlined.Policy,
                        title = if (isNepali) "गोपनीयता नीति र सेवा सर्तहरू" else "Privacy Policy & Terms of Service",
                        subtitle = if (isNepali) "डाटा सुरक्षा र प्रयोगकर्ता गोपनीयता" else "Data safety & privacy protection",
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Account Actions (Sign Out & Delete Account)
        item {
            SettingsCategoryHeader(title = if (isNepali) "खाता कार्यहरू" else "Account Actions")

            // Sign Out Button
            OutlinedButton(
                onClick = {
                    signOutUser(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = onSignOut,
                        scope = coroutineScope
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_sign_out_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isNepali) "साइन आउट गर्नुहोस्" else "Sign Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Delete Account Button (TASK 1: Mandatory for Google Play Store)
            Button(
                onClick = { showDeleteConfirmDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("delete_account_button")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isNepali) "खाता स्थायी रूपमा मेटाउनुहोस् (Delete Account)" else "Delete Account Permanently",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }

    // Confirmation Dialog for Account Deletion (TASK 1)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirmDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = NepalCrimson, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = if (isNepali) "के तपाईं पक्का खाता मेटाउन चाहनुहुन्छ?" else "Delete Account Permanently?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isNepali)
                            "यो कार्य पूर्ववत गर्न सकिँदैन। तपाईंको CONNECTNP प्रोफाइल, पोष्टहरू, सन्देशहरू, र वालेट ब्यालेन्स स्थायी रूपमा मेटिनेछ।"
                        else
                            "This action cannot be undone. All your CONNECTNP posts, profile data, wallet records, and credentials will be permanently erased and cannot be recovered.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    if (isDeleting) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Deleting account data...", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch {
                            val result = ConnectNpRepository.deleteUserAccount(context)
                            isDeleting = false
                            showDeleteConfirmDialog = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Account successfully deleted", Toast.LENGTH_LONG).show()
                                signOutUser(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onAccountDeleted,
                                    scope = coroutineScope
                                )
                            } else {
                                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete account"
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                // Sign out anyway if auth state is invalidated
                                signOutUser(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onAccountDeleted,
                                    scope = coroutineScope
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                    enabled = !isDeleting,
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text(if (isDeleting) "Deleting..." else "Yes, Delete Account", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Privacy Policy & Compliance Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy & Terms", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CONNECTNP is committed to safeguarding your privacy and data security:\n\n" +
                                "1. Account & Data Privacy: You can update or delete your account and personal data at any time.\n" +
                                "2. Community Safety: Zero tolerance for harassment or objectionable content. Easily report posts and block offending users.\n" +
                                "3. Secure Sign-In: Industry-standard authentication protecting your account.\n" +
                                "4. Encrypted Data: All communications and records are encrypted in transit and at rest.\n\n" +
                                "Contact: support@connectnp.app",
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Edit Profile Dialog (TASK 1: Save to Firestore & Firebase Storage)
    if (showEditProfileDialog) {
        EditProfileDialog(
            initialName = currentDisplayName,
            initialBio = currentBio,
            initialPhotoUrl = currentPhotoUrl,
            isNepali = isNepali,
            onDismiss = { showEditProfileDialog = false },
            onProfileUpdated = { newName, newBio, newPhotoUrl ->
                currentDisplayName = newName
                currentBio = newBio
                currentPhotoUrl = newPhotoUrl
            }
        )
    }

    // MPIN Setup / Change Dialog
    if (showMpinSetupDialog) {
        WalletMpinSetupDialog(
            isNepali = isNepali,
            onDismiss = { showMpinSetupDialog = false },
            onMpinSetupComplete = {
                showMpinSetupDialog = false
            }
        )
    }

    // Forgot MPIN / Reset Dialog
    if (showForgotMpinDialog) {
        ForgotMpinDialog(
            isNepali = isNepali,
            onDismiss = { showForgotMpinDialog = false },
            onMpinResetSuccess = {
                showForgotMpinDialog = false
            }
        )
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = ConnectNpBlue,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = ConnectNpBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = ConnectNpBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
