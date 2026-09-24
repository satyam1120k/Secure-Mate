package com.example.securemate.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityOrange
import com.example.ui.theme.SecurityRed

enum class RiskLevel(val label: String, val minScore: Int, val maxScore: Int) {
    LOW("Low", 0, 29),
    MODERATE("Moderate", 30, 59),
    HIGH("High", 60, 79),
    CRITICAL("Critical", 80, 100);

    val color: Color
        get() = when (this) {
            LOW -> SecurityGreen
            MODERATE -> SecurityAmber
            HIGH -> SecurityOrange
            CRITICAL -> SecurityRed
        }

    val badgeText: String
        get() = when (this) {
            LOW -> "✓ LOW"
            MODERATE -> "⚠ MODERATE"
            HIGH -> "⚠ HIGH"
            CRITICAL -> "✕ CRITICAL"
        }

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score >= 80 -> CRITICAL
            score >= 60 -> HIGH
            score >= 30 -> MODERATE
            else -> LOW
        }
    }
}

enum class PermissionClassification(val title: String) {
    NORMAL("Normal"),
    DANGEROUS("Dangerous"),
    SENSITIVE("Sensitive"),
    SPECIAL("Special Access")
}

data class PermissionInfo(
    val name: String,
    val simpleName: String,
    val classification: PermissionClassification,
    val description: String,
    val riskExplanation: String,
    val riskWeight: Int,
    val appsUsingCount: Int = 0,
    val apps: List<String> = emptyList()
)

data class AppSecurityInfo(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long?,
    val targetSdk: Int?,
    val minSdk: Int?,
    val isSystemApp: Boolean,
    val isDebuggable: Boolean,
    val requestedPermissions: List<String>,
    val dangerousPermissions: List<String>,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val riskReasons: List<String>,
    val installSource: String? = null
)

data class ScoreCategoryBreakdown(
    val category: String,
    val earnedScore: Int,
    val maxScore: Int,
    val statusIcon: String, // "✓", "⚠", "✕"
    val statusText: String,
    val explanation: String
)

data class SecurityScore(
    val overallScore: Int, // 0 - 100
    val scoreLevel: String, // "Secure", "Good", "Attention Required", "Critical"
    val permissionScore: Int,
    val appScore: Int,
    val passwordScore: Int,
    val phishingScore: Int,
    val deviceScore: Int,
    val updateScore: Int,
    val breakdown: List<ScoreCategoryBreakdown>
) {
    val levelColor: Color
        get() = when {
            overallScore >= 80 -> SecurityGreen
            overallScore >= 65 -> SecurityAmber
            overallScore >= 45 -> SecurityOrange
            else -> SecurityRed
        }
}

data class DeviceSecurityInfo(
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val patchAgeDays: Long?,
    val deviceModel: String,
    val manufacturer: String,
    val isDeviceSecure: Boolean,
    val isKeyguardSecure: Boolean,
    val isDeveloperOptionsEnabled: Boolean,
    val isAdbEnabled: Boolean,
    val rootIndicatorsDetected: Boolean,
    val rootIndicatorsList: List<String>,
    val playProtectNote: String
)

enum class ChecklistStatus(val icon: String, val label: String) {
    PASSED("✓", "Secure"),
    WARNING("⚠", "Attention"),
    FAILED("✕", "Action Required"),
    INFO("ℹ", "Informational")
}

data class SecurityChecklistItem(
    val id: String,
    val title: String,
    val category: String,
    val status: ChecklistStatus,
    val statusText: String,
    val explanation: String,
    val recommendedAction: String,
    val intentAction: String? = null
)

enum class RecommendationPriority(val label: String, val color: Color) {
    HIGH("High Priority", SecurityRed),
    MEDIUM("Medium Priority", SecurityAmber),
    LOW("Low Priority", SecurityGreen)
}

data class SecurityRecommendation(
    val id: String,
    val title: String,
    val priority: RecommendationPriority,
    val reason: String,
    val action: String,
    val relatedScreenRoute: String? = null
)

data class PhishingAnalysisResult(
    val url: String,
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val indicatorsDetected: List<String>,
    val heuristicDetails: List<String>,
    val isHttps: Boolean,
    val domain: String,
    val recommendations: List<String>
)

enum class PasswordStrengthLevel(val label: String, val color: Color) {
    VERY_WEAK("Very Weak", SecurityRed),
    WEAK("Weak", SecurityOrange),
    FAIR("Fair", SecurityAmber),
    STRONG("Strong", SecurityGreen),
    VERY_STRONG("Very Strong", SecurityGreen)
}

data class PasswordStrengthResult(
    val score: Int, // 0-100
    val strengthLevel: PasswordStrengthLevel,
    val entropyBits: Double,
    val feedbackMessages: List<String>,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigits: Boolean,
    val hasSymbols: Boolean,
    val length: Int,
    val commonPatternDetected: Boolean
)

data class SecurityScanRecord(
    val id: Long = 0,
    val timestamp: Long,
    val overallScore: Int,
    val appRiskScore: Int,
    val permissionScore: Int,
    val phishingScore: Int,
    val deviceScore: Int,
    val passwordScore: Int,
    val totalAppsScanned: Int,
    val highRiskAppsCount: Int
)
