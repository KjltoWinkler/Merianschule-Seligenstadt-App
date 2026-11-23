/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.kjltowinkler.msapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.news.NewsItem
import de.kjltowinkler.msapp.data.news.NewsType
import de.kjltowinkler.msapp.ui.components.handCursor
import de.kjltowinkler.msapp.ui.news.NewsImageView
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Material 3 Multi-Browse Karussell entsprechend der offiziellen Compose Material 3 Carousel Spezifikation
 * und `FigmaDesigns/mobile/startseite.png`.
 *
 * Zeigt Neuigkeiten und Schul-Events als flüssig morphendes Karussell mit `HorizontalMultiBrowseCarousel`,
 * `rememberCarouselState` und dynamischem `maskClip(MaterialTheme.shapes.extraLarge)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartseiteCarousel(
    newsItems: List<NewsItem>,
    onItemClick: (NewsItem) -> Unit,
    modifier: Modifier = Modifier,
    preferredItemWidth: Dp = 310.dp,
    carouselHeight: Dp = 220.dp
) {
    if (newsItems.isEmpty()) return

    val state = rememberCarouselState { newsItems.size }
    val animationScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalMultiBrowseCarousel(
            state = state,
            preferredItemWidth = preferredItemWidth,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight)
        ) { i ->
            val item = newsItems[i]
            StartseiteCarouselCard(
                item = item,
                onClick = {
                    if (state.currentItem == i) {
                        onItemClick(item)
                    } else {
                        animationScope.launch {
                            state.animateScrollToItem(i)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
            )
        }

        // Subtiler Pager-Indikator
        if (newsItems.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(newsItems.size.coerceAtMost(6)) { index ->
                    val isSelected = state.currentItem == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (isSelected) 18.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Einzelne Karte im Karussell mit abgerundeter Maske (`maskClip`), Bildhintergrund oder geometrischem
 * Ersatzmuster, Gradient und Typografie gemäß Figma-Design.
 */
@Composable
fun StartseiteCarouselCard(
    item: NewsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = item.title
            )
            .handCursor()
    ) {
        if (!item.imageUrl.isNullOrBlank()) {
            NewsImageView(
                imageUrl = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dunkler Verlaufsfilter für optimale Lesbarkeit des Titels
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                val tagText = when (item.type) {
                    NewsType.EVENT -> "Event"
                    NewsType.ANNOUNCEMENT -> "Wichtig"
                    NewsType.NEWS -> "Neuigkeit"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (item.dateText.isNotBlank()) {
                        Text(
                            text = item.dateText,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // Gestaltung mit geometrischen Shapes entsprechend `startseite.png`
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.bg_desktop_shapes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(0.65f)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )

                // Text-Badge unten
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
