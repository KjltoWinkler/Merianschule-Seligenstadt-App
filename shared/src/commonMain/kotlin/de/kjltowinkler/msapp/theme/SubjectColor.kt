package de.kjltowinkler.msapp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import de.kjltowinkler.msapp.data.timetable.TimetableRepository

/**
 * Liefert die offizielle Schulfach-Farbe (Stand Hessen / Merianschule Seligenstadt).
 *
 * Hessen / Merianschule Seligenstadt Heft- und Mappenfarben:
 * - Deutsch: Rot
 * - Mathematik: Blau
 * - Englisch: Gelb
 * - Französisch / WPU: Rosa
 * - Biologie: Grün
 * - Chemie: Schwarz / Anthrazit
 * - Physik: Grau
 * - Religion / Ethik: Lila
 * - Geschichte: Orange / Gelbbraun
 * - Erdkunde: Braun
 * - Politik & Wirtschaft: Koralle / Terracotta
 * - Kunst: Orange
 * - Musik: Violett-Blau
 * - Sport: Türkis / Cyan
 * - Informatik: Türkis
 * - Arbeitslehre: Weißlich (Off-White)
 *
 * Im Dark- und Light-Mode für optimale Lesbarkeit und Kontrast abgestimmt.
 */
object SubjectColor {

    fun getColor(
        subject: String,
        isDark: Boolean,
        fallback: Color
    ): Color {
        val trimmed = subject.trim()
        if (trimmed.isBlank()) return fallback

        val resolved = TimetableRepository.resolveSubjectName(trimmed).trim().lowercase()

        return when {
            // Deutsch: Rot
            resolved.startsWith("deutsch") || resolved == "d" || resolved == "daz" -> {
                if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
            }
            // Mathematik: Blau
            resolved.startsWith("mathe") || resolved == "m" -> {
                if (isDark) Color(0xFF42A5F5) else Color(0xFF1565C0)
            }
            // Englisch: Gelb
            resolved.startsWith("englisch") || resolved == "e" -> {
                if (isDark) Color(0xFFFFD54F) else Color(0xFFE65100)
            }
            // Französisch / WPU: Rosa
            resolved.startsWith("franz") || resolved.startsWith("wpu") || resolved == "f" -> {
                if (isDark) Color(0xFFF48FB1) else Color(0xFFC2185B)
            }
            // Biologie: Grün
            resolved.startsWith("bio") || resolved.startsWith("nawi") || resolved == "b" -> {
                if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
            }
            // Chemie: Schwarz / Schiefer
            resolved.startsWith("chemie") || resolved == "ch" -> {
                if (isDark) Color(0xFFCFD8DC) else Color(0xFF37474F)
            }
            // Physik: Grau / Blaugrau
            resolved.startsWith("physik") || resolved == "ph" -> {
                if (isDark) Color(0xFF90A4AE) else Color(0xFF455A64)
            }
            // Religion / Ethik: Lila
            resolved.startsWith("rel") || resolved.startsWith("ethik") || resolved.startsWith("ev") ||
                resolved.startsWith("kath") || resolved.startsWith("kr") -> {
                if (isDark) Color(0xFFBA68C8) else Color(0xFF7B1FA2)
            }
            // Geschichte: Orange / Gelbbraun
            resolved.startsWith("geschich") || resolved == "gl" || resolved == "g" -> {
                if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
            }
            // Erdkunde: Braun
            resolved.startsWith("erdkunde") || resolved.startsWith("geo") || resolved == "ek" -> {
                if (isDark) Color(0xFFBCAAA4) else Color(0xFF5D4037)
            }
            // Politik & Wirtschaft: Koralle / Terracotta
            resolved.startsWith("politik") || resolved.startsWith("powi") || resolved == "pw" -> {
                if (isDark) Color(0xFFFF8A65) else Color(0xFFD84315)
            }
            // Kunst: Orange
            resolved.startsWith("kunst") || resolved == "ku" -> {
                if (isDark) Color(0xFFFF7043) else Color(0xFFE64A19)
            }
            // Musik: Violett-Blau
            resolved.startsWith("musik") || resolved == "mu" -> {
                if (isDark) Color(0xFF80DEEA) else Color(0xFF00838F)
            }
            // Sport: Cyan / Türkis
            resolved.startsWith("sport") || resolved == "sp" -> {
                if (isDark) Color(0xFF4DD0E1) else Color(0xFF00838F)
            }
            // Informatik: Türkis
            resolved.startsWith("informatik") || resolved == "inf" -> {
                if (isDark) Color(0xFF26C6DA) else Color(0xFF00695C)
            }
            // Arbeitslehre: Weißlich (Off-White / Kreideweiß)
            resolved.startsWith("arbeitslehre") || resolved == "al" -> {
                if (isDark) Color(0xFFF5F3ED) else Color(0xFF57534E)
            }
            // Spanisch: Gelbgold
            resolved.startsWith("span") -> {
                if (isDark) Color(0xFFFFCA28) else Color(0xFFF57F17)
            }
            // Latein: Silber
            resolved.startsWith("latein") || resolved == "l" -> {
                if (isDark) Color(0xFFE0E0E0) else Color(0xFF424242)
            }
            else -> fallback
        }
    }
}

/**
 * Composable-Hilfsfunktion zur Ermittlung der Fachfarbe anhand des aktuellen MaterialTheme.
 */
@Composable
fun getSubjectColor(subject: String): Color {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    return SubjectColor.getColor(subject, isDark, fallback = colorScheme.primary)
}
