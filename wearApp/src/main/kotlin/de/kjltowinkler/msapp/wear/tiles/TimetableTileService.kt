package de.kjltowinkler.msapp.wear.tiles

import android.content.Context
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textDataCard
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.common.util.concurrent.ListenableFuture
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.substitution.SubstitutionRepository
import de.kjltowinkler.msapp.data.task.getCurrentTime
import de.kjltowinkler.msapp.data.task.getTodayDate
import de.kjltowinkler.msapp.data.timetable.TimetableDay
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.data.timetable.findNextLesson
import de.kjltowinkler.msapp.util.AppContextProvider

/**
 * Wear OS Tile für den Stundenplan (Google Material 3 ProtoLayout).
 * Nutzt den verfügbaren Platz voll aus: zeigt die aktuelle/nächste Stunde sowie
 * (falls vorhanden) direkt die darauffolgende Stunde des Tages.
 */
class TimetableTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        AppContextProvider.init(applicationContext)
        TimetableRepository.loadFromCache()

        val tile = buildTimetableTile(
            context = applicationContext,
            deviceConfiguration = requestParams.deviceConfiguration
        )
        return immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = immediateFuture(emptyResources())
}

internal fun buildTimetableTile(
    context: Context,
    deviceConfiguration: androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
): TileBuilders.Tile {
    val today = getTodayDate()
    val now = getCurrentTime()
    val currentMinutes = now.hour * 60 + now.minute
    val todayDay = TimetableDay.entries.getOrNull(today.dayOfWeek - 1)

    val todayLessons = todayDay?.let { TimetableRepository.getLessonsForDay(it).sortedBy { l -> l.periodNumber } }.orEmpty()
    val currentLessonIndex = todayLessons.indexOfFirst { isLessonNow(it.timeRange, currentMinutes) }

    val nextResult = if (currentLessonIndex == -1) findNextLesson() else null
    val displayDay = if (currentLessonIndex != -1) {
        todayDay
    } else {
        nextResult?.date?.let { TimetableDay.entries.getOrNull(it.dayOfWeek - 1) }
    }

    val dayLessons = displayDay?.let { TimetableRepository.getLessonsForDay(it).sortedBy { l -> l.periodNumber } }.orEmpty()
    val firstLesson: TimetableLesson?
    val secondLesson: TimetableLesson?

    if (currentLessonIndex != -1) {
        firstLesson = todayLessons[currentLessonIndex]
        secondLesson = todayLessons.getOrNull(currentLessonIndex + 1)
    } else if (nextResult != null) {
        val idx = dayLessons.indexOfFirst { it.id == nextResult.lesson.id && it.periodNumber == nextResult.lesson.periodNumber }
        firstLesson = nextResult.lesson
        secondLesson = if (idx != -1) dayLessons.getOrNull(idx + 1) else null
    } else {
        firstLesson = null
        secondLesson = null
    }

    val subs = SubstitutionRepository.getInitialOrCachedEntries()
    val firstSub = if (displayDay != null && firstLesson != null) {
        TimetableRepository.findMatchingSubstitution(displayDay, firstLesson, subs)
    } else null
    val secondSub = if (displayDay != null && secondLesson != null) {
        TimetableRepository.findMatchingSubstitution(displayDay, secondLesson, subs)
    } else null

    val userClass = TimetableRepository.getEffectiveUserClass()
    val openTimetable = launchRouteClickable(context, "timetable", "open_timetable")

    val layout = materialScope(
        context = context,
        deviceConfiguration = deviceConfiguration
    ) {
        val headerText = when {
            currentLessonIndex != -1 -> "Jetzt • $userClass"
            nextResult != null && nextResult.date == today -> "Heute • $userClass"
            displayDay != null -> "${displayDay.displayName} • $userClass"
            else -> "Stundenplan"
        }

        primaryLayout(
            titleSlot = { tileTitleText(headerText) },
            mainSlot = {
                if (firstLesson == null) {
                    textDataCard(
                        onClick = openTimetable,
                        width = expand(),
                        height = expand(),
                        title = {
                            text(
                                text = "Kein Unterricht".layoutString,
                                typography = Typography.TITLE_MEDIUM,
                                maxLines = 1
                            )
                        },
                        content = {
                            text(
                                text = "Tippen für den gesamten Stundenplan".layoutString,
                                typography = Typography.BODY_SMALL,
                                maxLines = 2
                            )
                        },
                        colors = tonalCardColors()
                    )
                } else if (secondLesson == null) {
                    lessonTileCard(
                        lesson = firstLesson,
                        matchingSub = firstSub,
                        onClick = openTimetable,
                        isPrimary = currentLessonIndex != -1
                    )
                } else {
                    LayoutElementBuilders.Column.Builder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .addContent(
                            lessonTileCard(
                                lesson = firstLesson,
                                matchingSub = firstSub,
                                onClick = openTimetable,
                                isPrimary = currentLessonIndex != -1,
                                compact = true
                            )
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setHeight(dp(4f))
                                .build()
                        )
                        .addContent(
                            lessonTileCard(
                                lesson = secondLesson,
                                matchingSub = secondSub,
                                onClick = openTimetable,
                                isPrimary = false,
                                compact = true
                            )
                        )
                        .build()
                }
            },
            bottomSlot = {
                textEdgeButton(
                    onClick = openTimetable,
                    labelContent = {
                        text("Stundenplan".layoutString)
                    }
                )
            }
        )
    }

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(TILE_RESOURCES_VERSION)
        .setFreshnessIntervalMillis(15 * 60 * 1000L)
        .setTileTimeline(singleEntryTimeline(layout))
        .build()
}

