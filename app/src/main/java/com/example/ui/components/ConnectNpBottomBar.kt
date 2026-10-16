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
    REELS("Reels", "रिल्स"),
    CHATS("Chats", "च्याट"),
    MARKETPLACE("Market", "बजार"),
    WATCH("Watch", "भिडियो"),
    HUB("SuperApp", "सेवाहरू")
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
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == NavigationTab.FEED) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_feed")
        )

        // Reels
        NavigationBarItem(
            selected = currentTab == NavigationTab.REELS,
            onClick = { onTabSelected(NavigationTab.REELS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavigationTab.REELS) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow,
                    contentDescription = "Reels"
                )
            },
            label = {
                Text(
                    text = if (isNepali) NavigationTab.REELS.labelNp else NavigationTab.REELS.labelEn,
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == NavigationTab.REELS) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_reels")
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
                    fontSize = 11.sp,
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
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == NavigationTab.MARKETPLACE) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_market")
        )

        // Watch
        NavigationBarItem(
            selected = currentTab == NavigationTab.WATCH,
            onClick = { onTabSelected(NavigationTab.WATCH) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavigationTab.WATCH) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions,
                    contentDescription = "Watch"
                )
            },
            label = {
                Text(
                    text = if (isNepali) NavigationTab.WATCH.labelNp else NavigationTab.WATCH.labelEn,
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == NavigationTab.WATCH) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_watch")
        )

        // SuperApp Hub
        NavigationBarItem(
            selected = currentTab == NavigationTab.HUB,
            onClick = { onTabSelected(NavigationTab.HUB) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavigationTab.HUB) Icons.Filled.Widgets else Icons.Outlined.Widgets,
                    contentDescription = "Hub"
                )
            },
            label = {
                Text(
                    text = if (isNepali) NavigationTab.HUB.labelNp else NavigationTab.HUB.labelEn,
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == NavigationTab.HUB) FontWeight.Bold else FontWeight.Normal
                )
            },
            modifier = Modifier.testTag("nav_tab_hub")
        )
    }
}
