package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NepalCrimson

enum class NavigationTab(val labelEn: String, val labelNp: String) {
    FEED("Feed", "फिड"),
    CHATS("Chats", "च्याट"),
    MARKETPLACE("Market", "बजार"),
    PROFILE("Profile", "प्रोफाइल"),
    SETTINGS("Settings", "सेटिङ"),
    REELS("Reels", "रिल्स"),
    WATCH("Watch", "भिडियो"),
    HUB("Services", "सेवाहरू")
}

@Composable
fun ConnectNpBottomBar(
    currentTab: NavigationTab,
    unreadMessagesCount: Int,
    isNepali: Boolean,
    onTabSelected: (NavigationTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        // Feed
        NavigationBarItem(
            selected = currentTab == NavigationTab.FEED,
            onClick = { onTabSelected(NavigationTab.FEED) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavigationTab.FEED) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Feed"
                )
            },
            label = {
                Text(
                    text = if (isNepali) NavigationTab.FEED.labelNp else NavigationTab.FEED.labelEn,
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == NavigationTab.FEED) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_feed")
        )

        // Chats
        NavigationBarItem(
            selected = currentTab == NavigationTab.CHATS,
            onClick = { onTabSelected(NavigationTab.CHATS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadMessagesCount > 0) {
                            Badge(
                                containerColor = NepalCrimson
                            ) {
                                Text(unreadMessagesCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentTab == NavigationTab.CHATS) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Chats"
                    )
                }
            },
            label = {
                Text(
                    text = if (isNepali) NavigationTab.CHATS.labelNp else NavigationTab.CHATS.labelEn,
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == NavigationTab.CHATS) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_chats")
        )

        // Marketplace
        NavigationBarItem(
            selected = currentTab == NavigationTab.MARKETPLACE,
            onClick = { onTabSelected(NavigationTab.MARKETPLACE) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavigationTab.MARKETPLACE) Icons.Filled.Storefront else Icons.Outlined.Storefront,
                    contentDescription = "Marketplace"
                )
            },
            label = {
                Text(
                    text = if (isNepali) NavigationTab.MARKETPLACE.labelNp else NavigationTab.MARKETPLACE.labelEn,
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == NavigationTab.MARKETPLACE) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_market")
        )

        // Profile
        NavigationBarItem(
            selected = currentTab == NavigationTab.PROFILE,
            onClick = { onTabSelected(NavigationTab.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavigationTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            },
            label = {
                Text(
                    text = if (isNepali) NavigationTab.PROFILE.labelNp else NavigationTab.PROFILE.labelEn,
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == NavigationTab.PROFILE) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_profile")
        )

        // Settings
        NavigationBarItem(
            selected = currentTab == NavigationTab.SETTINGS,
            onClick = { onTabSelected(NavigationTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavigationTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = {
                Text(
                    text = if (isNepali) NavigationTab.SETTINGS.labelNp else NavigationTab.SETTINGS.labelEn,
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == NavigationTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_settings")
        )
    }
}
