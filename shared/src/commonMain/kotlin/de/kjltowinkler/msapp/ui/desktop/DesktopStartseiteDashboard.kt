package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.substitution.SubstitutionStateManager
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.theme.*
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Desktop Startseite / Dashboard für die Merianschule App.
 *
 * Präsentiert eine mehrspaltige Übersicht:
 * - Willkommens-Hero-Banner mit Tageszusammenfassung
 * - Widget "Heutige Stunden & Vertretungen"
 * - Widget "Dringende Aufgaben" mit Direkt-Checkboxes
 * - Widget "Schnellzugriff"
 */
@Composable
fun DesktopStartseiteDashboard(
    accountName: String,
    onNavigateToTab: (Int) -> Unit,
    onAddNewTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingTasks = remember { TasksRepository.tasks }
    val openTasks = pendingTasks.filter { !it.isCompleted }.take(4)
    val substitutions by produceState<List<de.kjltowinkler.msapp.data.substitution.SubstitutionEntry>>(
        initialValue = de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.defaultEntries
    ) {
        val loaded = de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.loadSubstitutions()
        value = if (loaded.isNotEmpty()) loaded else de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.defaultEntries
    }
    val todaySubstitutions = substitutions.filter { it.date == "2026-10-12" || it.isNew }.take(3)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. HERO WILLKOMMENS-BANNER ---
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DesktopCardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222530))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Willkommen zurück, $accountName 👋",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = DesktopTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hier ist deine Übersicht für den heutigen Schultag an der Merianschule Seligenstadt.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = DesktopTextSecondary
                            )
                        )
                    }

                    // Status Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF192535))
                                .border(1.dp, DesktopAccentBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${todaySubstitutions.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DesktopAccentBlue
                                    )
                                )
                                Text(
                                    text = "Vertretungen",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = DesktopTextSecondary
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF192535))
                                .border(1.dp, DesktopAccentBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${openTasks.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DesktopAccentBlue
                                    )
                                )
                                Text(
                                    text = "Offene Aufgaben",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = DesktopTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. ZWEISPALTIGES WIDGET-LAYOUT ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Linkes Widget: Vertretungsplan Vorschau
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = DesktopCardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222530))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_nav_event_note),
                                contentDescription = null,
                                tint = DesktopAccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vertretungsplan heute",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = DesktopTextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { onNavigateToTab(2) },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Text(
                                    text = "Alle ansehen →",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = DesktopAccentBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (todaySubstitutions.isEmpty()) {
                            Text(
                                text = "Keine Vertretungen für heute vorhanden.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = DesktopTextSecondary),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            todaySubstitutions.forEach { subEntry ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF14161C))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (subEntry.type.contains("Entfall", ignoreCase = true)) Color(0xFF4A1818)
                                                else Color(0xFF143048)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = subEntry.period,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (subEntry.type.contains("Entfall", ignoreCase = true)) Color(0xFFFF8888)
                                                else DesktopAccentBlue
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${subEntry.subject} • ${subEntry.type}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = DesktopTextPrimary
                                            )
                                        )
                                        Text(
                                            text = subEntry.details.ifEmpty { subEntry.roomInfo },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                color = DesktopTextSecondary.copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Rechtes Widget: Aufgaben Vorschau
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = DesktopCardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222530))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_nav_checklist_rtl),
                                contentDescription = null,
                                tint = DesktopAccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nächste Aufgaben",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = DesktopTextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { onNavigateToTab(3) },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Text(
                                    text = "Alle Aufgaben →",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = DesktopAccentBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (openTasks.isEmpty()) {
                            Text(
                                text = "Alles erledigt! Keine offenen Aufgaben.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = DesktopTextSecondary),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            openTasks.forEach { task ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF14161C))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { checked ->
                                            TasksRepository.toggleCompleted(task.id)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = DesktopAccentBlue,
                                            checkmarkColor = DesktopOnAccentBlue,
                                            uncheckedColor = DesktopInputBorder
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = DesktopTextPrimary
                                            )
                                        )
                                        if (task.deadlineDate.isNotBlank()) {
                                            Text(
                                                text = "Fällig: ${task.deadlineDate}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.5.sp,
                                                    color = DesktopTextSecondary.copy(alpha = 0.7f)
                                                )
                                            )
                                        }
                                    }

                                    if (task.isStarred) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_star_filled),
                                            contentDescription = "Favorit",
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Button "+ Neue Aufgabe" direkt im Widget
                        OutlinedButton(
                            onClick = onAddNewTask,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .pointerHoverIcon(PointerIcon.Hand),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2F3A)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = DesktopAccentBlue
                            )
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_add),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Neue Aufgabe hinzufügen",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }
    }
}
