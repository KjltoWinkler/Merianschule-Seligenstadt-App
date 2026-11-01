package de.kjltowinkler.msapp.data.storage

import android.content.Context
import de.kjltowinkler.msapp.util.AppContextProvider

actual object PlatformStorage {
    private const val PREFS_NAME = "merianschule_prefs"

    private val prefs by lazy {
        AppContextProvider.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun getString(key: String): String? {
        return runCatching { prefs.getString(key, null) }.getOrNull()
    }

    actual fun setString(key: String, value: String?) {
        runCatching {
            if (value == null) {
                prefs.edit().remove(key).apply()
            } else {
                prefs.edit().putString(key, value).apply()
            }
        }
    }

    actual fun remove(key: String) {
        runCatching { prefs.edit().remove(key).apply() }
    }

    actual fun clear() {
        runCatching { prefs.edit().clear().apply() }
    }
}
