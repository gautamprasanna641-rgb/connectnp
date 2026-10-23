package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import com.example.data.ConnectNpRepository
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.ConnectNpFirestoreRepository
import com.example.data.SampleDataProvider
import com.example.model.FirestoreUserProfile
import com.example.model.User
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchDialog(
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onSelectUser: (User) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestoreRepo = remember { ConnectNpFirestoreRepository(context) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var firestoreUsers by remember { mutableStateOf<List<FirestoreUserProfile>>(emptyList()) }
    val followedUserIds = remember { mutableStateListOf<String>() }

    val localUsers = remember { emptyList<User>() }

    // Launch search query whenever searchQuery changes
    LaunchedEffect(searchQuery) {
        isSearching = true
        coroutineScope.launch {
            try {
                val res = firestoreRepo.searchUsers(searchQuery)
                firestoreUsers = res.getOrDefault(emptyList())
            } catch (_: Exception) {
                firestoreUsers = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    // Combine matching local users and firestore users
    val matchedUsers = remember(searchQuery, firestoreUsers, localUsers) {
        val q = searchQuery.trim()
        val combined = mutableListOf<User>()

        // 1. From local sample community
        if (q.isBlank()) {
            combined.addAll(localUsers)
        } else {
            combined.addAll(
                localUsers.filter {
                    it.name.contains(q, ignoreCase = true) || it.handle.contains(q, ignoreCase = true)
                }
            )
        }

        // 2. From Firestore users
        firestoreUsers.forEach { fUser ->
            if (combined.none { it.id == fUser.userId || it.name.equals(fUser.name, ignoreCase = true) }) {
                combined.add(
                    User(
                        id = fUser.userId,
                        name = fUser.name,
                        handle = "@${fUser.handle.removePrefix("@")}",
                        bio = fUser.bio,
                        avatarUrl = fUser.photoUrl.ifBlank { null },
                        isVerified = true,
                        location = "Nepal"
                    )
                )
            }
        }

        combined.distinctBy { it.id }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("user_search_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isNepali) "प्रयोगकर्ता खोज्नुहोस्" else "Search Users",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_search_input"),
                    placeholder = {
                        Text(
                            text = if (isNepali) "नाम वा ह्यान्डल द्वारा खोज्नुहोस्..." else "Search by name or handle...",
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ConnectNpBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ConnectNpBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Loading Indicator
                AnimatedVisibility(visible = isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = ConnectNpBlue
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isNepali) "नतिजाहरू (${matchedUsers.size})" else "Results (${matchedUsers.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // User Results List
                if (matchedUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isNepali) "कुनै प्रयोगकर्ता भेटिएन" else "No users found matching \"$searchQuery\"",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(matchedUsers, key = { it.id }) { user ->
                            val isFollowing = followedUserIds.contains(user.id)
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectUser(user)
                                        onDismiss()
                                    }
                                    .testTag("user_search_result_${user.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(ConnectNpBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!user.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = user.avatarUrl,
                                                contentDescription = user.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = user.name.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Name, Handle, Bio
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (user.isVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    tint = ConnectNpBlue,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = user.handle,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (user.bio.isNotBlank()) {
                                            Text(
                                                text = user.bio,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Follow Button
                                    FilledTonalButton(
                                        onClick = {
                                            if (isFollowing) {
                                                followedUserIds.remove(user.id)
                                            } else {
                                                followedUserIds.add(user.id)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = if (isFollowing) {
                                            ButtonDefaults.filledTonalButtonColors(
                                                containerColor = ConnectNpBlue.copy(alpha = 0.15f),
                                                contentColor = ConnectNpBlue
                                            )
                                        } else {
                                            ButtonDefaults.filledTonalButtonColors(
                                                containerColor = ConnectNpBlue,
                                                contentColor = Color.White
                                            )
                                        },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isFollowing) {
                                                if (isNepali) "पछ्याइयो" else "Following"
                                            } else {
                                                if (isNepali) "पछ्याउनुहोस्" else "Follow"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
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
}
