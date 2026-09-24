package com.example.securemate.security.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.securemate.domain.model.AppSecurityInfo
import com.example.securemate.domain.model.PermissionClassification
import com.example.securemate.domain.model.PermissionInfo

object PermissionAnalyzer {

    data class PermissionMetadata(
        val permission: String,
        val simpleName: String,
        val classification: PermissionClassification,
        val description: String,
        val riskExplanation: String,
        val recommendedAction: String,
        val riskWeight: Int,
        val settingsIntentAction: String? = null
    )

    val KNOWN_PERMISSIONS = listOf(
        PermissionMetadata(
            permission = Manifest.permission.RECORD_AUDIO,
            simpleName = "Microphone",
            classification = PermissionClassification.SENSITIVE,
            description = "Allows the application to record audio using the device microphone.",
            riskExplanation = "Microphone access can expose private conversations, ambient audio, and background acoustic environments.",
            recommendedAction = "Review applications that do not clearly require voice or audio recording.",
            riskWeight = 15
        ),
        PermissionMetadata(
            permission = Manifest.permission.CAMERA,
            simpleName = "Camera",
            classification = PermissionClassification.SENSITIVE,
            description = "Allows the application to capture photos and videos via the device camera.",
            riskExplanation = "Camera access allows recording visual surroundings, facial geometry, and documents.",
            recommendedAction = "Ensure only trusted camera, video call, or photo editing apps have this permission.",
            riskWeight = 12
        ),
        PermissionMetadata(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            simpleName = "Precise Location",
            classification = PermissionClassification.SENSITIVE,
            description = "Allows accessing high-precision GPS coordinates of the device.",
            riskExplanation = "Can be used to trace physical whereabouts, residences, workplaces, and daily routines.",
            recommendedAction = "Switch apps to 'Approximate Location' or 'Only while using the app' if exact GPS isn't needed.",
            riskWeight = 14
        ),
        PermissionMetadata(
            permission = Manifest.permission.READ_CONTACTS,
            simpleName = "Contacts",
            classification = PermissionClassification.DANGEROUS,
            description = "Allows reading the user's contacts data including names, phones, and emails.",
            riskExplanation = "Exposes your personal address book and social graph to third-party developers.",
            recommendedAction = "Revoke from apps that do not strictly need contacts for primary features.",
            riskWeight = 12
        ),
        PermissionMetadata(
            permission = Manifest.permission.READ_SMS,
            simpleName = "SMS (Read)",
            classification = PermissionClassification.DANGEROUS,
            description = "Allows reading SMS messages stored on the phone or SIM card.",
            riskExplanation = "High-risk vector: SMS often carries two-factor authentication (2FA) one-time codes and bank alerts.",
            recommendedAction = "Only your default messaging app should generally have SMS access.",
            riskWeight = 18
        ),
        PermissionMetadata(
            permission = Manifest.permission.SEND_SMS,
            simpleName = "SMS (Send)",
            classification = PermissionClassification.DANGEROUS,
            description = "Allows sending SMS messages on behalf of the user.",
            riskExplanation = "Can result in unexpected telecom charges or fraudulent background subscription messages.",
            recommendedAction = "Revoke from non-messaging applications immediately.",
            riskWeight = 16
        ),
        PermissionMetadata(
            permission = Manifest.permission.READ_PHONE_STATE,
            simpleName = "Phone State",
            classification = PermissionClassification.DANGEROUS,
            description = "Allows read access to the phone state, cellular network info, and ongoing calls.",
            riskExplanation = "Historically used to extract device identifiers (IMEI/IMSI) and monitor call states.",
            recommendedAction = "Review and disable if the application does not manage incoming calls.",
            riskWeight = 10
        ),
        PermissionMetadata(
            permission = Manifest.permission.SYSTEM_ALERT_WINDOW,
            simpleName = "Draw Over Other Apps",
            classification = PermissionClassification.SPECIAL,
            description = "Allows displaying floating windows on top of any running application.",
            riskExplanation = "Can be exploited for tapjacking, phishing screen overlays, or concealing malicious actions.",
            recommendedAction = "Grant only to trusted system tools or picture-in-picture media apps.",
            riskWeight = 20,
            settingsIntentAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
        ),
        PermissionMetadata(
            permission = Manifest.permission.READ_EXTERNAL_STORAGE,
            simpleName = "Storage",
            classification = PermissionClassification.DANGEROUS,
            description = "Allows reading files, photos, downloads, and documents from external storage.",
            riskExplanation = "Grants access to downloaded sensitive documents, personal media, and saved files.",
            recommendedAction = "Use modern scoped storage or the Android Photo Picker instead of broad storage.",
            riskWeight = 10
        ),
        PermissionMetadata(
            permission = Manifest.permission.BODY_SENSORS,
            simpleName = "Body Sensors",
            classification = PermissionClassification.DANGEROUS,
            description = "Allows accessing data from body sensors (e.g. heart rate monitors).",
            riskExplanation = "Exposes biometric and physiological health signals.",
            recommendedAction = "Restrict strictly to dedicated fitness and healthcare applications.",
            riskWeight = 8
        ),
        PermissionMetadata(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            simpleName = "Notifications",
            classification = PermissionClassification.NORMAL,
            description = "Allows posting notifications in the system status bar.",
            riskExplanation = "Low security risk, but can be misused for promotional spam or misleading alert messages.",
            recommendedAction = "Disable for noisy or unwanted advertising applications.",
            riskWeight = 2
        )
    )

    /**
     * Groups scanned applications by permission
     */
    fun analyzePermissions(apps: List<AppSecurityInfo>): List<PermissionInfo> {
        val userApps = apps.filter { !it.isSystemApp }

        return KNOWN_PERMISSIONS.map { meta ->
            val matchingApps = userApps.filter { app ->
                app.requestedPermissions.contains(meta.permission)
            }.map { it.appName }

            PermissionInfo(
                name = meta.permission,
                simpleName = meta.simpleName,
                classification = meta.classification,
                description = meta.description,
                riskExplanation = meta.riskExplanation,
                riskWeight = meta.riskWeight,
                appsUsingCount = matchingApps.size,
                apps = matchingApps
            )
        }.sortedByDescending { it.appsUsingCount * it.riskWeight }
    }

    /**
     * Creates an intent to review permission in system settings.
     * Respects Android platform capabilities.
     */
    fun createReviewIntent(context: Context, permission: String, packageName: String? = null): Intent {
        if (packageName != null) {
            return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // Special permissions
        if (permission == Manifest.permission.SYSTEM_ALERT_WINDOW) {
            return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // General app settings
        return Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
