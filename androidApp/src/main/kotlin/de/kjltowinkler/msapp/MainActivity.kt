package de.kjltowinkler.msapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

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

    private fun handleNavigationIntent(intent: android.content.Intent?) {
        val nav = intent?.getStringExtra("navigate_to")
        if (nav != null) {
            de.kjltowinkler.msapp.data.storage.PlatformStorage.setString("pending_navigate_to", nav)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun AppAndroidPreview() {
    App()
}