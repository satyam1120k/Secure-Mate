package com.example.securemate.security.recommendations

import com.example.securemate.domain.model.AppSecurityInfo
import com.example.securemate.domain.model.DeviceSecurityInfo
import com.example.securemate.domain.model.RecommendationPriority
import com.example.securemate.domain.model.RiskLevel
import com.example.securemate.domain.model.SecurityRecommendation

object SecurityRecommendationEngine {

    fun generateRecommendations(
        apps: List<AppSecurityInfo>,
        deviceInfo: DeviceSecurityInfo
    ): List<SecurityRecommendation> {
        val recommendations = mutableListOf<SecurityRecommendation>()
        val userApps = apps.filter { !it.isSystemApp }

        // 1. Device Lock Screen (Critical / High)
        if (!deviceInfo.isKeyguardSecure) {
            recommendations.add(
                SecurityRecommendation(
                    id = "rec_lock_screen",
                    title = "Set Up Screen Lock & Biometrics",
                    priority = RecommendationPriority.HIGH,
                    reason = "Your device currently has no screen lock or biometrics configured. Physical access exposes all data and unencrypted keystore items.",
                    action = "Open Security Settings and configure a PIN, pattern, or fingerprint.",
                    relatedScreenRoute = "checklist"
                )
            )
        }

        // 2. High Risk Applications (High)
        val highRiskApps = userApps.filter { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
        if (highRiskApps.isNotEmpty()) {
            val appNames = highRiskApps.take(3).joinToString(", ") { it.appName }
            recommendations.add(
                SecurityRecommendation(
                    id = "rec_high_risk_apps",
                    title = "Review ${highRiskApps.size} High-Risk Application${if (highRiskApps.size > 1) "s" else ""}",
                    priority = RecommendationPriority.HIGH,
                    reason = "Applications ($appNames) request sensitive permissions or exhibit elevated risk indicators.",
                    action = "Inspect permissions in the Apps menu and uninstall if unneeded.",
                    relatedScreenRoute = "apps"
                )
            )
        }

        // 3. Audio & Microphone Access (Medium)
        val micApps = userApps.filter { it.requestedPermissions.contains("android.permission.RECORD_AUDIO") }
        if (micApps.size >= 3) {
            recommendations.add(
                SecurityRecommendation(
                    id = "rec_microphone",
                    title = "Review Microphone Permissions",
                    priority = RecommendationPriority.MEDIUM,
                    reason = "${micApps.size} installed user applications have requested microphone recording permission.",
                    action = "Audit which apps legitimately require microphone access.",
                    relatedScreenRoute = "permissions"
                )
            )
        }

        // 4. SMS & Call Log Access (Medium/High)
        val smsApps = userApps.filter {
            it.requestedPermissions.any { perm -> perm.contains("SMS") }
        }
        if (smsApps.isNotEmpty()) {
            recommendations.add(
                SecurityRecommendation(
                    id = "rec_sms",
                    title = "Audit SMS & 2FA Access",
                    priority = RecommendationPriority.HIGH,
                    reason = "${smsApps.size} non-system application(s) request SMS access, which can intercept verification codes.",
                    action = "Ensure only your primary SMS app has SMS permissions.",
                    relatedScreenRoute = "permissions"
                )
            )
        }

        // 5. USB Debugging & Developer Options (Medium)
        if (deviceInfo.isAdbEnabled || deviceInfo.isDeveloperOptionsEnabled) {
            recommendations.add(
                SecurityRecommendation(
                    id = "rec_dev_options",
                    title = "Disable USB Debugging & Dev Options",
                    priority = RecommendationPriority.MEDIUM,
                    reason = "USB debugging permits arbitrary command execution and APK installation via connected computers.",
                    action = "Turn off USB Debugging in Developer Options when not testing.",
                    relatedScreenRoute = "device"
                )
            )
        }

        // 6. Security Patch Update (Medium / High)
        val patchAge = deviceInfo.patchAgeDays
        if (patchAge != null && patchAge > 90) {
            val priority = if (patchAge > 180) RecommendationPriority.HIGH else RecommendationPriority.MEDIUM
            recommendations.add(
                SecurityRecommendation(
                    id = "rec_patch_update",
                    title = "Install Latest Android Security Update",
                    priority = priority,
                    reason = "Last security patch was issued $patchAge days ago (${deviceInfo.securityPatch}). Known OS vulnerabilities may remain unpatched.",
                    action = "Check for system updates in Device Settings.",
                    relatedScreenRoute = "checklist"
                )
            )
        }

        // 7. Legacy Target SDK (Low / Medium)
        val legacyApps = userApps.filter { it.targetSdk != null && it.targetSdk < 31 }
        if (legacyApps.isNotEmpty()) {
            recommendations.add(
                SecurityRecommendation(
                    id = "rec_legacy_apps",
                    title = "Review ${legacyApps.size} Outdated Application${if (legacyApps.size > 1) "s" else ""}",
                    priority = RecommendationPriority.LOW,
                    reason = "These apps target older Android versions and bypass modern privacy indicators and scoped storage protections.",
                    action = "Check Google Play Store for updated versions.",
                    relatedScreenRoute = "apps"
                )
            )
        }

        // 8. General Hygiene Baseline (Low)
        recommendations.add(
            SecurityRecommendation(
                id = "rec_passwords",
                title = "Verify Password Hygiene & Complexity",
                priority = RecommendationPriority.LOW,
                reason = "Credential stuffing and weak passwords remain the top vector for personal account breaches.",
                action = "Use the Password Strength Tool to evaluate and generate high-entropy passwords.",
                relatedScreenRoute = "passwords"
            )
        )

        return recommendations.sortedBy { it.priority }
    }
}
