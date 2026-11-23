package de.kjltowinkler.msapp.ui.news

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.news.CreateNewsRequest
import de.kjltowinkler.msapp.data.news.NewsItem
import de.kjltowinkler.msapp.data.news.NewsRepository
import de.kjltowinkler.msapp.data.news.NewsType
import de.kjltowinkler.msapp.data.task.getCurrentEpochMillis
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import de.kjltowinkler.msapp.ui.components.handCursor
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Vollständige Neuigkeiten- & Event-Übersicht entsprechend den Figma-Vorlagen:
 * - `FigmaDesigns/mobile/neuigkeiten#few.png`
 * - `FigmaDesigns/mobile/neuigkeiten#fewup.png`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuigkeitenScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    forceSkeleton: Boolean = false
) {
    val newsList by NewsRepository.newsFlow.collectAsState()
    val isLoading by NewsRepository.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()

    var selectedNewsItem by remember { mutableStateOf<NewsItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val showSkeleton = forceSkeleton || (isLoading && newsList.isEmpty())

    AnimatedContent(
        targetState = showSkeleton,
        transitionSpec = {
            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "articles_skeleton_transition"
    ) { isSkeleton ->
        if (isSkeleton) {
            ArticlesSkeletonScreen(
                onBackClicked = onBackClicked,
                modifier = modifier
            )
        } else {
            val isTeacherOrAdmin = remember {
                val role = BackendApiClient.currentUser?.role?.uppercase()
                role in listOf("TEACHER", "ADMIN", "LEHRER")
            }

            val featuredNews = remember(newsList) {
                val featured = newsList.filter { it.isFeatured || !it.imageUrl.isNullOrBlank() }
                if (featured.isNotEmpty()) featured else newsList.take(3)
            }

            val regularNews = remember(newsList, featuredNews) {
                newsList.filter { it !in featuredNews }
            }

            val latestDate = remember(newsList) {
                newsList.firstOrNull()?.dateText?.takeIf { it.isNotBlank() } ?: "Stand 21.11.2026"
            }

            val scrollState = rememberScrollState()
            val density = LocalDensity.current
            val bannerHeight = 236.dp
            val transitionDistance = with(density) { (bannerHeight - 64.dp).toPx() }
            val scrollProgress = if (transitionDistance > 0f) {
                (scrollState.value.toFloat() / transitionDistance).coerceIn(0f, 1f)
            } else 1f
            val titleAlpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
            val isScrolledPast = scrollProgress >= 0.85f

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { NewsRepository.loadNews(forceRefresh = true) },
                    state = pullState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = isLoading,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 64.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Oberstes Bild (Hero-Banner entsprechend `neuigkeiten#fewup.png`)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(bannerHeight)
                                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.news_header_banner),
                                contentDescription = "Schul-Neuigkeiten Header",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Subtiler Verlauf oben für Statusleiste / Zurück-Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.5f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }

                        // 2. Inhaltsbereich entsprechend `neuigkeiten#fewup.png` und `neuigkeiten#few.png`
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Entdecke die neuesten\nNachrichten",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 30.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Datum-Chip: "Stand 21.11.2026"
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.padding(bottom = 20.dp)
                            ) {
                                Text(
                                    text = if (latestDate.startsWith("Stand")) latestDate else "Stand $latestDate",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            // 1. Prominente Karten (Featured News)
                            featuredNews.forEach { item ->
                                FeaturedNewsCard(
                                    item = item,
                                    onClick = { selectedNewsItem = item },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                )
                            }

                            // 2. Bereich "Weitere Neuigkeiten" aus `neuigkeiten#fewup.png`
                            if (regularNews.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Weitere Neuigkeiten",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 4.dp)
                                )

                                regularNews.forEach { item ->
                                    CompactNewsCard(
                                        item = item,
                                        onClick = { selectedNewsItem = item },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                // Fixierte TopAppBar, deren Titel "Das ist neu" (Quicksand) und Hintergrund
                // sanft eingeblendet werden, sobald das oberste Bild nach oben weggescrollt wird.
                TopAppBar(
                    title = {
                        Text(
                            text = "Das ist neu",
                            fontFamily = getQuicksandFontFamily(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
                            modifier = Modifier.graphicsLayer { alpha = titleAlpha }
                        )
                    },
                    navigationIcon = {
                        Surface(
                            shape = CircleShape,
                            color = if (isScrolledPast) Color.Transparent else Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                        ) {
                            IconButton(
                                onClick = onBackClicked,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .handCursor()
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_arrow_back),
                                    contentDescription = "Zurück",
                                    tint = if (isScrolledPast) MaterialTheme.colorScheme.onSurface else Color.White
                                )
                            }
                        }
                    },
                    actions = {
                        if (isTeacherOrAdmin) {
                            Surface(
                                shape = CircleShape,
                                color = if (isScrolledPast) Color.Transparent else Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(40.dp)
                            ) {
                                IconButton(
                                    onClick = { showCreateDialog = true },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .handCursor()
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_add),
                                        contentDescription = "Beitrag erstellen",
                                        tint = if (isScrolledPast) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = scrollProgress),
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }

    // Vollständige Artikel-Seite für einen angeklickten Beitrag mit Karussell
    AnimatedVisibility(
        visible = selectedNewsItem != null,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(150))
    ) {
        selectedNewsItem?.let { item ->
            NewsArticleScreen(
                item = item,
                onBackClicked = { selectedNewsItem = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Erstellen-Dialog für Lehrkräfte & Administratoren
    if (showCreateDialog) {
        CreateNewsDialog(
            onDismissRequest = { showCreateDialog = false },
            onSave = { request ->
                scope.launch {
                    NewsRepository.createNews(request)
                    showCreateDialog = false
                }
            }
        )
    }
}

/**
 * Große Bildkarte entsprechend `neuigkeiten#few.png`
 */
@Composable
private fun FeaturedNewsCard(
    item: NewsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(20.dp)

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .clip(cardShape)
            .clickable(onClick = onClick)
            .handCursor()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Bild oben
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                NewsImageView(
                    imageUrl = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Titel & Beschreibung unten
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

/**
 * Kompakte Karte für "Weitere Neuigkeiten" mit Tag-Icon links entsprechend `neuigkeiten#fewup.png`
 */
@Composable
private fun CompactNewsCard(
    item: NewsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(18.dp)

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .clip(cardShape)
            .clickable(onClick = onClick)
            .handCursor()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon-Badge links mit abgerundeter Form
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_tag_label),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text rechts
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

/**
 * Detailansicht für einen Beitrag (ModalBottomSheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailBottomSheet(
    item: NewsItem,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    NewsImageView(
                        imageUrl = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                val tagText = when (item.type) {
                    NewsType.EVENT -> "Schul-Event"
                    NewsType.ANNOUNCEMENT -> "Ankündigung"
                    NewsType.NEWS -> "Neuigkeit"
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = tagText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (item.dateText.isNotBlank()) {
                    Text(
                        text = item.dateText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = item.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp
            )

            if (item.author.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Von: ${item.author}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = item.description,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Schließen", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Dialog zum Erstellen von Neuigkeiten aus der App heraus (für Admins & Lehrer)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewsDialog(
    onDismissRequest: () -> Unit,
    onSave: (CreateNewsRequest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("21.11.2026") }
    var author by remember { mutableStateOf("Schulleitung") }
    var selectedType by remember { mutableStateOf(NewsType.NEWS) }
    var isFeatured by remember { mutableStateOf(true) }
    var selectedImagePreset by remember { mutableStateOf("res:news_stairs") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Neuen Beitrag verfassen",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung / Inhalt *") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Datum") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Autor") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isFeatured = !isFeatured }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isFeatured,
                        onCheckedChange = { isFeatured = it }
                    )
                    Text(
                        text = "Im Startseiten-Karussell anzeigen",
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onSave(
                            CreateNewsRequest(
                                title = title.trim(),
                                description = description.trim(),
                                imageUrl = selectedImagePreset,
                                type = selectedType,
                                dateText = dateText.trim(),
                                timestamp = getCurrentEpochMillis(),
                                author = author.trim(),
                                isFeatured = isFeatured
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Veröffentlichen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Abbrechen")
            }
        }
    )
}
