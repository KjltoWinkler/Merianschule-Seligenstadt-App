package de.kjltowinkler.msapp.data.substitution

import kotlinx.serialization.Serializable

/**
 * Modell für einen Vertretungsplan-Eintrag entsprechend dem Figma-Design:
 * `FigmaDesigns/vertretungsplan#alle.png`
 *
 * @param id Eindeutige ID
 * @param date ISO-Datum (z.B. "2026-10-12")
 * @param dayLabel Anzeigelabel für den Tag (z.B. "Heute, 12. Oktober", "Gestern, 11. Oktober" oder Wochentag)
 * @param period Stunde (z.B. "3. Std", "5. Std")
 * @param subject Fach (z.B. "Mathematik", "Deutsch")
 * @param className Klasse (z.B. "9aR")
 * @param type Art der Vertretung ("Vertretung", "Entfall", "Verlegung")
 * @param details Hauptzeile (z.B. "Hr. Müller statt Fr. Schmidt", "Fr. Becker entfällt ersatzlos")
 * @param roomInfo Zusatzinfo (z.B. "Raum 204 (statt 102) - Aufgaben...", "Entfall - Aufgaben im Schulportal...")
 * @param time Zeitangabe rechts oben (z.B. "7:41", "6:50", "Gestern")
 * @param isNew Zeigt an, ob der Eintrag neu ist (hellgrüner Statusindikator-Punkt)
 * @param isMarked Ob der Eintrag mit Stern markiert/favorisiert ist
 */
@Serializable
data class SubstitutionEntry(
    val id: String,
    val date: String,
    val dayLabel: String,
    val period: String,
    val subject: String,
    val className: String,
    val type: String,
    val details: String,
    val roomInfo: String,
    val time: String,
    val isNew: Boolean = false,
    val isMarked: Boolean = false
)
