package com.example.securemate

import android.content.Context
import com.example.securemate.data.local.AppDatabase
import com.example.securemate.data.local.SecurityPreferences
import com.example.securemate.data.repository.SecurityAssessmentRepository
import com.example.securemate.data.repository.SecurityScanRepository
import com.example.securemate.data.repository.SecurityScanRepositoryImpl
import com.example.securemate.security.phishing.LocalHeuristicPhishingDetector
import com.example.securemate.security.phishing.UrlReputationService

class SecureMateAppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val scanHistoryRepository: SecurityScanRepository by lazy {
        SecurityScanRepositoryImpl(database.securityScanDao())
    }

    val securityPreferences: SecurityPreferences by lazy {
        SecurityPreferences(context)
    }

    val assessmentRepository: SecurityAssessmentRepository by lazy {
        SecurityAssessmentRepository(context, scanHistoryRepository)
    }

    val phishingDetector: UrlReputationService by lazy {
        LocalHeuristicPhishingDetector()
    }
}
