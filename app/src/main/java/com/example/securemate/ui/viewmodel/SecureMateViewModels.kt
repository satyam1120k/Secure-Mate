package com.example.securemate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.securemate.data.local.SecurityPreferences
import com.example.securemate.data.repository.AssessmentState
import com.example.securemate.data.repository.SecurityAssessmentRepository
import com.example.securemate.data.repository.SecurityScanRepository
import com.example.securemate.domain.model.AppSecurityInfo
import com.example.securemate.domain.model.PasswordStrengthResult
import com.example.securemate.domain.model.PhishingAnalysisResult
import com.example.securemate.domain.model.RiskLevel
import com.example.securemate.domain.model.SecurityScanRecord
import com.example.securemate.security.passwords.PasswordStrengthAnalyzer
import com.example.securemate.security.phishing.UrlReputationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 1. Dashboard ViewModel
class DashboardViewModel(
    private val assessmentRepository: SecurityAssessmentRepository,
    private val scanHistoryRepository: SecurityScanRepository
) : ViewModel() {

    val assessmentState: StateFlow<AssessmentState> = assessmentRepository.assessmentState

    val latestScan: StateFlow<SecurityScanRecord?> = scanHistoryRepository.getLatestScan()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            assessmentRepository.getOrRunInitialAssessment()
        }
    }

    fun refreshAssessment() {
        viewModelScope.launch {
            assessmentRepository.performFullScan()
        }
    }
}

// 2. App Security Filter & ViewModel
enum class AppFilter(val label: String) {
    ALL("All"),
    USER_APPS("User Apps"),
    HIGH_RISK("High Risk"),
    MODERATE_RISK("Moderate"),
    LOW_RISK("Low Risk"),
    SYSTEM_APPS("System Apps")
}

class AppSecurityViewModel(
    private val assessmentRepository: SecurityAssessmentRepository
) : ViewModel() {

    val assessmentState: StateFlow<AssessmentState> = assessmentRepository.assessmentState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(AppFilter.ALL)
    val activeFilter: StateFlow<AppFilter> = _activeFilter.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppFilter) {
        _activeFilter.value = filter
    }

    fun getFilteredApps(apps: List<AppSecurityInfo>): List<AppSecurityInfo> {
        val query = _searchQuery.value.trim().lowercase()
        val filter = _activeFilter.value

        return apps.filter { app ->
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER_APPS -> !app.isSystemApp
                AppFilter.SYSTEM_APPS -> app.isSystemApp
                AppFilter.HIGH_RISK -> app.riskLevel == RiskLevel.HIGH || app.riskLevel == RiskLevel.CRITICAL
                AppFilter.MODERATE_RISK -> app.riskLevel == RiskLevel.MODERATE
                AppFilter.LOW_RISK -> app.riskLevel == RiskLevel.LOW
            }

            val matchesQuery = if (query.isEmpty()) true else {
                app.appName.lowercase().contains(query) || app.packageName.lowercase().contains(query)
            }

            matchesFilter && matchesQuery
        }
    }
}

// 3. Scan ViewModel
class ScanViewModel(
    private val assessmentRepository: SecurityAssessmentRepository
) : ViewModel() {

    val assessmentState: StateFlow<AssessmentState> = assessmentRepository.assessmentState

    fun runScan() {
        viewModelScope.launch {
            assessmentRepository.performFullScan()
        }
    }
}

// 4. Phishing Checker ViewModel
class PhishingViewModel(
    private val phishingDetector: UrlReputationService
) : ViewModel() {

    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _result = MutableStateFlow<PhishingAnalysisResult?>(null)
    val result: StateFlow<PhishingAnalysisResult?> = _result.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _inputUrl.value = newUrl
    }

    fun analyzeUrl(urlToTest: String? = null) {
        val target = urlToTest ?: _inputUrl.value
        if (target.isBlank()) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            val analysis = phishingDetector.checkUrl(target)
            _result.value = analysis
            _isAnalyzing.value = false
        }
    }

    fun setSampleUrl(url: String) {
        _inputUrl.value = url
        analyzeUrl(url)
    }

    fun clear() {
        _inputUrl.value = ""
        _result.value = null
    }
}

// 5. Password ViewModel (Zero persistence, privacy-focused)
class PasswordViewModel : ViewModel() {

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _strengthResult = MutableStateFlow<PasswordStrengthResult?>(null)
    val strengthResult: StateFlow<PasswordStrengthResult?> = _strengthResult.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    fun onPasswordChanged(newPassword: CharSequence) {
        val str = newPassword.toString()
        _passwordInput.value = str
        _strengthResult.value = PasswordStrengthAnalyzer.analyze(str)
    }

    fun toggleVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun generatePassword(length: Int = 16, useSymbols: Boolean = true) {
        val generated = PasswordStrengthAnalyzer.generateSecurePassword(
            length = length,
            includeSymbols = useSymbols
        )
        _passwordInput.value = generated
        _strengthResult.value = PasswordStrengthAnalyzer.analyze(generated)
    }

    fun clearPassword() {
        _passwordInput.value = ""
        _strengthResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure sensitive memory is cleared
        _passwordInput.value = ""
        _strengthResult.value = null
    }
}

// 6. History ViewModel
class HistoryViewModel(
    private val scanHistoryRepository: SecurityScanRepository
) : ViewModel() {

    val scanHistory: StateFlow<List<SecurityScanRecord>> = scanHistoryRepository.getAllScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAllHistory() {
        viewModelScope.launch {
            scanHistoryRepository.clearAllScans()
        }
    }
}

// 7. Settings ViewModel
class SettingsViewModel(
    private val preferences: SecurityPreferences,
    private val scanHistoryRepository: SecurityScanRepository
) : ViewModel() {

    val backgroundMonitoring = preferences.backgroundMonitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val scanFrequency = preferences.scanFrequencyHours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)

    val themeMode = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val notificationsEnabled = preferences.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setBackgroundMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setBackgroundMonitoringEnabled(enabled)
        }
    }

    fun setScanFrequency(hours: Int) {
        viewModelScope.launch {
            preferences.setScanFrequencyHours(hours)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationsEnabled(enabled)
        }
    }

    fun clearScanData() {
        viewModelScope.launch {
            scanHistoryRepository.clearAllScans()
        }
    }
}

// ViewModel Factory
class SecureMateViewModelFactory(
    private val assessmentRepository: SecurityAssessmentRepository,
    private val scanHistoryRepository: SecurityScanRepository,
    private val preferences: SecurityPreferences,
    private val phishingDetector: UrlReputationService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(assessmentRepository, scanHistoryRepository) as T
            }
            modelClass.isAssignableFrom(AppSecurityViewModel::class.java) -> {
                AppSecurityViewModel(assessmentRepository) as T
            }
            modelClass.isAssignableFrom(ScanViewModel::class.java) -> {
                ScanViewModel(assessmentRepository) as T
            }
            modelClass.isAssignableFrom(PhishingViewModel::class.java) -> {
                PhishingViewModel(phishingDetector) as T
            }
            modelClass.isAssignableFrom(PasswordViewModel::class.java) -> {
                PasswordViewModel() as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(scanHistoryRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(preferences, scanHistoryRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
