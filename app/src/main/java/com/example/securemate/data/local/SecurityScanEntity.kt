package com.example.securemate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.securemate.domain.model.SecurityScanRecord

@Entity(tableName = "security_scans")
data class SecurityScanEntity(
    @PrimaryKey(autoGenerate = true)
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
) {
    fun toDomain(): SecurityScanRecord = SecurityScanRecord(
        id = id,
        timestamp = timestamp,
        overallScore = overallScore,
        appRiskScore = appRiskScore,
        permissionScore = permissionScore,
        phishingScore = phishingScore,
        deviceScore = deviceScore,
        passwordScore = passwordScore,
        totalAppsScanned = totalAppsScanned,
        highRiskAppsCount = highRiskAppsCount
    )

    companion object {
        fun fromDomain(record: SecurityScanRecord): SecurityScanEntity = SecurityScanEntity(
            id = record.id,
            timestamp = record.timestamp,
            overallScore = record.overallScore,
            appRiskScore = record.appRiskScore,
            permissionScore = record.permissionScore,
            phishingScore = record.phishingScore,
            deviceScore = record.deviceScore,
            passwordScore = record.passwordScore,
            totalAppsScanned = record.totalAppsScanned,
            highRiskAppsCount = record.highRiskAppsCount
        )
    }
}
