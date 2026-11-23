package de.kjltowinkler.msapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import de.kjltowinkler.msapp.MainActivity
import de.kjltowinkler.msapp.R
import de.kjltowinkler.msapp.data.schoolway.SchoolWayRepository
import de.kjltowinkler.msapp.data.task.getCurrentTime
import de.kjltowinkler.msapp.data.task.getTodayDate
import de.kjltowinkler.msapp.data.timetable.TimetableDay
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.data.timetable.getInitialTimetableDay
import java.util.Calendar

/**
 * AppWidgetProvider für das kombinierte Notes+ Android-Homescreen-Widget.
 * Unterstützt dynamische Material You Farben (Android 12+), nächste Unterrichtsstunde
 * und Echtzeit-Schulweg-Countdown.
 */
class VeloAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, VeloAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "de.kjltowinkler.msapp.widget.ACTION_REFRESH"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_velo_schedule_schoolway)

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            // 1. Klick auf gesamtes Widget / Stundenplan -> Öffnet App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, pendingIntentFlags
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_schedule_section, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_schoolway_section, openAppPendingIntent)

            // 2. Klick auf Refresh-Button
            val refreshIntent = Intent(context, VeloAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1, refreshIntent, pendingIntentFlags
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // 3. Datum & Uhrzeit bestimmen
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val currentTotalMinutes = hour * 60 + minute

            val dayNames = arrayOf("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa")
            val dayName = dayNames.getOrElse(dayOfWeek - 1) { "Mo" }
            val timeString = "${dayName}., %02d:%02d".format(hour, minute)
            views.setTextViewText(R.id.widget_header_status, timeString)

            // 4. Stundenplan-Daten ermitteln
            try {
                TimetableRepository.loadFromCache()
                val today = getTodayDate()
                val nowTime = getCurrentTime()
                val targetDay = getInitialTimetableDay(today, nowTime)
                val lessons = TimetableRepository.getLessonsForDay(targetDay)

                val activeOrNextLesson = findRelevantLesson(lessons, currentTotalMinutes)

                if (activeOrNextLesson != null) {
                    val resolvedSubject = TimetableRepository.resolveSubjectName(activeOrNextLesson.subject)
                    val periodText = "${activeOrNextLesson.periodNumber}. Std"
                    val roomText = if (activeOrNextLesson.room.isNotBlank()) "Raum ${activeOrNextLesson.room}" else ""
                    val teacherText = if (activeOrNextLesson.teacherName.isNotBlank()) activeOrNextLesson.teacherName else activeOrNextLesson.teacherCode
                    val detailsText = listOfNotNull(
                        roomText.ifBlank { null },
                        teacherText.ifBlank { null },
                        activeOrNextLesson.timeRange.ifBlank { null }
                    ).joinToString(" • ")

                    views.setTextViewText(R.id.widget_lesson_period, periodText)
                    views.setTextViewText(R.id.widget_lesson_subject, resolvedSubject)
                    views.setTextViewText(R.id.widget_lesson_details, detailsText.ifBlank { "Keine Details" })
                } else {
                    views.setTextViewText(R.id.widget_lesson_period, "Frei")
                    views.setTextViewText(R.id.widget_lesson_subject, "Schulschluss")
                    views.setTextViewText(R.id.widget_lesson_details, "Keine weiteren Stunden heute")
                }
            } catch (_: Throwable) {
                views.setTextViewText(R.id.widget_lesson_period, "–")
                views.setTextViewText(R.id.widget_lesson_subject, "Stundenplan")
                views.setTextViewText(R.id.widget_lesson_details, "Tippen zum Öffnen")
            }

            // 5. Schulweg / ÖPNV-Daten ermitteln
            try {
                val config = SchoolWayRepository.currentConfig
                val busLine = config.busLine.ifBlank { "OF-85" }
                views.setTextViewText(R.id.widget_bus_line, busLine)

                val isMorning = hour < 13
                if (isMorning) {
                    views.setTextViewText(R.id.widget_bus_stop, "Zur Schule")
                    val start = config.morningStartStop.substringAfterLast(" ").ifBlank { config.morningStartStop }
                    views.setTextViewText(R.id.widget_bus_detail, "Ab $start • Gehzeit: ${config.walkingMinutes} Min.")

                    // Countdown: Nächste Abfahrt alle 20 Minuten
                    val interval = 20
                    val minutesUntil = interval - (minute % interval)
                    views.setTextViewText(R.id.widget_bus_countdown, "in $minutesUntil min")
                } else {
                    views.setTextViewText(R.id.widget_bus_stop, "Nach Hause")
                    val end = config.afternoonEndStop.substringAfterLast(" ").ifBlank { config.afternoonEndStop }
                    views.setTextViewText(R.id.widget_bus_detail, "Nach $end • Gehzeit: ${config.walkingMinutes} Min.")

                    val interval = 20
                    val minutesUntil = interval - (minute % interval)
                    views.setTextViewText(R.id.widget_bus_countdown, "in $minutesUntil min")
                }
            } catch (_: Throwable) {
                views.setTextViewText(R.id.widget_bus_line, "Bus")
                views.setTextViewText(R.id.widget_bus_stop, "Schulweg")
                views.setTextViewText(R.id.widget_bus_detail, "Tippen zum Konfigurieren")
                views.setTextViewText(R.id.widget_bus_countdown, "")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun findRelevantLesson(
            lessons: List<TimetableLesson>,
            currentTotalMinutes: Int
        ): TimetableLesson? {
            if (lessons.isEmpty()) return null

            // Suche nach laufender oder nächster anstehender Stunde
            for (lesson in lessons.sortedBy { it.periodNumber }) {
                val range = lesson.timeRange
                if (range.contains("-")) {
                    val parts = range.split("-")
                    val endMinutes = parseTimeToMinutes(parts[1].trim())
                    if (endMinutes != null && currentTotalMinutes <= endMinutes) {
                        return lesson
                    }
                }
            }
            // Falls alle vorbei sind, gib null zurück
            return null
        }

        private fun parseTimeToMinutes(timeStr: String): Int? {
            return try {
                val clean = timeStr.replace("Uhr", "").trim()
                val parts = clean.split(":")
                val h = parts[0].trim().toInt()
                val m = parts[1].trim().toInt()
                h * 60 + m
            } catch (_: Throwable) {
                null
            }
        }
    }
}
