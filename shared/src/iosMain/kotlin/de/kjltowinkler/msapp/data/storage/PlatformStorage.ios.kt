package de.kjltowinkler.msapp.data.storage

import platform.Foundation.NSUserDefaults

actual object PlatformStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String): String? {
        return defaults.stringForKey(key)
    }

    actual fun setString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    actual fun clear() {
        defaults.dictionaryRepresentation().keys.forEach { k ->
            defaults.removeObjectForKey(k.toString())
        }
    }
}
