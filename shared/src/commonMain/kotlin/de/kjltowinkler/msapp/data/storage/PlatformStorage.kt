package de.kjltowinkler.msapp.data.storage

expect object PlatformStorage {
    fun getString(key: String): String?
    fun setString(key: String, value: String?)
    fun remove(key: String)
    fun clear()
}
