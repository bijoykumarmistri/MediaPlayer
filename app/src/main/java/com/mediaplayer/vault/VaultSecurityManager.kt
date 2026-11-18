package com.mediaplayer.vault

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

class VaultSecurityManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var isSessionUnlocked: Boolean = false

    companion object {
        private const val PREFS_NAME = "mediaplayer_vault_security"
        private const val KEY_PIN_HASH = "vault_pin_hash"
        private const val KEY_PIN_SALT = "vault_pin_salt"
        private const val KEY_SECURITY_QUESTION = "vault_security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "vault_security_answer_hash"
        private const val KEY_SECURITY_ANSWER_SALT = "vault_security_answer_salt"

        val DEFAULT_SECURITY_QUESTIONS = listOf(
            "What is your mother's maiden name?",
            "What was the name of your first school?",
            "What city were you born in?",
            "What is your favorite book or movie?",
            "What was the make of your first vehicle?"
        )
    }

    fun isPinSet(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun isUnlocked(): Boolean {
        return isSessionUnlocked
    }

    fun setUnlocked(unlocked: Boolean) {
        isSessionUnlocked = unlocked
    }

    fun lock() {
        isSessionUnlocked = false
    }

    fun setupPin(pin: String, question: String, answer: String): Boolean {
        if (pin.length < 4 || answer.trim().isEmpty()) return false

        val pinSalt = generateSalt()
        val pinHash = hashWithSalt(pin, pinSalt)

        val answerSalt = generateSalt()
        val answerHash = hashWithSalt(answer.trim().lowercase(), answerSalt)

        prefs.edit()
            .putString(KEY_PIN_SALT, pinSalt)
            .putString(KEY_PIN_HASH, pinHash)
            .putString(KEY_SECURITY_QUESTION, question)
            .putString(KEY_SECURITY_ANSWER_SALT, answerSalt)
            .putString(KEY_SECURITY_ANSWER_HASH, answerHash)
            .apply()

        isSessionUnlocked = true
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false

        val computedHash = hashWithSalt(pin, storedSalt)
        val valid = computedHash == storedHash
        if (valid) {
            isSessionUnlocked = true
        }
        return valid
    }

    fun getSecurityQuestion(): String {
        return prefs.getString(KEY_SECURITY_QUESTION, DEFAULT_SECURITY_QUESTIONS.first()) ?: DEFAULT_SECURITY_QUESTIONS.first()
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val storedSalt = prefs.getString(KEY_SECURITY_ANSWER_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_SECURITY_ANSWER_HASH, null) ?: return false

        val computedHash = hashWithSalt(answer.trim().lowercase(), storedSalt)
        return computedHash == storedHash
    }

    fun resetPinWithAnswer(answer: String, newPin: String): Boolean {
        if (newPin.length < 4) return false
        if (!verifySecurityAnswer(answer)) return false

        val newSalt = generateSalt()
        val newHash = hashWithSalt(newPin, newSalt)

        prefs.edit()
            .putString(KEY_PIN_SALT, newSalt)
            .putString(KEY_PIN_HASH, newHash)
            .apply()

        isSessionUnlocked = true
        return true
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (newPin.length < 4) return false
        if (!verifyPin(oldPin)) return false

        val newSalt = generateSalt()
        val newHash = hashWithSalt(newPin, newSalt)

        prefs.edit()
            .putString(KEY_PIN_SALT, newSalt)
            .putString(KEY_PIN_HASH, newHash)
            .apply()

        return true
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return java.util.Base64.getEncoder().encodeToString(salt)
    }

    private fun hashWithSalt(data: String, saltBase64: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salt = java.util.Base64.getDecoder().decode(saltBase64)
        digest.update(salt)
        val hash = digest.digest(data.toByteArray(Charsets.UTF_8))
        return java.util.Base64.getEncoder().encodeToString(hash)
    }
}
