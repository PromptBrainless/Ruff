package de.ruff.dimension.data.repository

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal interface AdminAccessRepository {
    fun isConfigured(): Boolean

    fun setPin(pin: String)

    fun verifyPin(pin: String): Boolean

    fun changePin(
        currentPin: String,
        newPin: String,
    ): Boolean
}

/**
 * Gerätegebundener PIN-Schutz gegen versehentliches Editieren, kein Ersatz für Android-Geräteschutz.
 * Es wird ausschließlich ein gesalzener PBKDF2-Hash gespeichert.
 */
internal class SharedPreferencesAdminAccess(
    context: Context,
) : AdminAccessRepository {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun isConfigured(): Boolean = preferences.contains(HASH_KEY) && preferences.contains(SALT_KEY)

    override fun setPin(pin: String) {
        require(pin.length >= MIN_PIN_LENGTH && pin.all(Char::isDigit)) {
            "Die Admin-PIN muss mindestens vier Ziffern enthalten."
        }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val hash = derive(pin, salt)
        check(
            preferences
                .edit()
                .putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(HASH_KEY, Base64.encodeToString(hash, Base64.NO_WRAP))
                .commit(),
        ) { "Admin-PIN konnte nicht lokal gespeichert werden." }
    }

    override fun verifyPin(pin: String): Boolean {
        val salt = preferences.getString(SALT_KEY, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val expected = preferences.getString(HASH_KEY, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(expected, derive(pin, salt))
    }

    override fun changePin(
        currentPin: String,
        newPin: String,
    ): Boolean {
        require(newPin.length >= MIN_PIN_LENGTH && newPin.all(Char::isDigit)) {
            "Die neue Admin-PIN muss mindestens vier Ziffern enthalten."
        }
        if (!verifyPin(currentPin)) return false
        setPin(newPin)
        return true
    }

    private fun derive(
        pin: String,
        salt: ByteArray,
    ): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val PREFERENCES = "ruff_admin_access"
        const val SALT_KEY = "pin_salt"
        const val HASH_KEY = "pin_hash"
        const val MIN_PIN_LENGTH = 4
        const val SALT_BYTES = 16
        const val HASH_BITS = 256
        const val ITERATIONS = 150_000
    }
}
