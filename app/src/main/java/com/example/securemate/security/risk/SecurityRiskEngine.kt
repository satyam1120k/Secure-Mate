package com.example.securemate.security.risk

import com.example.securemate.domain.model.AppSecurityInfo
import com.example.securemate.domain.model.DeviceSecurityInfo
import com.example.securemate.domain.model.RiskLevel
import com.example.securemate.domain.model.ScoreCategoryBreakdown
import com.example.securemate.domain.model.SecurityScore
import kotlin.math.max
import kotlin.math.min

object SecurityRiskEngine {

    // Permission Risk Weights
    private val HIGH_RISK_PERMISSIONS = mapOf(
        "android.permission.RECORD_AUDIO" to 12,
        "android.permission.CAMERA" to 10,
        "android.permission.ACCESS_FINE_LOCATION" to 12,
        "android.permission.ACCESS_BACKGROUND_LOCATION" to 15,
        "android.permission.READ_CONTACTS" to 10,
        "android.permission.WRITE_CONTACTS" to 10,
        "android.permission.READ_SMS" to 15,
        "android.permission.SEND_SMS" to 15,
        "android.permission.RECEIVE_SMS" to 15,
        "android.permission.CALL_PHONE" to 10,
        "android.permission.READ_PHONE_STATE" to 10,
        "android.permission.SYSTEM_ALERT_WINDOW" to 18,
        "android.permission.REQUEST_INSTALL_PACKAGES" to 15,
        "android.permission.MANAGE_EXTERNAL_STORAGE" to 12,
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to 20
    )

    /**
     * Evaluates a single installed application and returns an explainable risk score (0-100),
     * risk level, and reasons.
     */
    fun evaluateAppRisk(
        packageName: String,
        appName: String,
        targetSdk: Int?,
        isSystemApp: Boolean,
        isDebuggable: Boolean,
        requestedPermissions: List<String>
    ): AppRiskEvaluation {
        var score = 0
        val reasons = mutableListOf<String>()
        val dangerousPermissions = mutableListOf<String>()

        // 1. Check dangerous & sensitive permissions
        for (perm in requestedPermissions) {
            val weight = HIGH_RISK_PERMISSIONS[perm]
            if (weight != null) {
                dangerousPermissions.add(perm)
                score += weight
                val simpleName = perm.substringAfterLast(".")
                reasons.add("Requests sensitive permission: $simpleName (+${weight} pts)")
            }
        }

        // 2. Check permission combinations
        val hasMic = requestedPermissions.contains("android.permission.RECORD_AUDIO")
        val hasLoc = requestedPermissions.contains("android.permission.ACCESS_FINE_LOCATION") ||
                requestedPermissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION")
        val hasSms = requestedPermissions.any { it.contains("SMS") }
        val hasContacts = requestedPermissions.any { it.contains("CONTACTS") }
        val hasOverlay = requestedPermissions.contains("android.permission.SYSTEM_ALERT_WINDOW")
        val hasInstall = requestedPermissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")

        if (hasMic && hasLoc) {
            score += 15
            reasons.add("Potentially sensitive combination: Audio recording and Location access (+15 pts)")
        }
        if (hasSms && hasContacts) {
            score += 15
            reasons.add("Sensitive combination: SMS transmission and Contacts access (+15 pts)")
        }
        if (hasOverlay && hasInstall) {
            score += 20
            reasons.add("Elevated privilege pattern: Screen overlay and package installation capabilities (+20 pts)")
        }

        // 3. Target SDK checks
        if (targetSdk != null) {
            if (targetSdk < 28) {
                score += 20
                reasons.add("Targets legacy Android SDK $targetSdk (Android 8 or older, misses runtime sandbox protections) (+20 pts)")
            } else if (targetSdk < 31) {
                score += 10
                reasons.add("Targets older Android SDK $targetSdk (misses Android 12+ privacy dashboard & indicators) (+10 pts)")
            }
        }

        // 4. Debuggable flag check
        if (isDebuggable) {
            score += 20
            reasons.add("Application is compiled with android:debuggable=true (allows memory inspection) (+20 pts)")
        }

        // System app dampening: trusted pre-installed packages have slightly lower risk profile
        if (isSystemApp) {
            score = (score * 0.65).toInt()
        }

        val finalScore = min(100, max(0, score))
        val riskLevel = RiskLevel.fromScore(finalScore)

        if (reasons.isEmpty()) {
            reasons.add("✓ No significant risk indicators or sensitive permissions detected.")
        }

        return AppRiskEvaluation(
            riskScore = finalScore,
            riskLevel = riskLevel,
            dangerousPermissions = dangerousPermissions,
            riskReasons = reasons
        )
    }

