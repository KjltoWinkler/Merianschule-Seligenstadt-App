package de.kjltowinkler.msapp.wear.tiles

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.CardColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import com.google.common.util.concurrent.ListenableFuture
import de.kjltowinkler.msapp.wear.presentation.MainActivity
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

internal const val TILE_RESOURCES_VERSION = "1"

internal fun <T : Any> immediateFuture(value: T): ListenableFuture<T> =
    object : ListenableFuture<T> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = true
        override fun get(): T = value
        override fun get(timeout: Long, unit: TimeUnit): T = value
        override fun addListener(listener: Runnable, executor: Executor) {
            executor.execute(listener)
        }
    }

internal fun MaterialScope.primaryCardColors(): CardColors =
    CardColors(
        backgroundColor = colorScheme.primaryContainer,
        titleColor = colorScheme.onPrimaryContainer,
        contentColor = colorScheme.onPrimaryContainer
    )

internal fun MaterialScope.tonalCardColors(): CardColors =
    CardColors(
        backgroundColor = colorScheme.surfaceContainer,
        titleColor = colorScheme.onSurface,
        contentColor = colorScheme.onSurfaceVariant
    )

internal fun MaterialScope.errorCardColors(): CardColors =
    CardColors(
        backgroundColor = colorScheme.errorContainer,
        titleColor = colorScheme.onErrorContainer,
        contentColor = colorScheme.onErrorContainer
    )

internal fun emptyResources(): ResourceBuilders.Resources =
    ResourceBuilders.Resources.Builder()
        .setVersion(TILE_RESOURCES_VERSION)
        .build()

internal fun singleEntryTimeline(
    layoutElement: androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
): TimelineBuilders.Timeline =
    TimelineBuilders.Timeline.fromLayoutElement(layoutElement)

/**
 * Erstellt eine Clickable-Action, die [MainActivity] direkt auf der gewünschten [route] öffnet.
 */
internal fun launchRouteClickable(
    context: Context,
    route: String,
    id: String = "open_$route"
): ModifiersBuilders.Clickable {
    val action = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(context.packageName)
                .setClassName(MainActivity::class.java.name)
                .addKeyToExtraMapping(
                    "route",
                    ActionBuilders.AndroidStringExtra.Builder()
                        .setValue(route)
                        .build()
                )
                .build()
        )
        .build()

    return ModifiersBuilders.Clickable.Builder()
        .setId(id)
        .setOnClick(action)
        .build()
}

internal fun MaterialScope.tileTitleText(title: String) = text(
    text = title.layoutString,
    typography = Typography.TITLE_SMALL,
    color = colorScheme.onSurface,
    maxLines = 1
)
