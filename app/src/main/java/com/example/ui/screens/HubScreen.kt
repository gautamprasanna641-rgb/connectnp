package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import com.example.data.ConnectNpRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.SampleDataProvider
import com.example.model.DatingProfile
import com.example.model.JobItem
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.ConnectNpSuccess
import com.example.ui.theme.NepalCrimson

enum class HubSubSection {
    OVERVIEW, DATING, JOBS, CREATOR_DASHBOARD, LIVE_STREAM, SAFETY_SOS
}

@Composable
fun HubScreen(
    jobs: List<JobItem>,
    datingProfiles: List<DatingProfile>,
    datingMatches: Int,
    isNepali: Boolean,
    isDarkMode: Boolean,
    onApplyJob: (String) -> Unit,
    onSwipeDating: (String, Boolean) -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenLogin: () -> Unit = {},
    onOpenSplash: () -> Unit = {}
) {
    var activeSection by remember { mutableStateOf(HubSubSection.OVERVIEW) }

    when (activeSection) {
        HubSubSection.DATING -> {
            DatingSubSection(
                profiles = datingProfiles,
                matches = datingMatches,
                isNepali = isNepali,
                onBack = { activeSection = HubSubSection.OVERVIEW },
                onSwipe = onSwipeDating
            )
        }
        HubSubSection.JOBS -> {
            JobsSubSection(
                jobs = jobs,
                isNepali = isNepali,
                onBack = { activeSection = HubSubSection.OVERVIEW },
                onApply = onApplyJob
            )
        }
        HubSubSection.CREATOR_DASHBOARD -> {
            CreatorDashboardSubSection(
                isNepali = isNepali,
                onBack = { activeSection = HubSubSection.OVERVIEW }
            )
        }
        HubSubSection.LIVE_STREAM -> {
            LiveStreamSubSection(
                isNepali = isNepali,
                onBack = { activeSection = HubSubSection.OVERVIEW }
            )
        }
        HubSubSection.SAFETY_SOS -> {
            SafetySosSubSection(
                isNepali = isNepali,
                onBack = { activeSection = HubSubSection.OVERVIEW }
            )
        }
        HubSubSection.OVERVIEW -> {
            HubOverview(
                isNepali = isNepali,
                isDarkMode = isDarkMode,
                onSelectSection = { activeSection = it },
                onToggleLanguage = onToggleLanguage,
                onToggleDarkMode = onToggleDarkMode,
                onOpenLogin = onOpenLogin,
                onOpenSplash = onOpenSplash
            )
        }
    }
}

