package de.kjltowinkler.msapp.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

data class LanguageOption(
    val code: String,
    val displayName: String
)

val AvailableLanguages = listOf(
    LanguageOption("de-DE", "Deutsch (Deutschland)"),
    LanguageOption("en-US", "Englisch (United Stats)"),
    LanguageOption("en-GB", "Englisch (United Kingdom)"),
    LanguageOption("fr-FR", "Französisch (Frankreich)"),
    LanguageOption("es-ES", "Spanisch (Spanien)"),
    LanguageOption("tr-TR", "Türkisch (Türkei)"),
    LanguageOption("it-IT", "Italienisch (Italien)"),
    LanguageOption("uk-UA", "Ukrainisch (Ukraine)"),
    LanguageOption("ar", "Arabisch"),
    LanguageOption("pl-PL", "Polnisch (Polen)"),
    LanguageOption("ru-RU", "Russisch")
)

data class SchoolOption(
    val id: String,
    val displayName: String
)

val AvailableSchools = listOf(
    SchoolOption("merianschule", "Merianschule-Seligenstadt")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingWelcomeScreen(
    onNextClicked: () -> Unit = {},
    onAccessibilityClicked: () -> Unit = {},
    onLanguageSelected: (LanguageOption) -> Unit = {},
    onSchoolSelected: (SchoolOption) -> Unit = {}
) {
    var selectedLanguage by remember { mutableStateOf(AvailableLanguages[0]) }
    var isLanguageMenuExpanded by remember { mutableStateOf(false) }
    var selectedSchool by remember { mutableStateOf(AvailableSchools[0]) }
    var isSchoolMenuExpanded by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    // Exakte Linienfarbe aus schulapp.fig: #B1C5AE mit Transparenz
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Exakte organische Linien aus schulapp.fig (Blume oben + Kreisbogen)
        OnboardingExactBackgroundArt(lineColor = lineColor)

        // Screen Inhalt
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Freiraum oben, platziert den Textblock exakt wie in Figma onboarding#1 (ab ~53% Bildschirmhöhe)
            Spacer(modifier = Modifier.weight(2.2f))

            // 1. Headline
            Text(
                text = "Willkommen in deiner\nneuen Schulapp!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            // Fester Abstand zwischen Überschrift und Sprachanzeige (exakt wie in Figma)
            Spacer(modifier = Modifier.height(24.dp))

            // 2. Sprachauswahl Trigger & Dropdown-Menü
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isLanguageMenuExpanded = !isLanguageMenuExpanded }
                        .padding(start = 2.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_language),
                        contentDescription = "Sprache auswählen",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedLanguage.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Material 3 DropdownMenu mit allen scrollbaren Sprachen
                DropdownMenu(
                    expanded = isLanguageMenuExpanded,
                    onDismissRequest = { isLanguageMenuExpanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .widthIn(min = 241.dp, max = 290.dp)
                        .heightIn(max = 280.dp)
                ) {
                    AvailableLanguages.forEach { language ->
                        val isSelected = language.code == selectedLanguage.code
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = language.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_play_arrow),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(25.dp)
                                )
                            },
                            onClick = {
                                selectedLanguage = language
                                onLanguageSelected(language)
                                isLanguageMenuExpanded = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isSelected) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                                    else Modifier
                                )
                        )
                    }
                }
            }

            // Fester Abstand zwischen Sprache und Schulauswahl
            Spacer(modifier = Modifier.height(20.dp))

            // 3. Schulauswahl Trigger & Dropdown-Menü (gemerged aus Onboarding #2)
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isSchoolMenuExpanded = !isSchoolMenuExpanded }
                        .padding(start = 2.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_school),
                        contentDescription = "Schule auswählen",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedSchool.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Material 3 DropdownMenu mit allen scrollbaren Schulen
                DropdownMenu(
                    expanded = isSchoolMenuExpanded,
                    onDismissRequest = { isSchoolMenuExpanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 340.dp)
                        .heightIn(max = 280.dp)
                ) {
                    AvailableSchools.forEach { school ->
                        val isSelected = school.id == selectedSchool.id
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = school.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_play_arrow),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(25.dp)
                                )
                            },
                            onClick = {
                                selectedSchool = school
                                onSchoolSelected(school)
                                isSchoolMenuExpanded = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isSelected) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                                    else Modifier
                                )
                        )
                    }
                }
            }

            // Puffer nach unten zu den Buttons, der Platz für das offene Dropdown lässt
            Spacer(modifier = Modifier.weight(1f))

            // 4. Bottom Bar Buttons aus Figma (Höhe 69dp, 89dp Barrierefreiheit, Rest Weiter)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(69.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        showAccessibilityDialog = true
                        onAccessibilityClicked()
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .width(89.dp)
                        .fillMaxHeight()
                        .semantics { contentDescription = "Barrierefreiheit" }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_accessibility),
                        contentDescription = "Barrierefreiheit",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Button(
                    onClick = onNextClicked,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { contentDescription = "Weiter" }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_forward),
                        contentDescription = "Weiter",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(31.dp)
                    )
                }
            }
        }

        // Schnelldialog für Barrierefreiheit
        if (showAccessibilityDialog) {
            AlertDialog(
                onDismissRequest = { showAccessibilityDialog = false },
                title = {
                    Text(
                        text = "Barrierefreiheit",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Normal
                        )
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Passe die Schulapp an deine Bedürfnisse an:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal
                            )
                        )
                        Text(
                            text = "• Größere Schrift & dynamischer Text\n• Hoher Kontrast & Reduzierte Bewegung\n• Screenreader-Unterstützung",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAccessibilityDialog = false }) {
                        Text(
                            text = "Verstanden",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun OnboardingExactBackgroundArt(lineColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "WelcomeBackgroundArtTransition")

    // 1. Blume / Wellenform oben
    val flowerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 42000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FlowerRotation"
    )

    val flowerScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlowerScale"
    )

    val flowerShiftY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlowerShiftY"
    )

    // 2. Kreisbogen / Ellipse unten
    val circleRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CircleRotation"
    )

    val circleScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CircleScale"
    )

    val circleShiftX by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CircleShiftX"
    )

    val circleShiftY by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CircleShiftY"
    )

    val baseCirclePath = remember {
        Path().apply {
            moveTo(423.31f, 794.31f)
            cubicTo(254.40f, 963.22f, 29.14f, 1011.80f, -79.83f, 902.83f)
            moveTo(-79.83f, 902.83f)
            cubicTo(-188.80f, 793.86f, -140.22f, 568.60f, 28.69f, 399.69f)
            moveTo(28.69f, 399.69f)
            cubicTo(197.60f, 230.78f, 422.86f, 182.20f, 531.83f, 291.17f)
            moveTo(531.83f, 291.17f)
            cubicTo(640.80f, 400.14f, 592.22f, 625.40f, 423.31f, 794.31f)
        }
    }

    val baseFlowerPath = remember {
        Path().apply {
            moveTo(174.53f, -90.91f)
            cubicTo(175.51f, -90.69f, 176.01f, -90.57f, 176.49f, -90.47f)
            moveTo(211.00f, -94.89f)
            cubicTo(210.09f, -94.46f, 209.64f, -94.24f, 209.19f, -94.04f)
            moveTo(176.49f, -90.47f)
            cubicTo(187.49f, -88.14f, 198.96f, -89.39f, 209.19f, -94.04f)
            moveTo(111.47f, -59.89f)
            cubicTo(112.41f, -61.94f, 112.87f, -62.96f, 113.32f, -63.85f)
            moveTo(174.53f, -90.91f)
            cubicTo(172.33f, -91.42f, 171.24f, -91.68f, 170.25f, -91.86f)
            moveTo(113.32f, -63.85f)
            cubicTo(123.87f, -84.86f, 147.12f, -96.30f, 170.25f, -91.86f)
            moveTo(86.12f, -33.44f)
            cubicTo(87.02f, -33.90f, 87.47f, -34.13f, 87.90f, -34.36f)
            moveTo(111.47f, -59.89f)
            cubicTo(111.06f, -58.97f, 110.85f, -58.51f, 110.64f, -58.07f)
            moveTo(87.90f, -34.36f)
            cubicTo(97.83f, -39.63f, 105.80f, -47.94f, 110.64f, -58.07f)
            moveTo(57.84f, 30.80f)
            cubicTo(57.24f, 28.63f, 56.93f, 27.54f, 56.70f, 26.57f)
            moveTo(86.12f, -33.44f)
            cubicTo(84.11f, -32.42f, 83.11f, -31.91f, 82.23f, -31.43f)
            moveTo(56.70f, 26.57f)
            cubicTo(51.24f, 3.70f, 61.67f, -19.99f, 82.23f, -31.43f)
            moveTo(55.47f, 67.33f)
            cubicTo(55.86f, 66.41f, 56.06f, 65.95f, 56.24f, 65.49f)
            moveTo(57.84f, 30.80f)
            cubicTo(58.11f, 31.77f, 58.25f, 32.25f, 58.37f, 32.72f)
            moveTo(56.24f, 65.49f)
            cubicTo(60.45f, 55.08f, 61.19f, 43.60f, 58.37f, 32.72f)
            moveTo(75.20f, 134.73f)
            cubicTo(73.34f, 133.45f, 72.41f, 132.82f, 71.61f, 132.22f)
            moveTo(55.47f, 67.33f)
            cubicTo(54.59f, 69.41f, 54.15f, 70.44f, 53.79f, 71.37f)
            moveTo(71.61f, 132.22f)
            cubicTo(52.70f, 118.19f, 45.42f, 93.34f, 53.79f, 71.37f)
            moveTo(96.92f, 164.26f)
            cubicTo(96.63f, 163.30f, 96.48f, 162.82f, 96.33f, 162.36f)
            moveTo(75.20f, 134.73f)
            cubicTo(76.03f, 135.30f, 76.45f, 135.58f, 76.85f, 135.87f)
            moveTo(96.33f, 162.36f)
            cubicTo(92.84f, 151.68f, 86.01f, 142.40f, 76.85f, 135.87f)
            moveTo(155.43f, 203.28f)
            cubicTo(153.19f, 203.50f, 152.06f, 203.61f, 151.06f, 203.66f)
            moveTo(96.92f, 164.26f)
            cubicTo(97.58f, 166.42f, 97.91f, 167.50f, 98.24f, 168.44f)
            moveTo(151.06f, 203.66f)
            cubicTo(127.55f, 205.03f, 105.98f, 190.65f, 98.24f, 168.44f)
            moveTo(191.08f, 211.99f)
            cubicTo(190.23f, 211.45f, 189.81f, 211.17f, 189.39f, 210.91f)
            moveTo(155.43f, 203.28f)
            cubicTo(156.43f, 203.18f, 156.93f, 203.14f, 157.42f, 203.10f)
            moveTo(189.39f, 210.91f)
            cubicTo(179.84f, 204.96f, 168.64f, 202.23f, 157.42f, 203.10f)
            moveTo(260.99f, 204.37f)
            cubicTo(259.41f, 205.98f, 258.62f, 206.78f, 257.89f, 207.47f)
            moveTo(191.08f, 211.99f)
            cubicTo(192.97f, 213.22f, 193.92f, 213.84f, 194.77f, 214.35f)
            moveTo(257.89f, 207.47f)
            cubicTo(240.78f, 223.60f, 215.00f, 226.41f, 194.77f, 214.35f)
            moveTo(293.88f, 188.19f)
            cubicTo(292.88f, 188.31f, 292.38f, 188.37f, 291.90f, 188.44f)
            moveTo(260.99f, 204.37f)
            cubicTo(261.69f, 203.66f, 262.05f, 203.30f, 262.40f, 202.95f)
            moveTo(291.90f, 188.44f)
            cubicTo(280.76f, 190.00f, 270.42f, 195.09f, 262.40f, 202.95f)
            moveTo(342.48f, 137.50f)
            cubicTo(342.31f, 139.74f, 342.22f, 140.87f, 342.11f, 141.86f)
            moveTo(293.88f, 188.19f)
            cubicTo(296.12f, 187.92f, 297.24f, 187.78f, 298.23f, 187.62f)
            moveTo(342.11f, 141.86f)
            cubicTo(339.40f, 165.20f, 321.48f, 183.90f, 298.23f, 187.62f)
            moveTo(357.23f, 103.99f)
            cubicTo(356.55f, 104.72f, 356.20f, 105.09f, 355.88f, 105.45f)
            moveTo(342.48f, 137.50f)
            cubicTo(342.56f, 136.50f, 342.60f, 135.99f, 342.65f, 135.51f)
            moveTo(355.88f, 105.45f)
            cubicTo(348.36f, 113.80f, 343.72f, 124.33f, 342.65f, 135.51f)
            moveTo(361.78f, 33.94f)
            cubicTo(363.10f, 35.78f, 363.75f, 36.69f, 364.30f, 37.53f)
            moveTo(357.23f, 103.99f)
            cubicTo(358.77f, 102.34f, 359.54f, 101.52f, 360.20f, 100.76f)
            moveTo(364.30f, 37.53f)
            cubicTo(377.27f, 57.16f, 375.59f, 82.99f, 360.20f, 100.76f)
            moveTo(351.49f, -1.21f)
            cubicTo(351.44f, -0.21f, 351.41f, 0.29f, 351.39f, 0.78f)
            moveTo(361.78f, 33.94f)
            cubicTo(361.20f, 33.13f, 360.90f, 32.72f, 360.62f, 32.31f)
            moveTo(351.39f, 0.78f)
            cubicTo(351.02f, 12.00f, 354.25f, 23.06f, 360.62f, 32.31f)
            moveTo(309.86f, -57.83f)
            cubicTo(312.04f, -57.27f, 313.13f, -56.99f, 314.09f, -56.70f)
            moveTo(351.49f, -1.21f)
            cubicTo(351.61f, -3.46f, 351.67f, -4.58f, 351.68f, -5.58f)
            moveTo(314.09f, -56.70f)
            cubicTo(336.66f, -49.96f, 352.02f, -29.08f, 351.68f, -5.58f)
            moveTo(279.33f, -78.18f)
            cubicTo(279.94f, -77.38f, 280.24f, -76.98f, 280.55f, -76.59f)
            moveTo(309.86f, -57.83f)
            cubicTo(308.88f, -58.08f, 308.39f, -58.21f, 307.92f, -58.34f)
            moveTo(280.55f, -76.59f)
            cubicTo(287.48f, -67.75f, 297.08f, -61.35f, 307.92f, -58.34f)
            moveTo(279.33f, -78.18f)
            cubicTo(277.98f, -79.98f, 277.30f, -80.88f, 276.67f, -81.66f)
            moveTo(211.00f, -94.89f)
            cubicTo(213.04f, -95.86f, 214.06f, -96.34f, 214.97f, -96.74f)
            moveTo(276.67f, -81.66f)
            cubicTo(261.79f, -99.89f, 236.59f, -106.05f, 214.97f, -96.74f)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scaleX = size.width / 412f
        val scaleY = size.height / 917f
        val avgScale = (scaleX + scaleY) * 0.5f
        val stroke = Stroke(width = 1.dp.toPx() / avgScale, cap = StrokeCap.Round)

        // 1. Kreisbogen / Ellipse (Figma shape 502)
        withTransform({
            scale(scaleX, scaleY, pivot = Offset.Zero)
            translate(circleShiftX, circleShiftY)
            rotate(circleRotation, pivot = Offset(226f, 597f))
            scale(circleScale, circleScale, pivot = Offset(226f, 597f))
        }) {
            drawPath(
                path = baseCirclePath,
                color = lineColor,
                style = stroke
            )
        }

        // 2. Blume / Wellenform oben (Figma shape 504)
        withTransform({
            scale(scaleX, scaleY, pivot = Offset.Zero)
            translate(0f, flowerShiftY)
            rotate(flowerRotation, pivot = Offset(211.35f, 60.18f))
            scale(flowerScale, flowerScale, pivot = Offset(211.35f, 60.18f))
        }) {
            drawPath(
                path = baseFlowerPath,
                color = lineColor,
                style = stroke
            )
        }
    }
}