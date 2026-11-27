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
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.substitution.SubstitutionRepository
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.util.AppContextProvider

/**
 * Wear OS Tile für den Vertretungsplan (Google Material 3 ProtoLayout).
 * Nutzt die gesamte verfügbare Fläche durch expandierende, gestapelte Karten.
 */
class SubstitutionTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        AppContextProvider.init(applicationContext)
        BackendApiClient.loadSavedSession()
        val entries = if (BackendApiClient.isLoggedIn) {
            SubstitutionRepository.getInitialOrCachedEntries()
        } else {
            emptyList()
        }
        val tile = buildSubstitutionTile(
            context = applicationContext,
            deviceConfiguration = requestParams.deviceConfiguration,
            entries = entries,
            isLoggedIn = BackendApiClient.isLoggedIn
        )
        return immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = immediateFuture(emptyResources())
}

internal fun buildSubstitutionTile(
    context: Context,
    deviceConfiguration: androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters,
    entries: List<SubstitutionEntry>,
    isLoggedIn: Boolean = true
): TileBuilders.Tile {
    if (!isLoggedIn) {
        return buildLoggedOutTile(
            context = context,
            deviceConfiguration = deviceConfiguration,
            title = "Vertretungsplan",
            route = "substitution"
        )
    }
    val userClass = TimetableRepository.getEffectiveUserClass()
    val normalizedClass = userClass.trim().lowercase().removePrefix("0")
    val myEntries = entries.filter { entry ->
        val c = entry.className.lowercase()
        c.contains(userClass.lowercase()) || (normalizedClass.isNotBlank() && c.contains(normalizedClass))
    }

    val openSubstitution = launchRouteClickable(context, "substitution", "open_substitution")

    val layout = materialScope(
        context = context,
        deviceConfiguration = deviceConfiguration
    ) {
        primaryLayout(
            titleSlot = { tileTitleText("Vertretung • $userClass") },
            mainSlot = {
                if (myEntries.isEmpty()) {
                    textDataCard(
                        onClick = openSubstitution,
                        width = expand(),
                        height = expand(),
                        title = {
                            text(
                                text = "Keine Vertretungen".layoutString,
                                typography = Typography.TITLE_MEDIUM,
                                maxLines = 1
                            )
                        },
                        content = {
                            text(
                                text = "Unterricht findet regulär statt".layoutString,
                                typography = Typography.BODY_SMALL,
                                maxLines = 2
                            )
                        },
                        colors = tonalCardColors()
                    )
                } else if (myEntries.size == 1) {
                    substitutionTileCard(
                        entry = myEntries.first(),
                        onClick = openSubstitution,
                        compact = false
                    )
                } else {
                    val topTwo = myEntries.take(2)
                    LayoutElementBuilders.Column.Builder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .addContent(
                            substitutionTileCard(
                                entry = topTwo[0],
                                onClick = openSubstitution,
                                compact = true
                            )
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setHeight(dp(4f))
                                .build()
                        )
                        .addContent(
                            substitutionTileCard(
                                entry = topTwo[1],
                                onClick = openSubstitution,
                                compact = true
                            )
                        )
                        .build()
                }
            },
            bottomSlot = {
                textEdgeButton(
                    onClick = openSubstitution,
                    labelContent = {
                        val label = if (myEntries.size > 2) {
                            "Alle (${myEntries.size})"
                        } else {
                            "Öffnen"
                        }
                        text(label.layoutString)
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

private fun androidx.wear.protolayout.material3.MaterialScope.substitutionTileCard(
    entry: SubstitutionEntry,
    onClick: androidx.wear.protolayout.ModifiersBuilders.Clickable,
    compact: Boolean
): LayoutElementBuilders.LayoutElement {
    val subject = TimetableRepository.resolveSubjectName(entry.subject).ifBlank { entry.subject }
    val isCancelled = entry.type.equals("Entfall", ignoreCase = true)

    return textDataCard(
        onClick = onClick,
        width = expand(),
        height = expand(),
        title = {
            text(
                text = "${entry.period} • $subject".layoutString,
                typography = if (compact) Typography.TITLE_SMALL else Typography.TITLE_MEDIUM,
                maxLines = 1
            )
        },
        content = {
            text(
                text = entry.details.ifBlank { entry.type }.layoutString,
                typography = Typography.BODY_SMALL,
                maxLines = if (compact) 1 else 2
            )
        },
        colors = if (isCancelled) errorCardColors() else tonalCardColors()
    )
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
private fun substitutionTilePreview(context: Context) = TilePreviewData(
    onTileRequest = { request ->
        buildSubstitutionTile(
            context = context,
            deviceConfiguration = request.deviceConfiguration,
            entries = SubstitutionRepository.defaultEntries.take(3)
        )
    }
)
