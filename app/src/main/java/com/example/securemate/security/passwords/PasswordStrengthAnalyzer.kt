package com.example.securemate.security.passwords

import com.example.securemate.domain.model.PasswordStrengthLevel
import com.example.securemate.domain.model.PasswordStrengthResult
import java.security.SecureRandom
import java.util.Locale
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

object PasswordStrengthAnalyzer {

    private val COMMON_PASSWORDS = setOf(
        "password", "123456", "12345678", "1234", "qwerty", "12345", "dragon",
        "pussy", "baseball", "football", "letmein", "master", "monkey", "shadow",
        "superman", "trustno1", "admin", "welcome", "login", "princess", "solo",
        "starwars", "iloveyou", "computer", "michelle", "jessica", "charlie",
        "hunter2", "access", "sunshine", "freedom", "secret", "whatever", "default",
        "passcode", "security", "administrator", "system", "changeme", "test",
        "testing", "hello", "orange", "qwerty123", "password123", "abc123"
    )

    private val KEYBOARD_SEQUENCES = listOf(
        "qwertyuiop", "asdfghjkl", "zxcvbnm",
        "1234567890", "0987654321",
        "abcdefghijklmnopqrstuvwxyz"
    )

    fun analyze(password: CharSequence?): PasswordStrengthResult {
        if (password.isNullOrEmpty()) {
            return PasswordStrengthResult(
                score = 0,
                strengthLevel = PasswordStrengthLevel.VERY_WEAK,
                entropyBits = 0.0,
                feedbackMessages = listOf("Enter a password to evaluate its strength."),
                hasUppercase = false,
                hasLowercase = false,
                hasDigits = false,
                hasSymbols = false,
                length = 0,
                commonPatternDetected = false
            )
        }

        val pwd = password.toString()
        val length = pwd.length
        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSymbol = false

        for (ch in pwd) {
            when {
                ch.isUpperCase() -> hasUpper = true
                ch.isLowerCase() -> hasLower = true
                ch.isDigit() -> hasDigit = true
                else -> hasSymbol = true
            }
        }

        // Calculate Character Pool Size for Shannon Entropy
        var poolSize = 0
        if (hasLower) poolSize += 26
        if (hasUpper) poolSize += 26
        if (hasDigit) poolSize += 10
        if (hasSymbol) poolSize += 33

        val entropyBits = if (poolSize > 0) {
            length * log2(poolSize.toDouble())
        } else {
            0.0
        }

        val feedback = mutableListOf<String>()
        var patternDetected = false

        // 1. Common dictionary check
        val lowerCasePwd = pwd.lowercase(Locale.US)
        if (COMMON_PASSWORDS.contains(lowerCasePwd)) {
            feedback.add("⚠ Appears in leaked common password dictionaries (extremely vulnerable to brute-force).")
            patternDetected = true
        }

        // 2. Sequential & Keyboard Patterns
        for (seq in KEYBOARD_SEQUENCES) {
            if (seq.contains(lowerCasePwd) && lowerCasePwd.length >= 3) {
                feedback.add("⚠ Contains sequential keyboard patterns (e.g. '$lowerCasePwd').")
                patternDetected = true
                break
            }
            // Check substring of length 4 or more
            for (i in 0..(seq.length - 4)) {
                val sub = seq.substring(i, i + 4)
                if (lowerCasePwd.contains(sub)) {
                    feedback.add("⚠ Contains sequential sequence ('$sub').")
                    patternDetected = true
                    break
                }
            }
            if (patternDetected) break
        }

        // 3. Repeated characters (e.g. 'aaaa', '1111')
        val repeatedPattern = "(\\w)\\1\\1\\1".toRegex()
        if (repeatedPattern.containsMatchIn(pwd)) {
            feedback.add("⚠ Contains excessively repeated characters.")
            patternDetected = true
        }

        // Construct Positive / Constructive feedback
        if (length >= 16) {
            feedback.add("✓ Excellent length (16+ characters).")
        } else if (length >= 12) {
            feedback.add("✓ Good length (12+ characters).")
        } else if (length >= 8) {
            feedback.add("⚠ Moderate length. Recommend at least 12–16 characters.")
        } else {
            feedback.add("✕ Too short (less than 8 characters). Easily cracked.")
        }

        if (hasUpper && hasLower) {
            feedback.add("✓ Mixes uppercase and lowercase letters.")
        } else {
            feedback.add("⚠ Use a combination of both uppercase and lowercase letters.")
        }

        if (hasDigit) {
            feedback.add("✓ Contains numerical digits.")
        } else {
            feedback.add("⚠ Add numbers (0-9) to expand entropy.")
        }

        if (hasSymbol) {
            feedback.add("✓ Contains special symbols (!@#\$%...).")
        } else {
            feedback.add("⚠ Include special characters to resist automated dictionary attacks.")
        }

        // Base Score calculation
        var calculatedScore = 0

        // Length contribution (up to 40 pts)
        calculatedScore += min(40, (length * 3))

        // Character diversity contribution (up to 30 pts)
        var diversityCount = 0
        if (hasLower) diversityCount++
        if (hasUpper) diversityCount++
        if (hasDigit) diversityCount++
        if (hasSymbol) diversityCount++
        calculatedScore += (diversityCount * 7.5).toInt()

        // Entropy contribution (up to 30 pts)
        if (entropyBits >= 80) calculatedScore += 30
        else if (entropyBits >= 60) calculatedScore += 20
        else if (entropyBits >= 40) calculatedScore += 10

        // Deductions for patterns
        if (patternDetected) {
            calculatedScore -= 35
        }
        if (COMMON_PASSWORDS.contains(lowerCasePwd)) {
            calculatedScore = min(15, calculatedScore)
        }

        val finalScore = max(0, min(100, calculatedScore))

        val level = when {
            finalScore >= 80 -> PasswordStrengthLevel.VERY_STRONG
            finalScore >= 65 -> PasswordStrengthLevel.STRONG
            finalScore >= 45 -> PasswordStrengthLevel.FAIR
            finalScore >= 25 -> PasswordStrengthLevel.WEAK
            else -> PasswordStrengthLevel.VERY_WEAK
        }

        return PasswordStrengthResult(
            score = finalScore,
            strengthLevel = level,
            entropyBits = entropyBits,
            feedbackMessages = feedback,
            hasUppercase = hasUpper,
            hasLowercase = hasLower,
            hasDigits = hasDigit,
            hasSymbols = hasSymbol,
            length = length,
            commonPatternDetected = patternDetected
        )
    }

