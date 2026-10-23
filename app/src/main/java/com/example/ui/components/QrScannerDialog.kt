package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.data.ConnectNpRepository
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
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
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onSimulateScanSuccess: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = My Code, 1 = Scan

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("qr_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONNECTNP QR",
                        fontFamily = com.example.ui.theme.PoppinsFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My QR Code", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Scanner", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                if (selectedTab == 0) {
                    // My QR Code
                    val activeUser = ConnectNpRepository.getCurrentActiveUser()
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                3.dp,
                                Brush.linearGradient(listOf(ConnectNpBlue, NepalCrimson)),
                                RoundedCornerShape(16.dp)
                            )
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        // QR Graphic placeholder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Pattern",
                                tint = Color.Black,
                                modifier = Modifier.size(140.dp)
                            )
                            Text(
                                text = "connectnp.me/${activeUser.handle.removePrefix("@")}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = activeUser.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeUser.handle} • Nepal Verified",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* Share */ },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SHARE CONNECTNP PROFILE",
                            fontFamily = com.example.ui.theme.PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    // Simulated Scanner
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Scanner reticle
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .border(2.dp, ConnectNpBlue, RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "Align QR code inside",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onSimulateScanSuccess("member_np")
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simulate Scan (Test QR Code)")
                    }
                }
            }
        }
    }
}
