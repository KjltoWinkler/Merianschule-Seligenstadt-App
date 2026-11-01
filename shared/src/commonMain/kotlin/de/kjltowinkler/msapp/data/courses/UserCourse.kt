package de.kjltowinkler.msapp.data.courses

import kotlinx.serialization.Serializable

/**
 * Repräsentiert einen Kurs oder ein differenziertes Fach eines Schülers
 * (z. B. Ethik, ev./kath. Religion, WPU, 2. Fremdsprache wie Französisch/Latein).
 */
@Serializable
data class UserCourse(
    val id: String,
    val subject: String,
    val teacher: String = "",
    val name: String = "",
    val room: String = "",
    val isCustom: Boolean = false
) {
    /**
     * Gibt ein lesbares Label zurück, z.B. "Ethik (Web)" oder "WPU Informatik (Mül)".
     */
    fun displayLabel(): String {
        return when {
            name.isNotBlank() && teacher.isNotBlank() -> "$name ($teacher)"
            name.isNotBlank() -> name
            teacher.isNotBlank() -> "$subject ($teacher)"
            else -> subject
        }
    }
}
