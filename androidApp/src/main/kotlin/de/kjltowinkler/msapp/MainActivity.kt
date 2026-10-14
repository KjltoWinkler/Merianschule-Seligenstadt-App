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

        setContent {
            App()
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun AppAndroidPreview() {
    App()
}