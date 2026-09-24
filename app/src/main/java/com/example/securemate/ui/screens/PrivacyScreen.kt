package com.example.securemate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.securemate.ui.components.SecurityCard
import com.example.securemate.ui.theme.CyanPrimary
import com.example.securemate.ui.theme.SecurityGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Trust", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("privacy_screen")
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Banner
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SecurityGreen.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecurityGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Privacy-First Architecture",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "SecureMate does not monetize, harvest, or transmit your device data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Pillar 1: Zero Telemetry
            item {
                PrivacyPillarCard(
                    title = "1. Zero Telemetry & No Ad Tracking",
                    description = "SecureMate contains zero analytics trackers, advertising identifiers, or cloud telemetry. Audit operations are executed directly on your phone hardware.",
                    icon = Icons.Default.Lock,
                    iconColor = CyanPrimary
                )
            }

            // Pillar 2: Ephemeral Passwords
            item {
                PrivacyPillarCard(
                    title = "2. Ephemeral Password Processing",
                    description = "When you test a password in the Password Strength tool, it is held strictly in temporary RAM. It is never written to Room database tables, preferences, or device logs. The memory state is explicitly zeroed out when you leave the screen.",
                    icon = Icons.Default.Key,
                    iconColor = SecurityGreen
                )
            }

            // Pillar 3: On-Device Heuristic Phishing
            item {
                PrivacyPillarCard(
                    title = "3. Local Heuristic Phishing Inspection",
                    description = "Suspicious links are analyzed on-device against local heuristics (punycode, raw IP hosts, deceptive TLDs, and path obfuscation). Your browsing inquiries are not shipped to third-party query services.",
                    icon = Icons.Default.Link,
                    iconColor = CyanPrimary
                )
            }

            // Pillar 4: Android Permissions Transparency
            item {
                SecurityCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyanPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Android Permissions We Request",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        PermissionExplanationItem(
                            permission = "QUERY_ALL_PACKAGES",
                            purpose = "Required strictly to audit installed app manifests for target SDK versions, requested permissions, and debug flags to calculate application risk scores."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PermissionExplanationItem(
                            permission = "POST_NOTIFICATIONS",
                            purpose = "Optional. Used exclusively for local background audit reminders and security patch alerts."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PermissionExplanationItem(
                            permission = "INTERNET",
                            purpose = "Allows testing connectivity state and potential future reputation lookups. No personal data or scan history is ever sent over the network."
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyPillarCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color
) {
    SecurityCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PermissionExplanationItem(
    permission: String,
    purpose: String
) {
    Column {
        Text(
            text = permission,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = purpose,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