private fun androidx.wear.protolayout.material3.MaterialScope.lessonTileCard(
    lesson: TimetableLesson,
    matchingSub: SubstitutionEntry?,
    onClick: androidx.wear.protolayout.ModifiersBuilders.Clickable,
    isPrimary: Boolean,
    compact: Boolean = false
): LayoutElementBuilders.LayoutElement {
    val isCancelled = matchingSub?.type?.equals("Entfall", ignoreCase = true) == true
    val subjectName = TimetableRepository.resolveSubjectName(lesson.subject)
    val titleText = if (isCancelled) "$subjectName (Entfall)" else subjectName
    val detailText = buildString {
        append(lesson.timeRange)
        if (lesson.room.isNotBlank()) append(" • ${lesson.room}")
        if (matchingSub != null && !isCancelled) append(" • ${matchingSub.type}")
    }

    val colors = when {
        isCancelled -> errorCardColors()
        isPrimary -> primaryCardColors()
        else -> tonalCardColors()
    }

    return textDataCard(
        onClick = onClick,
        width = expand(),
        height = expand(),
        title = {
            text(
                text = titleText.layoutString,
                typography = if (compact) Typography.TITLE_SMALL else Typography.TITLE_MEDIUM,
                maxLines = 1
            )
        },
        content = {
            text(
                text = detailText.layoutString,
                typography = Typography.BODY_SMALL,
                maxLines = if (compact) 1 else 2
            )
        },
        colors = colors
    )
}

private fun isLessonNow(timeRange: String, currentMinutes: Int): Boolean {
    val parts = timeRange.split("-")
    if (parts.size < 2) return false
    val start = parseMinutesSafe(parts[0]) ?: return false
    val end = parseMinutesSafe(parts[1]) ?: return false
    return currentMinutes in start until end
}

private fun parseMinutesSafe(raw: String): Int? {
    val segs = raw.trim().split(":")
    if (segs.size < 2) return null
    val h = segs[0].trim().toIntOrNull() ?: return null
    val m = segs[1].trim().take(2).toIntOrNull() ?: return null
    return h * 60 + m
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
private fun timetableTilePreview(context: Context) = TilePreviewData(
    onTileRequest = { request ->
        TimetableRepository.applyDefaultMockLessons()
        buildTimetableTile(context, request.deviceConfiguration)
    }
)