@Composable
fun HubOverview(
    isNepali: Boolean,
    isDarkMode: Boolean,
    onSelectSection: (HubSubSection) -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenLogin: () -> Unit = {},
    onOpenSplash: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp)
            .testTag("hub_overview"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // User Profile Card Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                val activeUser = ConnectNpRepository.getCurrentActiveUser()
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(ConnectNpBlue, NepalCrimson))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!activeUser.avatarUrl.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = activeUser.avatarUrl,
                                contentDescription = activeUser.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = activeUser.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeUser.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = ConnectNpBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "${activeUser.handle} • ${activeUser.followersCount} Followers",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🇳🇵 Nepal Creator & Verified Builder",
                            fontSize = 11.sp,
                            color = NepalCrimson,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (isNepali) "CONNECTNP सुपर-एप सेवाहरू" else "CONNECTNP SUPER-APP ECOSYSTEM",
                fontFamily = com.example.ui.theme.PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Services Grid / List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HubServiceItem(
                    title = if (isNepali) "CONNECTNP डेटिङ (Dating)" else "CONNECTNP DATING",
                    subtitle = "Find genuine connections & dating in Nepal",
                    icon = Icons.Default.Favorite,
                    badge = "Hot 🔥",
                    badgeColor = NepalCrimson,
                    onClick = { onSelectSection(HubSubSection.DATING) }
                )

                HubServiceItem(
                    title = if (isNepali) "CONNECTNP रोजगारी (Jobs & Careers)" else "CONNECTNP JOBS & CAREERS",
                    subtitle = "Discover verified tech & business jobs across Nepal",
                    icon = Icons.Default.Work,
                    badge = "New 💼",
                    badgeColor = ConnectNpBlue,
                    onClick = { onSelectSection(HubSubSection.JOBS) }
                )

                HubServiceItem(
                    title = if (isNepali) "CONNECTNP लाइभ स्ट्रिमिङ (Live Broadcast)" else "CONNECTNP LIVE STREAMING",
                    subtitle = "Go live to followers, receive tips & gifts",
                    icon = Icons.Default.Sensors,
                    badge = "Live 🔴",
                    badgeColor = NepalCrimson,
                    onClick = { onSelectSection(HubSubSection.LIVE_STREAM) }
                )

                HubServiceItem(
                    title = if (isNepali) "CONNECTNP सर्जक ड्यासबोर्ड" else "CONNECTNP CREATOR STUDIO",
                    subtitle = "Analytics, ad revenue, sponsorships & earnings",
                    icon = Icons.Default.Insights,
                    badge = "रु 48k+",
                    badgeColor = ConnectNpSuccess,
                    onClick = { onSelectSection(HubSubSection.CREATOR_DASHBOARD) }
                )

                HubServiceItem(
                    title = if (isNepali) "CONNECTNP सुरक्षा र आपतकालीन SOS" else "CONNECTNP SAFETY & SOS",
                    subtitle = "Emergency location beacon, privacy controls",
                    icon = Icons.Default.Shield,
                    badge = "Safe 🛡️",
                    badgeColor = Color(0xFFF7B928),
                    onClick = { onSelectSection(HubSubSection.SAFETY_SOS) }
                )
            }
        }

        // Settings & Language Section
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = if (isNepali) "एप सेटिङहरू (Settings)" else "App Settings & Preferences",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Language Switcher Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleLanguage() }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = ConnectNpBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Language / भाषा", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(if (isNepali) "नेपाली (Nepali)" else "English (US)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = isNepali, onCheckedChange = { onToggleLanguage() })
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Dark Mode Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDarkMode() }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null, tint = NepalCrimson)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dark Theme / डार्क मोड", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(if (isDarkMode) "Enabled" else "Disabled", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() })
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Preview Splash Screen Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSplash() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = ConnectNpBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CONNECTNP Splash Screen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("View Spotify/Netflix-grade brand splash screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Preview Login Dialog Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenLogin() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, tint = NepalCrimson)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CONNECTNP Sign In / Login", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Nepal OTP + Google + Biometrics auth modal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // About CONNECTNP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ConnectNpSuccess)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("About CONNECTNP v2.4", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Connecting Nepal & 1 Billion global citizens • 100% Nepali Built", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HubServiceItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DatingSubSection(
    profiles: List<DatingProfile>,
    matches: Int,
    isNepali: Boolean,
    onBack: () -> Unit,
    onSwipe: (String, Boolean) -> Unit
) {
    var matchDialogProfile by remember { mutableStateOf<DatingProfile?>(null) }
    val currentProfile = profiles.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isNepali) "ConnectNP डेटिङ 💕" else "ConnectNP Dating 💕",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NepalCrimson.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Matches: $matches",
                    color = NepalCrimson,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (currentProfile != null) {
            // Dating Profile Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                Brush.verticalGradient(
                                    listOf(ConnectNpBlue.copy(alpha = 0.8f), NepalCrimson.copy(alpha = 0.9f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentProfile.name.take(1),
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NepalCrimson
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${currentProfile.name}, ${currentProfile.age}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Text(
                                text = "${currentProfile.profession} • ${currentProfile.city}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "About Me:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentProfile.bio,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            currentProfile.interests.take(4).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Pass (X) & Like (Heart)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { onSwipe(currentProfile.id, false) },
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Pass", tint = Color.Gray, modifier = Modifier.size(32.dp))
                }

                IconButton(
                    onClick = {
                        matchDialogProfile = currentProfile
                        onSwipe(currentProfile.id, true)
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NepalCrimson)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = NepalCrimson, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You've viewed all nearby profiles for now! Check back soon.", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Match celebration dialog
    if (matchDialogProfile != null) {
        Dialog(onDismissRequest = { matchDialogProfile = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉 It's a Match!", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NepalCrimson)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You and ${matchDialogProfile!!.name} liked each other! Start chatting on ConnectNP.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { matchDialogProfile = null },
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Send a Message 💕")
                    }
                }
            }
        }
    }
}

@Composable
fun JobsSubSection(
    jobs: List<JobItem>,
    isNepali: Boolean,
    onBack: () -> Unit,
    onApply: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isNepali) "नेपालका रोजगारीहरू (Jobs)" else "ConnectNP Jobs Nepal",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(jobs) { job ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = job.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ConnectNpBlue.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = job.type,
                                    color = ConnectNpBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "${job.company} • ${job.location}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = job.salaryRange, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ConnectNpSuccess)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = job.description, fontSize = 12.sp, lineHeight = 16.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onApply(job.id) },
                            enabled = !job.hasApplied,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (job.hasApplied) ConnectNpSuccess else ConnectNpBlue
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text(
                                text = if (job.hasApplied) "Application Submitted ✅" else "Easy Apply with ConnectNP Profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorDashboardSubSection(
    isNepali: Boolean,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text(text = "Creator Studio & Insights", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estimated Monthly Earnings", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("रु 48,250.00", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ConnectNpSuccess)
                Text("Payout next scheduled on 1st of month via ConnectNP Pay", fontSize = 11.sp)

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Views (30d)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("2.4M 🚀", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column {
                        Text("Engagement Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("8.7% 📈", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column {
                        Text("Followers Gained", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+1,840 👥", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStreamSubSection(
    isNepali: Boolean,
    onBack: () -> Unit
) {
    var giftNotice by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NepalCrimson
                ) {
                    Text(
                        text = "LIVE 🔴 1,420 Viewers",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (giftNotice != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = giftNotice!!,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Text("Live Comments Stream:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text("• Namaste from Kathmandu! 🇳🇵", color = Color.White, fontSize = 13.sp)
            Text("• Beautiful mountain sunset! 🏔️✨", color = Color.White, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Virtual Gifts (Khukuri, Rhododendron, Everest)
            Text("Send Virtual Gifts:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { giftNotice = "🎁 Sent Khukuri Badge (रु 50)!" },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson)
                ) {
                    Text("🗡️ Khukuri (रु 50)")
                }
                Button(
                    onClick = { giftNotice = "🎁 Sent Laligurans Flower (रु 20)!" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                ) {
                    Text("🌺 Laligurans (रु 20)")
                }
                Button(
                    onClick = { giftNotice = "🎁 Sent Everest Peak (रु 100)!" },
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue)
                ) {
                    Text("🏔️ Everest (रु 100)")
                }
            }
        }
    }
}

@Composable
fun SafetySosSubSection(
    isNepali: Boolean,
    onBack: () -> Unit
) {
    var sosSent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text(text = "ConnectNP Safety & SOS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = NepalCrimson, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Emergency Location SOS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Broadcasts your real-time GPS coordinates to trusted emergency contacts and local authorities across Nepal.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { sosSent = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (sosSent) "🚨 SOS Beacon Active!" else "Trigger Emergency SOS", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
