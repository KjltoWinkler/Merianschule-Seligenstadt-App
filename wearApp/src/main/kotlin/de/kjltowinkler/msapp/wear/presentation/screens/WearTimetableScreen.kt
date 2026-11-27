package de.kjltowinkler.msapp.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.substitution.SubstitutionRepository
import de.kjltowinkler.msapp.data.task.getCurrentTime
import de.kjltowinkler.msapp.data.task.getTodayDate
import de.kjltowinkler.msapp.data.timetable.TimetableDay
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.data.timetable.getInitialTimetableDay
import de.kjltowinkler.msapp.theme.SubjectColor
import de.kjltowinkler.msapp.wear.R
import de.kjltowinkler.msapp.wear.presentation.theme.GoogleSansRounded
import de.kjltowinkler.msapp.wear.presentation.theme.WearAppTheme
import kotlinx.coroutines.launch

/**
 * Stundenplan-Screen für Wear OS (Google Pixel Watch).
 *
 * Orientiert sich an Googles Wear OS Material 3 Expressive Guidelines:
 * - TransformingLazyColumn mit ScreenScaffold & RSB-Unterstützung
 * - Kompakte Wochentag-Auswahl (Mo–Fr) ohne Swipe-Konflikt mit SwipeDismissableNavHost
 * - Hervorhebung der aktuellen Stunde + Abgleich mit dem Vertretungsplan
 */
@Composable
fun WearTimetableScreen() {
    val coroutineScope = rememberCoroutineScope()
    var selectedDay by remember { mutableStateOf(getInitialTimetableDay()) }
    var substitutions by remember {
        mutableStateOf(SubstitutionRepository.getInitialOrCachedEntries())
    }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        TimetableRepository.loadFromCache()
        if (BackendApiClient.isLoggedIn) {
            TimetableRepository.loadFromServer()
        }
        val loadedSubs = SubstitutionRepository.loadSubstitutions(forceRefresh = false)
        if (loadedSubs.isNotEmpty()) {
            substitutions = loadedSubs
        }
    }

    val lessons = TimetableRepository.getLessonsForDay(selectedDay)
    val userClass = TimetableRepository.getEffectiveUserClass()

    WearTimetableScreenContent(
        selectedDay = selectedDay,
        userClass = userClass,
        lessons = lessons,
        substitutions = substitutions,
        isRefreshing = isRefreshing,
        onSelectDay = { selectedDay = it },
        onRefreshClick = {
            if (!isRefreshing) {
                isRefreshing = true
                coroutineScope.launch {
                    try {
                        TimetableRepository.loadFromServer()
                        val freshSubs = SubstitutionRepository.loadSubstitutions(forceRefresh = true)
                        if (freshSubs.isNotEmpty()) {
                            substitutions = freshSubs
                        }
                    } finally {
                        isRefreshing = false
                    }
                }
            }
        }
    )
}

@Composable
fun WearTimetableScreenContent(
    selectedDay: TimetableDay,
    userClass: String,
    lessons: List<TimetableLesson>,
    substitutions: List<SubstitutionEntry>,
    isRefreshing: Boolean,
    onSelectDay: (TimetableDay) -> Unit,
    onRefreshClick: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    val today = remember { getTodayDate() }
    val now = remember { getCurrentTime() }
    val currentMinutes = now.hour * 60 + now.minute
    val isToday = (today.dayOfWeek - 1) == selectedDay.ordinal

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onRefreshClick,
                buttonSize = EdgeButtonSize.Small
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = "Aktualisieren",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Kopfzeile: Wochentag & Klasse
            item(key = "header") {
                ListHeader(
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = selectedDay.displayName,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isRefreshing) "Aktualisiere…" else "Klasse $userClass",
                            fontFamily = GoogleSansRounded,
                            fontSize = 11.sp,
                            color = if (isRefreshing) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Kompakte Wochentag-Leiste (Mo–Fr)
            item(key = "day_selector") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimetableDay.entries.forEach { day ->
                        val isSelected = day == selectedDay
                        val isDayToday = (today.dayOfWeek - 1) == day.ordinal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isDayToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    }
                                )
                                .clickable { onSelectDay(day) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.shortName,
                                fontFamily = GoogleSansRounded,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isDayToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (lessons.isEmpty()) {
                item(key = "empty") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_schedule),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Kein Unterricht",
                            fontFamily = GoogleSansRounded,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Für ${selectedDay.displayName} sind keine Stunden eingetragen.",
                            fontFamily = GoogleSansRounded,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(
                    items = lessons,
                    key = { "${selectedDay.id}_${it.id}_${it.periodNumber}" }
                ) { lesson ->
                    val matchingSub = remember(selectedDay, lesson, substitutions) {
                        TimetableRepository.findMatchingSubstitution(selectedDay, lesson, substitutions)
                    }
                    val isCurrent = isToday && isLessonCurrentlyRunning(lesson.timeRange, currentMinutes)

                    WearTimetableLessonCard(
                        lesson = lesson,
                        matchingSub = matchingSub,
                        isCurrent = isCurrent,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                    )
                }
            }
        }
    }
}

