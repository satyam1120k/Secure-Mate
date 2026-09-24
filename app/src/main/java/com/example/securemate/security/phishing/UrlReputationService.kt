package com.example.securemate.security.phishing

import com.example.securemate.domain.model.PhishingAnalysisResult

interface UrlReputationService {
    suspend fun checkUrl(url: String): PhishingAnalysisResult
}