    /**
     * Generates a cryptographically strong random password using Android SecureRandom
     */
    fun generateSecurePassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val upperChars = "ABCDEFGHJKLMNPQRSTUVWXYZ" // exclude confusing letters O, I
        val lowerChars = "abcdefghijkmnopqrstuvwxyz" // exclude l
        val digitChars = "23456789" // exclude 0, 1
        val symbolChars = "!@#$%^&*-_+=<>?"

        val charPool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()
        val random = SecureRandom()

        if (includeLower) {
            charPool.append(lowerChars)
            guaranteedChars.add(lowerChars[random.nextInt(lowerChars.length)])
        }
        if (includeUpper) {
            charPool.append(upperChars)
            guaranteedChars.add(upperChars[random.nextInt(upperChars.length)])
        }
        if (includeDigits) {
            charPool.append(digitChars)
            guaranteedChars.add(digitChars[random.nextInt(digitChars.length)])
        }
        if (includeSymbols) {
            charPool.append(symbolChars)
            guaranteedChars.add(symbolChars[random.nextInt(symbolChars.length)])
        }

        if (charPool.isEmpty()) {
            charPool.append(lowerChars).append(digitChars)
        }

        val poolString = charPool.toString()
        val passwordChars = mutableListOf<Char>()
        passwordChars.addAll(guaranteedChars)

        for (i in guaranteedChars.size until length) {
            passwordChars.add(poolString[random.nextInt(poolString.length)])
        }

        // Shuffle in place
        passwordChars.shuffle(random)

        return passwordChars.joinToString("")
    }
}