@Composable
private fun WearTimetableLessonCard(
    lesson: TimetableLesson,
    matchingSub: SubstitutionEntry?,
    isCurrent: Boolean,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier
) {
    val isCancelled = matchingSub?.type?.equals("Entfall", ignoreCase = true) == true
    val hasSubstitution = matchingSub != null && !isCancelled
    val resolvedSubject = TimetableRepository.resolveSubjectName(lesson.subject)
    val subjectColor = SubjectColor.getColor(
        subject = lesson.subject,
        isDark = true,
        fallback = MaterialTheme.colorScheme.primary
    )
    val teacherDisplay = lesson.teacherCode.takeIf { it.isNotBlank() }?.let {
        TimetableRepository.getTeacherDisplayName(it)
    }.orEmpty()

    val containerColor = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        onClick = {},
        enabled = false,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = if (isCurrent) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        transformation = transformation,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Obere Zeile: Stunde & Zeit + Status-Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${lesson.periodNumber}. Std • ${lesson.timeRange}",
                    fontFamily = GoogleSansRounded,
                    fontSize = 11.sp,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                when {
                    isCancelled -> {
                        WearStatusBadge(
                            text = "Entfall",
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    }
                    hasSubstitution -> {
                        WearStatusBadge(
                            text = matchingSub?.type ?: "Vertretung",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    isCurrent -> {
                        WearStatusBadge(
                            text = "Jetzt",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Hauptzeile: Fach-Farbpunkt + Fachname
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isCancelled) MaterialTheme.colorScheme.error else subjectColor)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = resolvedSubject,
                    fontFamily = GoogleSansRounded,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Untere Zeile: Raum & Lehrkraft oder Vertretungs-Detail
            val metaText = buildString {
                if (lesson.room.isNotBlank()) append("Raum ${lesson.room}")
                if (teacherDisplay.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(teacherDisplay)
                }
            }

            if (metaText.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = metaText,
                    fontFamily = GoogleSansRounded,
                    fontSize = 12.sp,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (matchingSub != null && matchingSub.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = matchingSub.details,
                    fontFamily = GoogleSansRounded,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCancelled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (lesson.breakAfterMinutes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lesson.breakLabel ?: "${lesson.breakAfterMinutes} Min. Pause danach",
                    fontFamily = GoogleSansRounded,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun WearStatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontFamily = GoogleSansRounded,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1
        )
    }
}

private fun isLessonCurrentlyRunning(timeRange: String, currentMinutes: Int): Boolean {
    val parts = timeRange.split("-")
    if (parts.size < 2) return false
    val start = parseMinutes(parts[0]) ?: return false
    val end = parseMinutes(parts[1]) ?: return false
    return currentMinutes in start until end
}

private fun parseMinutes(raw: String): Int? {
    val segs = raw.trim().split(":")
    if (segs.size < 2) return null
    val h = segs[0].trim().toIntOrNull() ?: return null
    val m = segs[1].trim().take(2).toIntOrNull() ?: return null
    return h * 60 + m
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun WearTimetableScreenPreview() {
    WearAppTheme(useDynamicColors = false) {
        WearTimetableScreenContent(
            selectedDay = TimetableDay.MONTAG,
            userClass = "09AR",
            lessons = TimetableRepository.getDefaultLessonsForDay(TimetableDay.MONTAG),
            substitutions = SubstitutionRepository.defaultEntries.take(2),
            isRefreshing = false,
            onSelectDay = {},
            onRefreshClick = {}
        )
    }
}
