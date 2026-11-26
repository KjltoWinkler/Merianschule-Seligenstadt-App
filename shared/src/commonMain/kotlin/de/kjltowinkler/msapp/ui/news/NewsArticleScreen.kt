package de.kjltowinkler.msapp.ui.news

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.kjltowinkler.msapp.data.news.NewsItem
import de.kjltowinkler.msapp.data.news.NewsType
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import de.kjltowinkler.msapp.ui.components.handCursor
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Vollständige Einzelseite für einen Artikel der Merianschule.
 * Unterstützt ein Bilder-Karussell (HorizontalPager), wenn mehrere Bilder vorhanden sind,
 * Vollbildansicht beim Anklicken eines Bildes mit stufenlosem Zoom (Pinch & Double-Tap)
 * und weiterem Durchblättern, sowie vollständigen formatierten Text, Metadaten und Zurück-Navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsArticleScreen(
    item: NewsItem,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val images = remember(item) { item.allImages }
    val pagerState = rememberPagerState { images.size }
    val coroutineScope = rememberCoroutineScope()
    var fullscreenImageIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Beitrag",
                        fontFamily = getQuicksandFontFamily(),
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier.handCursor()
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(bottom = 40.dp)
        ) {
            // 1. Bilder-Bereich (Karussell bei mehreren Bildern oder Einzelbild)
            if (images.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (images.size > 1) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(24.dp))
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { fullscreenImageIndex = page }
                                            .handCursor()
                                    ) {
                                        NewsImageView(
                                            imageUrl = images[page],
                                            contentDescription = "${item.title} - Bild ${page + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                // Bildzähler-Badge ("1 / 6")
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / ${images.size}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Karussell-Indikatoren (Punkte)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(images.size) { index ->
                                    val isSelected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 10.dp else 7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant
                                                }
                                            )
                                            .clickable {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                            .handCursor()
                                    )
                                }
                            }
                        }
                    } else {
                        // Einzelnes Bild
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { fullscreenImageIndex = 0 }
                                .handCursor()
                        ) {
                            NewsImageView(
                                imageUrl = images.first(),
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Metadaten (Kategorie, Datum, Autor)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

                // Titel
                Text(
                    text = item.title,
                    fontFamily = getQuicksandFontFamily(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 32.sp
                )

                if (item.author.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Von: ${item.author}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(18.dp))

                // 3. Artikel-Text
                val paragraphs = remember(item.description) {
                    item.description.split("\n\n").filter { it.isNotBlank() }
                }

                if (paragraphs.isNotEmpty()) {
                    paragraphs.forEachIndexed { index, paragraph ->
                        Text(
                            text = paragraph.trim(),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 26.sp
                        )
                        if (index < paragraphs.size - 1) {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                } else {
                    Text(
                        text = item.description,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 26.sp
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // 4. Zurück-Button am Ende des Beitrags
                OutlinedButton(
                    onClick = onBackClicked,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .handCursor()
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zurück zur Übersicht",
                        fontFamily = getQuicksandFontFamily(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    // 5. Vollbild-Galerie-Dialog beim Klick auf ein Bild mit Zoom & Durchblättern
    fullscreenImageIndex?.let { startIndex ->
        FullscreenImageGalleryDialog(
            images = images,
            initialIndex = startIndex,
            onDismissRequest = { fullscreenImageIndex = null }
        )
    }
}

/**
 * Vollbild-Bildergalerie mit Durchblättern per Swipe (HorizontalPager), Klickpfeilen
 * und Zoom-Unterstützung (Pinch-to-Zoom, Doppel-Tap, Drag/Pan und Reset).
 */
@Composable
fun FullscreenImageGalleryDialog(
    images: List<String>,
    initialIndex: Int,
    onDismissRequest: () -> Unit
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, images.size - 1)) { images.size }
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Beim Seitenwechsel Zoom und Bildverschiebung zurücksetzen
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    val isZoomed = scale > 1.05f

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Bilder-Pager mit horizontalem Swipe bei Normalgröße und Pan bei Zoom
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isZoomed,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(page) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val targetScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = targetScale
                                if (targetScale > 1.05f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .pointerInput(page) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2.5f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        NewsImageView(
                            imageUrl = images[page],
                            contentDescription = "Vollbild ${page + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 60.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Obere Steuerungsleiste (Zähler + Zoom-Anzeige / Reset + Schließen-Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${images.size}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isZoomed) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isZoomed) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            TextButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                modifier = Modifier.handCursor()
                            ) {
                                Text(
                                    text = "1x",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_close),
                                contentDescription = "Schließen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Navigationspfeile links und rechts (wenn nicht gezoomt oder zur schnellen Navigation)
            if (images.size > 1 && !isZoomed) {
                if (pagerState.currentPage > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_arrow_back),
                                contentDescription = "Vorheriges Bild",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (pagerState.currentPage < images.size - 1) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier.handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_arrow_forward),
                                contentDescription = "Nächstes Bild",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Unten: Indikator-Punkte
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(images.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.4f))
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .handCursor()
                        )
                    }
                }
            }
        }
    }
}
