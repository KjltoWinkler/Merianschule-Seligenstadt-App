package de.kjltowinkler.msapp.data.storage

import java.util.prefs.Preferences

actual object PlatformStorage {
    private val prefs by lazy {
        Preferences.userRoot().node("de.kjltowinkler.msapp")
    }

    actual fun getString(key: String): String? {
        return runCatching {
            val v = prefs.get(key, null)
            if (v.isNullOrEmpty()) null else v
        }.getOrNull()
    }

    actual fun setString(key: String, value: String?) {
        runCatching {
            if (value == null) {
                prefs.remove(key)
            } else {
                prefs.put(key, value)
            }
            prefs.flush()
        }
    }

    actual fun remove(key: String) {
        runCatching {
            prefs.remove(key)
            prefs.flush()
        }
    }

    actual fun clear() {
        runCatching {
            prefs.clear()
            prefs.flush()
        }
    }
}
