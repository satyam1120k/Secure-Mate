package com.example.securemate.data.repository

import android.content.Context
import com.example.securemate.domain.model.AppSecurityInfo
import com.example.securemate.domain.model.DeviceSecurityInfo
import com.example.securemate.domain.model.PermissionInfo
import com.example.securemate.domain.model.RiskLevel
import com.example.securemate.domain.model.SecurityChecklistItem
import com.example.securemate.domain.model.SecurityRecommendation
import com.example.securemate.domain.model.SecurityScanRecord
import com.example.securemate.domain.model.SecurityScore
import com.example.securemate.security.apps.AppSecurityAnalyzer
import com.example.securemate.security.device.DeviceSecurityInspector
import com.example.securemate.security.permissions.PermissionAnalyzer
import com.example.securemate.security.recommendations.SecurityRecommendationEngine
import com.example.securemate.security.risk.SecurityRiskEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AssessmentState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val currentScanStage: String = "",
    val securityScore: SecurityScore? = null,
    val scannedApps: List<AppSecurityInfo> = emptyList(),
    val analyzedPermissions: List<PermissionInfo> = emptyList(),
    val deviceInfo: DeviceSecurityInfo? = null,
    val checklistItems: List<SecurityChecklistItem> = emptyList(),
    val recommendations: List<SecurityRecommendation> = emptyList(),
    val lastScanTime: Long? = null
)

class SecurityAssessmentRepository(
    private val context: Context,
    private val scanHistoryRepository: SecurityScanRepository
) {
    private val appAnalyzer = AppSecurityAnalyzer(context)
    private val deviceInspector = DeviceSecurityInspector(context)

    private val _assessmentState = MutableStateFlow(AssessmentState())
    val assessmentState: StateFlow<AssessmentState> = _assessmentState.asStateFlow()

    suspend fun performFullScan(
        onProgressUpdate: ((stage: String, progress: Float) -> Unit)? = null
    ): SecurityScore = withContext(Dispatchers.IO) {
        _assessmentState.value = _assessmentState.value.copy(
            isScanning = true,
            scanProgress = 0.05f,
            currentScanStage = "Scanning installed applications..."
        )
        onProgressUpdate?.invoke("Scanning installed applications...", 0.1f)

        // Stage 1: Applications
        val apps = appAnalyzer.getInstalledAppsSecurity()
        _assessmentState.value = _assessmentState.value.copy(
            scanProgress = 0.35f,
            scannedApps = apps,
            currentScanStage = "Analyzing granted permissions..."
        )
        onProgressUpdate?.invoke("Analyzing granted permissions...", 0.35f)

        // Stage 2: Permissions
        val permissions = PermissionAnalyzer.analyzePermissions(apps)
        _assessmentState.value = _assessmentState.value.copy(
            scanProgress = 0.55f,
            analyzedPermissions = permissions,
            currentScanStage = "Inspecting device security configuration..."
        )
        onProgressUpdate?.invoke("Inspecting device security configuration...", 0.55f)

        // Stage 3: Device Security
        val deviceInfo = deviceInspector.inspectDevice()
        val checklist = deviceInspector.generateSecurityChecklist(deviceInfo)
        _assessmentState.value = _assessmentState.value.copy(
            scanProgress = 0.75f,
            deviceInfo = deviceInfo,
            checklistItems = checklist,
            currentScanStage = "Calculating composite risk score..."
        )
        onProgressUpdate?.invoke("Calculating composite risk score...", 0.75f)

        // Stage 4: Risk Scoring & Recommendations
        val score = SecurityRiskEngine.calculateOverallSecurityScore(
            apps = apps,
            deviceInfo = deviceInfo
        )
        val recommendations = SecurityRecommendationEngine.generateRecommendations(
            apps = apps,
            deviceInfo = deviceInfo
        )

        val timestamp = System.currentTimeMillis()

        // Stage 5: Save to Room DB
        val highRiskCount = apps.count { !it.isSystemApp && (it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL) }
        val scanRecord = SecurityScanRecord(
            timestamp = timestamp,
            overallScore = score.overallScore,
            appRiskScore = score.appScore,
            permissionScore = score.permissionScore,
            phishingScore = score.phishingScore,
            deviceScore = score.deviceScore,
            passwordScore = score.passwordScore,
            totalAppsScanned = apps.size,
            highRiskAppsCount = highRiskCount
        )
        scanHistoryRepository.saveScan(scanRecord)

        _assessmentState.value = _assessmentState.value.copy(
            isScanning = false,
            scanProgress = 1.0f,
            currentScanStage = "Scan completed successfully",
            securityScore = score,
            scannedApps = apps,
            analyzedPermissions = permissions,
            deviceInfo = deviceInfo,
            checklistItems = checklist,
            recommendations = recommendations,
            lastScanTime = timestamp
        )
        onProgressUpdate?.invoke("Scan completed", 1.0f)

        score
    }

    suspend fun getOrRunInitialAssessment(): AssessmentState {
        if (_assessmentState.value.securityScore == null && !_assessmentState.value.isScanning) {
            performFullScan()
        }
        return _assessmentState.value
    }
}
