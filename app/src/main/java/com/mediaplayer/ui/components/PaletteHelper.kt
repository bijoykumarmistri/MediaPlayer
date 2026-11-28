package com.mediaplayer.ui.components

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DominantColors(
    val dominant: Color = Color(0xFF1E202B),
    val accent: Color = Color(0xFF00E5FF),
    val darkBackground: Color = Color(0xFF0A0B0E),
)

private val dominantColorsCache = androidx.collection.LruCache<Uri, DominantColors>(64)

@Composable
fun rememberDominantColors(uri: Uri?): DominantColors {
    val context = LocalContext.current
    val systemPrimary = MaterialTheme.colorScheme.primary
    val systemSurfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val systemSurface = MaterialTheme.colorScheme.surface

    val defaultFallback = remember(systemPrimary, systemSurfaceContainer, systemSurface) {
        DominantColors(
            dominant = systemSurfaceContainer,
            accent = systemPrimary,
            darkBackground = systemSurface
        )
    }

    var dominantColors by remember(uri, defaultFallback) {
        mutableStateOf(uri?.let { dominantColorsCache[it] } ?: defaultFallback)
    }

    val appContext = remember(context) { context.applicationContext }

    LaunchedEffect(uri, defaultFallback) {
        if (uri == null) {
            dominantColors = defaultFallback
            return@LaunchedEffect
        }

        val cached = dominantColorsCache[uri]
        if (cached != null) {
            dominantColors = cached
            return@LaunchedEffect
        }

        val extracted = withContext(Dispatchers.IO) {
            try {
                val loader = appContext.imageLoader
                val request = ImageRequest.Builder(appContext)
                    .data(uri)
                    .allowHardware(enable = false)
                    .size(256)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        val dominantRgb = palette.dominantSwatch?.rgb
                        val vibrantRgb = palette.vibrantSwatch?.rgb ?: palette.darkVibrantSwatch?.rgb ?: dominantRgb
                        val darkRgb = palette.darkMutedSwatch?.rgb ?: palette.darkVibrantSwatch?.rgb

                        val dominantColor = dominantRgb?.let { Color(it) } ?: systemSurfaceContainer
                        val accentColor = vibrantRgb?.let { Color(it) } ?: systemPrimary
                        val darkBgColor = darkRgb?.let { Color(it) } ?: systemSurface

                        val colors = DominantColors(
                            dominant = dominantColor,
                            accent = accentColor,
                            darkBackground = darkBgColor,
                        )
                        dominantColorsCache.put(uri, colors)
                        colors
                    } else null
                } else null
            } catch (_: Exception) {
                null
            }
        }
        if (extracted != null) {
            dominantColors = extracted
        }
    }

    return dominantColors
}
