package com.example.securemate.security.phishing

import com.example.securemate.domain.model.PhishingAnalysisResult
import com.example.securemate.domain.model.RiskLevel
import java.net.URI
import java.util.Locale
import java.util.regex.Pattern

class LocalHeuristicPhishingDetector : UrlReputationService {

    private val ipPattern = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")

    private val suspiciousTlds = setOf(
        "xyz", "top", "work", "click", "country", "zip", "mov", "buzz", "surf",
        "cam", "kim", "fit", "gq", "cf", "ml", "ga", "tk"
    )

    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "is.gd", "buff.ly", "ow.ly", "cutt.ly",
        "shorturl.at", "tiny.cc", "rebrand.ly"
    )

    private val lookalikeBrands = mapOf(
        "paypa1" to "PayPal",
        "micros0ft" to "Microsoft",
        "g00gle" to "Google",
        "app1e" to "Apple",
        "netf1ix" to "Netflix",
        "amaz0n" to "Amazon",
        "faceb00k" to "Facebook",
        "chase-security" to "Chase",
        "wellsfarg0" to "Wells Fargo",
        "binance-verify" to "Binance"
    )

    private val highRiskKeywords = listOf(
        "login-verify", "secure-account", "update-billing", "bank-login",
        "wallet-connect", "passcode-reset", "suspended-notice", "claim-reward",
        "kyc-verify", "urgent-notice"
    )

    override suspend fun checkUrl(url: String): PhishingAnalysisResult {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return PhishingAnalysisResult(
                url = trimmed,
                riskLevel = RiskLevel.LOW,
                riskScore = 0,
                indicatorsDetected = listOf("No URL entered"),
                heuristicDetails = emptyList(),
                isHttps = false,
                domain = "",
                recommendations = listOf("Please enter a valid URL to analyze.")
            )
        }

        val normalizedUrl = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val indicators = mutableListOf<String>()
        val details = mutableListOf<String>()
        var score = 0

        val uri = try {
            URI(normalizedUrl)
        } catch (e: Exception) {
            return PhishingAnalysisResult(
                url = trimmed,
                riskLevel = RiskLevel.HIGH,
                riskScore = 75,
                indicatorsDetected = listOf("Malformed or unparseable URL syntax"),
                heuristicDetails = listOf("The provided address does not adhere to RFC standards for web URLs."),
                isHttps = false,
                domain = "",
                recommendations = listOf("Do not navigate to malformed links as they may crash handlers or conceal attack payloads.")
            )
        }

        val host = uri.host?.lowercase(Locale.US) ?: ""
        val scheme = uri.scheme?.lowercase(Locale.US) ?: ""
        val port = uri.port
        val rawAuthority = uri.rawAuthority ?: ""

        // 1. Check Protocol: Insecure HTTP
        val isHttps = scheme == "https"
        if (!isHttps) {
            score += 25
            indicators.add("Insecure HTTP protocol (no TLS encryption)")
            details.add("Unencrypted HTTP allows eavesdropping and man-in-the-middle tampering of data in transit.")
        }

        // 2. Host is a direct IP address
        if (ipPattern.matcher(host).matches() || host.startsWith("[") && host.endsWith("]")) {
            score += 40
            indicators.add("Host is a direct raw IP address instead of a domain name")
            details.add("Legitimate organizations rarely use raw numerical IP addresses in consumer links. Common tactic in phishing campaigns to evade domain blacklists.")
        }

        // 3. Check for @ symbol in authority (HTTP Basic Auth trick)
        if (rawAuthority.contains("@") || trimmed.contains("@")) {
            score += 45
            indicators.add("Contains '@' symbol in URL authority")
            details.add("The '@' symbol can be used to deceive users: everything before '@' is treated as credentials, while the actual server follows the '@'.")
        }

        // 4. Punycode / IDN Homograph indicator
        if (host.startsWith("xn--") || host.contains(".xn--")) {
            score += 35
            indicators.add("Punycode (IDN) detected: Internationalized Domain Name")
            details.add("Punycode ('xn--') allows non-ASCII characters that visually mimic Latin letters (e.g. Cyrillic 'а' vs Latin 'a').")
        }

        // 5. Excessive subdomains
        val dotCount = host.count { it == '.' }
        if (dotCount > 3) {
            score += 20
            indicators.add("Excessive subdomain depth ($dotCount subdomains)")
            details.add("Phishing sites often stack subdomains (e.g. 'paypal.com.verify-user.xyz') to fool users into reading the trusted brand name at the start.")
        }

        // 6. Suspicious / High-Abuse TLDs
        val tld = host.substringAfterLast(".", "")
        if (suspiciousTlds.contains(tld)) {
            score += 18
            indicators.add("High-abuse Top-Level Domain (.$tld)")
            details.add("The .$tld extension has statistically elevated rates of disposable phishing and malicious infrastructure registrations.")
        }

        // 7. URL Shorteners
        if (urlShorteners.contains(host)) {
            score += 15
            indicators.add("URL shortener service detected ($host)")
            details.add("Shortened URLs conceal the real destination page until clicked. Verify the unshortened link before entering passwords or sensitive info.")
        }

        // 8. Typosquatting & Brand lookalikes
        for ((lookalike, brand) in lookalikeBrands) {
            if (host.contains(lookalike)) {
                score += 40
                indicators.add("Brand lookalike / Typosquatting detected for '$brand'")
                details.add("Domain contains '$lookalike', a deceptive substitution for the legitimate brand '$brand'.")
                break
            }
        }

        // 9. Suspicious keywords in host or path
        val fullPath = (uri.path ?: "") + (uri.query ?: "")
        for (kw in highRiskKeywords) {
            if (host.contains(kw) || fullPath.contains(kw)) {
                score += 15
                indicators.add("High-risk credential keyword pattern ('$kw')")
                details.add("The link contains patterns typically associated with credential harvesting and fake authentication portals.")
                break
            }
        }

        // 10. Non-standard ports
        if (port != -1 && port != 80 && port != 443 && port != 8080) {
            score += 15
            indicators.add("Non-standard web port (:$port)")
            details.add("Public websites typically serve traffic on standard ports (80/443). Uncommon ports may indicate temporary or compromised hosts.")
        }

        val finalScore = score.coerceIn(0, 100)
        val riskLevel = RiskLevel.fromScore(finalScore)

        val recommendations = mutableListOf<String>()
        when (riskLevel) {
            RiskLevel.CRITICAL, RiskLevel.HIGH -> {
                recommendations.add("Do NOT enter passwords, credit cards, or personal credentials on this website.")
                recommendations.add("Verify the official website address directly through search engines or saved bookmarks.")
                recommendations.add("If you already visited, consider changing passwords on related services immediately.")
            }
            RiskLevel.MODERATE -> {
                recommendations.add("Proceed with caution. Carefully verify the browser address bar for subtle domain misspellings.")
                recommendations.add("Avoid signing in through unsolicited links received via SMS, email, or chat.")
            }
            RiskLevel.LOW -> {
                recommendations.add("No immediate heuristic flags detected. Local heuristics cannot guarantee complete safety against newly minted domains.")
                recommendations.add("Always confirm the domain matches the service you intend to visit.")
            }
        }

        if (indicators.isEmpty()) {
            indicators.add("✓ Standard HTTPS domain structure")
            details.add("Valid TLS scheme, standard port, and recognized domain hierarchy.")
        }

        return PhishingAnalysisResult(
            url = trimmed,
            riskLevel = riskLevel,
            riskScore = finalScore,
            indicatorsDetected = indicators,
            heuristicDetails = details,
            isHttps = isHttps,
            domain = host,
            recommendations = recommendations
        )
    }
}
