package com.bkm.player.mpv

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Full-screen immersive host activity for [VideoPlayerScreen].
 * Also accepts an incoming `ACTION_VIEW` video URI when launched from a file manager.
 */
class PlayerActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enterImmersiveFullscreen()

        intent?.data?.let { incomingUri ->
            playerViewModel.openDocument(this, incomingUri)
        }

        setContent {
            val context = LocalContext.current
            val pitchBlack = context.getSharedPreferences("app_settings_customization", MODE_PRIVATE)
                .getBoolean("pref_pitch_black_theme", false)
            val darkTheme = isSystemInDarkTheme() || pitchBlack
            val baseScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) darkColorScheme() else lightColorScheme()
            }
            val colorScheme = if (pitchBlack) {
                baseScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceDim = Color.Black,
                    surfaceBright = Color(0xFF141418),
                    surfaceVariant = Color(0xFF121218),
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color(0xFF050507),
                    surfaceContainer = Color(0xFF0A0A0D),
                    surfaceContainerHigh = Color(0xFF121216),
                    surfaceContainerHighest = Color(0xFF1A1A20),
                    outlineVariant = Color(0xFF22222A)
                )
            } else {
                baseScheme
            }
            MaterialTheme(colorScheme = colorScheme) {
                VideoPlayerScreen(viewModel = playerViewModel)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterImmersiveFullscreen()
        }
    }

    private fun enterImmersiveFullscreen() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}
