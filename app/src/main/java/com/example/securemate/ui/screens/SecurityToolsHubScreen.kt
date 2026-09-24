package com.example.securemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.securemate.ui.navigation.Screen
import com.example.securemate.ui.theme.CyanAccent
import com.example.securemate.ui.theme.CyanPrimary
import com.example.securemate.ui.theme.SecurityAmber
import com.example.securemate.ui.theme.SecurityGreen
import com.example.securemate.ui.theme.SecurityOrange
import com.example.securemate.ui.theme.SecurityPurple

@Composable
fun SecurityToolsHubScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("security_tools_hub_screen")
    ) {
        item {
            Column {
                Text(
                    text = "Security Hub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tools and diagnostics to audit your mobile cyber posture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ToolHubCard(
                title = "App Security Auditor",
                description = "Inspect installed apps, risk evaluations, and manifest privileges.",
                icon = Icons.Default.Apps,
                iconColor = CyanPrimary,
                onClick = { onNavigate(Screen.AppList.route) }
            )
        }

        item {
            ToolHubCard(
                title = "Permission Auditor",
                description = "Audit high-risk grants including Camera, Microphone, SMS, and Location.",
                icon = Icons.Default.Security,
                iconColor = SecurityAmber,
                onClick = { onNavigate(Screen.Permissions.route) }
            )
        }

        item {
            ToolHubCard(
                title = "Phishing URL Analyzer",
                description = "Analyze suspicious links locally for punycode, raw IPs, and deceptive TLDs.",
                icon = Icons.Default.Link,
                iconColor = SecurityOrange,
                onClick = { onNavigate(Screen.Phishing.route) }
            )
        }

        item {
            ToolHubCard(
                title = "Password Hygiene & Generator",
                description = "Zero-knowledge entropy calculation and cryptographically secure generator.",
                icon = Icons.Default.Key,
                iconColor = SecurityGreen,
                onClick = { onNavigate(Screen.Passwords.route) }
            )
        }

        item {
            ToolHubCard(
                title = "Device Hardening Checklist",
                description = "Verify lock screens, keystore protection, and attack surface configurations.",
                icon = Icons.Default.Checklist,
                iconColor = CyanAccent,
                onClick = { onNavigate(Screen.Checklist.route) }
            )
        }

        item {
            ToolHubCard(
                title = "Hardware & OS Specs",
                description = "Inspect Android release, security patch age, and root heuristic signals.",
                icon = Icons.Default.PhoneAndroid,
                iconColor = SecurityPurple,
                onClick = { onNavigate(Screen.DeviceSecurity.route) }
            )
        }

        item {
            ToolHubCard(
                title = "Audit History & Trends",
                description = "Review historical score timelines and past scan logs.",
                icon = Icons.Default.History,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onNavigate(Screen.History.route) }
            )
        }
    }
}

@Composable
fun ToolHubCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
