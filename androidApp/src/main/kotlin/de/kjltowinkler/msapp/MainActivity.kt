package de.kjltowinkler.msapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        de.kjltowinkler.msapp.util.AppContextProvider.init(this)
        de.kjltowinkler.msapp.worker.SubstitutionWorkScheduler.schedulePeriodicSync(this)

        handleNavigationIntent(intent)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleNavigationIntent(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    de.kjltowinkler.msapp.data.update.AppUpdateManager.checkAndNotifyUpdate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            de.kjltowinkler.msapp.data.update.AppUpdateManager.checkForUpdates()
            de.kjltowinkler.msapp.data.timetable.TimetableSyncManager.checkForUpdates(forceRefresh = true, notifyIfChanged = true)
        }
    }

    private fun handleNavigationIntent(intent: android.content.Intent?) {
        val nav = intent?.getStringExtra("navigate_to")
        val highlightId = intent?.getStringExtra("highlight_id")
        if (nav != null) {
            de.kjltowinkler.msapp.data.storage.PlatformStorage.setString("pending_navigate_to", nav)
            if (!highlightId.isNullOrBlank()) {
                de.kjltowinkler.msapp.data.storage.PlatformStorage.setString("pending_highlight_id", highlightId)
            }
            de.kjltowinkler.msapp.util.NavigationEvents.emit(nav, highlightId)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun AppAndroidPreview() {
    App()
}