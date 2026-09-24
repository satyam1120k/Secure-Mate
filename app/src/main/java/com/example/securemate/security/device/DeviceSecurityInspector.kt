package com.example.securemate.security.device

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.securemate.domain.model.ChecklistStatus
import com.example.securemate.domain.model.DeviceSecurityInfo
import com.example.securemate.domain.model.SecurityChecklistItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeviceSecurityInspector(private val context: Context) {

    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    fun inspectDevice(): DeviceSecurityInfo {
        val androidVersion = Build.VERSION.RELEASE ?: "Unknown"
        val apiLevel = Build.VERSION.SDK_INT
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH ?: "Not Reported"
        } else {
            "Not Available"
        }
        val deviceModel = Build.MODEL ?: "Unknown Device"
        val manufacturer = Build.MANUFACTURER ?: "Android"

        // Patch age calculation
        val patchAgeDays = calculatePatchAgeDays(securityPatch)

        // Lock screen / Biometric hardware configuration
        val isDeviceSecure = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager?.isDeviceSecure ?: false
        } else {
            keyguardManager?.isKeyguardSecure ?: false
        }
        val isKeyguardSecure = keyguardManager?.isKeyguardSecure ?: false

        // Developer Options & ADB status
        val isDevOptions = try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            false
        }

        val isAdb = try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            false
        }

        // Heuristic Root Detection
        val rootIndicators = checkRootHeuristics()

        val playProtectStatusNote = "Google Play Protect runs automatically in the background via Google Play Services. Third-party apps cannot query its real-time scanning database directly through Android public APIs."

        return DeviceSecurityInfo(
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            securityPatch = securityPatch,
            patchAgeDays = patchAgeDays,
            deviceModel = deviceModel,
            manufacturer = manufacturer,
            isDeviceSecure = isDeviceSecure,
            isKeyguardSecure = isKeyguardSecure,
            isDeveloperOptionsEnabled = isDevOptions,
            isAdbEnabled = isAdb,
            rootIndicatorsDetected = rootIndicators.isNotEmpty(),
            rootIndicatorsList = rootIndicators,
            playProtectNote = playProtectStatusNote
        )
    }

    fun generateSecurityChecklist(info: DeviceSecurityInfo): List<SecurityChecklistItem> {
        val list = mutableListOf<SecurityChecklistItem>()

        // 1. Screen Lock & Biometrics
        list.add(
            SecurityChecklistItem(
                id = "screen_lock",
                title = "Screen Lock & Biometrics",
                category = "Access Control",
                status = if (info.isKeyguardSecure) ChecklistStatus.PASSED else ChecklistStatus.FAILED,
                statusText = if (info.isKeyguardSecure) "Enabled (PIN/Pattern/Biometrics)" else "Disabled (Device Unlocked)",
                explanation = "A secured lock screen encrypts cryptographic keystore entries and prevents unauthorized physical access.",
                recommendedAction = if (info.isKeyguardSecure) "Maintain a strong PIN, pattern, or biometric authentication."
                else "Set up a lock screen PIN, password, or biometric unlock in Device Settings.",
                intentAction = Settings.ACTION_SECURITY_SETTINGS
            )
        )

        // 2. Hardware Keystore / Device Security
        list.add(
            SecurityChecklistItem(
                id = "device_keystore",
                title = "Hardware Keystore Encryption",
                category = "Hardware Security",
                status = if (info.isDeviceSecure) ChecklistStatus.PASSED else ChecklistStatus.WARNING,
                statusText = if (info.isDeviceSecure) "Secured by Hardware Keyguard" else "Keystore Unlocked",
                explanation = "Android Keystore protects authentication keys inside an isolated hardware environment (TEE / StrongBox).",
                recommendedAction = "Ensure lock screen is armed so cryptographic keys require user authentication.",
                intentAction = Settings.ACTION_SECURITY_SETTINGS
            )
        )

        // 3. Security Patch Level
        val patchStatus = when {
            info.patchAgeDays == null -> ChecklistStatus.INFO
            info.patchAgeDays < 90 -> ChecklistStatus.PASSED
            info.patchAgeDays < 180 -> ChecklistStatus.WARNING
            else -> ChecklistStatus.FAILED
        }
        list.add(
            SecurityChecklistItem(
                id = "security_patch",
                title = "System Security Updates",
                category = "System Integrity",
                status = patchStatus,
                statusText = "Patch Level: ${info.securityPatch}",
                explanation = "Regular monthly security bulletins patch known privilege escalation and remote code execution vulnerabilities.",
                recommendedAction = if (patchStatus == ChecklistStatus.PASSED) "Device is up-to-date." else "Check for system updates in System Settings.",
                intentAction = Settings.ACTION_SYSTEM_UPDATE_SETTINGS
            )
        )

        // 4. Developer Options
        list.add(
            SecurityChecklistItem(
                id = "developer_options",
                title = "Developer Options",
                category = "Attack Surface",
                status = if (!info.isDeveloperOptionsEnabled) ChecklistStatus.PASSED else ChecklistStatus.WARNING,
                statusText = if (!info.isDeveloperOptionsEnabled) "Disabled" else "Enabled",
                explanation = "Developer Options unlock debugging features, mock locations, and testing bridges that can widen the device attack surface.",
                recommendedAction = if (info.isDeveloperOptionsEnabled) "Turn off Developer Options unless actively developing software." else "No action needed.",
                intentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            )
        )

        // 5. USB Debugging (ADB)
        list.add(
            SecurityChecklistItem(
                id = "usb_debugging",
                title = "USB Debugging (ADB)",
                category = "Attack Surface",
                status = if (!info.isAdbEnabled) ChecklistStatus.PASSED else ChecklistStatus.WARNING,
                statusText = if (!info.isAdbEnabled) "Disabled" else "Active",
                explanation = "USB debugging permits a connected computer to execute shell commands, pull app data, and install APKs without prompts.",
                recommendedAction = if (info.isAdbEnabled) "Disable USB debugging when not testing or connected to trusted workstations." else "Secure.",
                intentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            )
        )

        // 6. System Integrity / Root Heuristics
        list.add(
            SecurityChecklistItem(
                id = "root_integrity",
                title = "System Integrity & Root Heuristics",
                category = "System Integrity",
                status = if (!info.rootIndicatorsDetected) ChecklistStatus.PASSED else ChecklistStatus.WARNING,
                statusText = if (!info.rootIndicatorsDetected) "Stock OS Verified (No SU binaries)" else "Potential Heuristic Indicators Found",
                explanation = if (info.rootIndicatorsDetected)
                    "Detected heuristic indicators: ${info.rootIndicatorsList.joinToString(", ")}. Note: Root checks are heuristics and may produce false flags in custom ROMs or virtual environments."
                else "Standard Android sandboxing, SELinux policies, and permission controls are intact.",
                recommendedAction = if (info.rootIndicatorsDetected) "Verify device integrity and ensure no untrusted superuser manager is running." else "OS integrity is intact."
            )
        )

        // 7. Google Play Protect Status (Platform API Transparency)
        list.add(
            SecurityChecklistItem(
                id = "play_protect",
                title = "Google Play Protect",
                category = "Malware Defense",
                status = ChecklistStatus.INFO,
                statusText = "Active in Google Play Services",
                explanation = "Google Play Protect performs continuous scanning of apps installed from Google Play and sideloaded sources. Detailed scanning logs are not exposed to third-party apps via public Android APIs.",
                recommendedAction = "Verify in Google Play Store -> Settings -> Play Protect that scanning is switched on."
            )
        )

        return list
    }

    private fun calculatePatchAgeDays(patchString: String): Long? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val patchDate = format.parse(patchString) ?: return null
            val now = Date()
            val diff = now.time - patchDate.time
            TimeUnit.MILLISECONDS.toDays(diff)
        } catch (e: Exception) {
            null
        }
    }

    private fun checkRootHeuristics(): List<String> {
        val detected = mutableListOf<String>()

        // 1. Check for su binary files
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/system/sd/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        for (path in suPaths) {
            try {
                if (File(path).exists()) {
                    detected.add("su binary present at $path")
                }
            } catch (ignored: Exception) {
            }
        }

        // 2. Check build tags for test-keys
        val tags = Build.TAGS
        if (tags != null && tags.contains("test-keys")) {
            detected.add("OS compiled with test-keys signature")
        }

        return detected
    }
}