    /**
     * Calculates the overall composite security score (0-100) and breakdown
     */
    fun calculateOverallSecurityScore(
        apps: List<AppSecurityInfo>,
        deviceInfo: DeviceSecurityInfo,
        passwordHygieneScore: Int = 100,
        phishingAwarenessScore: Int = 85
    ): SecurityScore {
        val userApps = apps.filter { !it.isSystemApp }
        val highRiskAppsCount = userApps.count { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
        val moderateRiskAppsCount = userApps.count { it.riskLevel == RiskLevel.MODERATE }

        // 1. Applications Score (Max: 20 pts)
        val maxAppScore = 20
        var appScoreDeduction = (highRiskAppsCount * 5) + (moderateRiskAppsCount * 2)
        val appScore = max(0, maxAppScore - appScoreDeduction)

        // 2. Permissions Score (Max: 20 pts)
        val maxPermScore = 20
        val totalSensitivePerms = userApps.sumOf { it.dangerousPermissions.size }
        val permDeduction = min(maxPermScore, (totalSensitivePerms / 2))
        val permScore = max(0, maxPermScore - permDeduction)

        // 3. Device Security Score (Max: 25 pts)
        val maxDeviceScore = 25
        var devScore = maxDeviceScore
        if (!deviceInfo.isKeyguardSecure) devScore -= 12
        if (!deviceInfo.isDeviceSecure) devScore -= 5
        if (deviceInfo.rootIndicatorsDetected) devScore -= 10
        if (deviceInfo.isDeveloperOptionsEnabled) devScore -= 3
        if (deviceInfo.isAdbEnabled) devScore -= 2
        val deviceScore = max(0, devScore)

        // 4. Update / Patch Score (Max: 10 pts)
        val maxUpdateScore = 10
        var updateScore = maxUpdateScore
        val patchAge = deviceInfo.patchAgeDays
        if (patchAge != null) {
            if (patchAge > 180) updateScore -= 7
            else if (patchAge > 90) updateScore -= 4
        } else {
            // Android version check
            if (deviceInfo.apiLevel < 31) updateScore -= 5
        }
        updateScore = max(0, updateScore)

        // 5. Password Score (Max: 15 pts)
        val maxPasswordScore = 15
        val passwordScore = ((passwordHygieneScore / 100.0) * maxPasswordScore).toInt()

        // 6. Phishing & Network Score (Max: 10 pts)
        val maxPhishingScore = 10
        val phishingScore = ((phishingAwarenessScore / 100.0) * maxPhishingScore).toInt()

        val overall = min(100, max(0, appScore + permScore + deviceScore + updateScore + passwordScore + phishingScore))

        val level = when {
            overall >= 80 -> "Secure"
            overall >= 65 -> "Good"
            overall >= 45 -> "Attention Required"
            else -> "Critical"
        }

        val breakdowns = listOf(
            ScoreCategoryBreakdown(
                category = "Permissions",
                earnedScore = permScore,
                maxScore = maxPermScore,
                statusIcon = if (permScore >= 16) "✓" else if (permScore >= 10) "⚠" else "✕",
                statusText = if (permScore >= 16) "Optimal" else "Review Needed",
                explanation = "$totalSensitivePerms sensitive permission grants found across ${userApps.size} installed user apps."
            ),
            ScoreCategoryBreakdown(
                category = "Applications",
                earnedScore = appScore,
                maxScore = maxAppScore,
                statusIcon = if (appScore >= 16) "✓" else if (appScore >= 10) "⚠" else "✕",
                statusText = if (appScore >= 16) "Healthy" else "Attention",
                explanation = if (highRiskAppsCount > 0) "$highRiskAppsCount high-risk applications detected." else "No critical risk apps identified."
            ),
            ScoreCategoryBreakdown(
                category = "Device Security",
                earnedScore = deviceScore,
                maxScore = maxDeviceScore,
                statusIcon = if (deviceScore >= 20) "✓" else if (deviceScore >= 14) "⚠" else "✕",
                statusText = if (deviceInfo.isKeyguardSecure) "Protected" else "Vulnerable",
                explanation = if (deviceInfo.isKeyguardSecure) "Screen lock and cryptographic keystore active." else "Screen lock or biometrics not configured."
            ),
            ScoreCategoryBreakdown(
                category = "Updates",
                earnedScore = updateScore,
                maxScore = maxUpdateScore,
                statusIcon = if (updateScore >= 8) "✓" else if (updateScore >= 5) "⚠" else "✕",
                statusText = if (updateScore >= 8) "Current" else "Outdated",
                explanation = "Security patch: ${deviceInfo.securityPatch} (API ${deviceInfo.apiLevel})."
            ),
            ScoreCategoryBreakdown(
                category = "Passwords",
                earnedScore = passwordScore,
                maxScore = maxPasswordScore,
                statusIcon = if (passwordScore >= 12) "✓" else "⚠",
                statusText = "Checked",
                explanation = "Password entropy and dictionary security analyzer active."
            ),
            ScoreCategoryBreakdown(
                category = "Phishing",
                earnedScore = phishingScore,
                maxScore = maxPhishingScore,
                statusIcon = if (phishingScore >= 8) "✓" else "⚠",
                statusText = "Active",
                explanation = "Local heuristic URL & punycode detector operational."
            )
        )

        return SecurityScore(
            overallScore = overall,
            scoreLevel = level,
            permissionScore = permScore,
            appScore = appScore,
            passwordScore = passwordScore,
            phishingScore = phishingScore,
            deviceScore = deviceScore,
            updateScore = updateScore,
            breakdown = breakdowns
        )
    }
}

data class AppRiskEvaluation(
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val dangerousPermissions: List<String>,
    val riskReasons: List<String>
)
