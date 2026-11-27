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
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.util.AppContextProvider

/**
 * Wear OS Tile für Aufgaben (Google Keep / Tasks Material 3 Style).
 * Nutzt den gesamten Raum durch expandierende Aufgaben-Karten (bis zu 2 Aufgaben direkt sichtbar).
 */
class TasksTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        AppContextProvider.init(applicationContext)
        val openTasks = TasksRepository.tasks
            .filter { !it.isSchoolTask && !it.isCompleted }
            .sortedByDescending { it.isStarred }

        val tile = buildTasksTile(
            context = applicationContext,
            deviceConfiguration = requestParams.deviceConfiguration,
            openTasks = openTasks
        )
        return immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = immediateFuture(emptyResources())
}

internal fun buildTasksTile(
    context: Context,
    deviceConfiguration: androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters,
    openTasks: List<TaskItem>
): TileBuilders.Tile {
    val openTasksRoute = launchRouteClickable(context, "tasks", "open_tasks")
    val createTaskRoute = launchRouteClickable(context, "create_task/my_tasks", "create_task")

    val layout = materialScope(
        context = context,
        deviceConfiguration = deviceConfiguration
    ) {
        val header = if (openTasks.isEmpty()) {
            "Aufgaben"
        } else {
            "Aufgaben (${openTasks.size})"
        }

        primaryLayout(
            titleSlot = { tileTitleText(header) },
            mainSlot = {
                if (openTasks.isEmpty()) {
                    textDataCard(
                        onClick = openTasksRoute,
                        width = expand(),
                        height = expand(),
                        title = {
                            text(
                                text = "Alles erledigt".layoutString,
                                typography = Typography.TITLE_MEDIUM,
                                maxLines = 1
                            )
                        },
                        content = {
                            text(
                                text = "Keine offenen Aufgaben".layoutString,
                                typography = Typography.BODY_SMALL,
                                maxLines = 2
                            )
                        },
                        colors = tonalCardColors()
                    )
                } else if (openTasks.size == 1) {
                    taskTileCard(
                        task = openTasks.first(),
                        onClick = openTasksRoute,
                        compact = false
                    )
                } else {
                    val topTwo = openTasks.take(2)
                    LayoutElementBuilders.Column.Builder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .addContent(
                            taskTileCard(
                                task = topTwo[0],
                                onClick = openTasksRoute,
                                compact = true
                            )
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setHeight(dp(4f))
                                .build()
                        )
                        .addContent(
                            taskTileCard(
                                task = topTwo[1],
                                onClick = openTasksRoute,
                                compact = true
                            )
                        )
                        .build()
                }
            },
            bottomSlot = {
                textEdgeButton(
                    onClick = createTaskRoute,
                    labelContent = {
                        text("+ Aufgabe".layoutString)
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

private fun androidx.wear.protolayout.material3.MaterialScope.taskTileCard(
    task: TaskItem,
    onClick: androidx.wear.protolayout.ModifiersBuilders.Clickable,
    compact: Boolean
): LayoutElementBuilders.LayoutElement {
    val subtitle = buildString {
        if (task.isStarred) append("★ ")
        if (task.dueDate.isNotBlank()) {
            append(task.dueDate)
            if (task.dueTime.isNotBlank()) append(" • ${task.dueTime}")
        } else if (task.subject.isNotBlank()) {
            append(task.subject)
        } else {
            append("Offene Aufgabe")
        }
    }

    return textDataCard(
        onClick = onClick,
        width = expand(),
        height = expand(),
        title = {
            text(
                text = task.title.layoutString,
                typography = if (compact) Typography.TITLE_SMALL else Typography.TITLE_MEDIUM,
                maxLines = 1
            )
        },
        content = {
            text(
                text = subtitle.layoutString,
                typography = Typography.BODY_SMALL,
                maxLines = if (compact) 1 else 2
            )
        },
        colors = if (task.isStarred) primaryCardColors() else tonalCardColors()
    )
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
private fun tasksTilePreview(context: Context) = TilePreviewData(
    onTileRequest = { request ->
        buildTasksTile(
            context = context,
            deviceConfiguration = request.deviceConfiguration,
            openTasks = listOf(
                TaskItem(
                    id = "1",
                    title = "Mathe Übungsblatt S. 42",
                    subject = "Mathematik",
                    dueDate = "Morgen",
                    isStarred = true
                ),
                TaskItem(
                    id = "2",
                    title = "Englisch Vokabeln lernen"
                )
            )
        )
    }
)
