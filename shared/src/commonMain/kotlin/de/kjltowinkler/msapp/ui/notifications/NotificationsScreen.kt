package de.kjltowinkler.msapp.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.notifications.AppNotification
import de.kjltowinkler.msapp.data.notifications.NotificationRepository
import de.kjltowinkler.msapp.data.notifications.NotificationType
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import de.kjltowinkler.msapp.ui.components.PredictiveBackContainer
import de.kjltowinkler.msapp.ui.components.handCursor
import de.kjltowinkler.msapp.util.NavigationEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Screen für "Benachrichtigungen" exakt nach den Figma-Entwürfen:
 * - `benachrichtigungen#few.png` (Gruppierte Liste mit Swipe-to-Archive)
 * - `benachrichtigungen#leer.png` (Leere Ansicht mit zentrierter Meldung)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    useMaterialYou: Boolean = true,
    onNotificationClick: ((AppNotification) -> Unit)? = null
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    PredictiveBackContainer(
        enabled = showOverflowMenu,
        onBack = { showOverflowMenu = false }
    ) {}

    // Exakte Figma-Farbwerte
    val screenBackground = if (useMaterialYou) {
        MaterialTheme.colorScheme.background
    } else {
        Color(0xFF0C0F0B)
    }

    val cardBackground = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color(0xFF151B15)
    }

    val textPrimary = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFFDFE9DE)
    }

    val textSecondary = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFA5ADA2)
    }

    val categoryHeaderColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFE9F0B1)
    }

    val groupedNotifications = NotificationRepository.getGroupedNotifications()
    val isEmpty = groupedNotifications.isEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- TOP BAR: Zurück, Titel "Benachrichtigungen", 3-Punkte-Menü ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .size(44.dp)
                        .handCursor()
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = "Zurück",
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Benachrichtigungen",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = getQuicksandFontFamily(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal,
                        color = textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier
                            .size(44.dp)
                            .handCursor()
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_more_vert),
                            contentDescription = "Weitere Optionen",
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier.background(cardBackground)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Alle als gelesen markieren",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = textPrimary
                                    )
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                NotificationRepository.markAllAsRead()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Alle Benachrichtigungen löschen",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                NotificationRepository.clearAll()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Standarddaten wiederherstellen",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = textSecondary
                                    )
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                NotificationRepository.resetToDefaults()
                            }
                        )
                    }
                }
            }

            // --- INHALT ---
            if (isEmpty) {
                // Exakt wie im Leerzustand:
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Benachrichtigungen",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondary
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    groupedNotifications.forEach { (dateHeader, itemsInGroup) ->
                        // Überschrift pro Tag (Schriftart wie CategoryHeader in Einstellungen)
                        item(key = "header_$dateHeader") {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = categoryHeaderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp)
                            )
                        }

                        itemsIndexed(
                            items = itemsInGroup,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            // Dynamische Rundung nach M3 Expressive Gruppierung
                            val isSingle = itemsInGroup.size == 1
                            val shape = when {
                                isSingle -> RoundedCornerShape(16.dp)
                                index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                index == itemsInGroup.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                else -> RoundedCornerShape(4.dp)
                            }

                            SwipeableNotificationItem(
                                item = item,
                                shape = shape,
                                cardBackground = cardBackground,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onDismiss = {
                                    NotificationRepository.archiveNotification(item.id)
                                },
                                onClick = {
                                    NotificationRepository.markAsRead(item.id)
                                    if (onNotificationClick != null) {
                                        onNotificationClick(item)
                                    } else {
                                        defaultHandleNotificationClick(item, onBackClicked)
                                    }
                                }
                            )

                            if (index < itemsInGroup.size - 1) {
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Einzelnes Benachrichtigungselement mit Swipe-To-Archive Animation nach Figma `benachrichtigungen#few.png`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationItem(
    item: AppNotification,
    shape: RoundedCornerShape,
    cardBackground: Color,
    textPrimary: Color,
    textSecondary: Color,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isItemLeaving by remember(item.id) { mutableStateOf(false) }
    var actionTriggered by remember(item.id) { mutableStateOf(false) }
    var hasHapticClicked by remember(item.id) { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart && !actionTriggered) {
                actionTriggered = true
                coroutineScope.launch {
                    delay(150)
                    isItemLeaving = true
                    delay(220)
                    onDismiss()
                }
                true
            } else {
                false
            }
        }
    )

    val isThresholdReached = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

    LaunchedEffect(isThresholdReached) {
        if (isThresholdReached && !hasHapticClicked) {
            hasHapticClicked = true
            try {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            } catch (_: Throwable) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else if (!isThresholdReached) {
            hasHapticClicked = false
        }
    }

    AnimatedVisibility(
        visible = !isItemLeaving,
        enter = expandVertically(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 250)),
        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(220))
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }

                // Dynamisches Icon-Verhalten wie in Gmail (analog zum Vertretungsplan)
                val progress = dismissState.progress
                val iconScale = (progress * 1.25f).coerceIn(0.6f, 1.15f)
                val iconAlpha = (progress * 2.5f).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .clip(shape)
                        .background(Color(0xFF6DD58C))
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    if (direction != SwipeToDismissBoxValue.Settled) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_archive),
                            contentDescription = "Archivieren",
                            tint = Color(0xFF0A3818),
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                    alpha = iconAlpha
                                }
                        )
                    }
                }
            }
        ) {
            Surface(
                shape = shape,
                color = cardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(shape)
                    .clickable(onClick = onClick)
                    .handCursor()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondary,
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Standard-Navigation beim Antippen einer Benachrichtigung.
 */
private fun defaultHandleNotificationClick(
    item: AppNotification,
    onBackClicked: () -> Unit
) {
    when (item.type) {
        NotificationType.INVITATION -> {
            onBackClicked()
            NavigationEvents.emit("tasks")
        }
        NotificationType.SUBSTITUTION -> {
            onBackClicked()
            NavigationEvents.emit("substitution", item.targetId)
        }
        NotificationType.TIMETABLE -> {
            onBackClicked()
            NavigationEvents.emit("timetable")
        }
        NotificationType.TASK -> {
            onBackClicked()
            NavigationEvents.emit("tasks", item.targetId)
        }
        NotificationType.GENERAL -> {
            onBackClicked()
        }
    }
}
