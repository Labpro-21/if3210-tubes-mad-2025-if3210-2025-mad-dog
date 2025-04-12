package com.example.purrytify.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SpotifyColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyWhite,
    primaryContainer = SpotifyGreen.copy(alpha = 0.8f),
    onPrimaryContainer = SpotifyWhite,
    
    secondary = SpotifyLightGray,
    onSecondary = SpotifyWhite,
    secondaryContainer = SpotifyLightGray.copy(alpha = 0.6f),
    onSecondaryContainer = SpotifyWhite,
    
    tertiary = SpotifyWhite,
    onTertiary = SpotifyBlack,
    
    background = SpotifyBlack,
    onBackground = SpotifyWhite,
    
    surface = SpotifyDarkGray,
    onSurface = SpotifyWhite,
    
    surfaceVariant = SpotifyDarkGray.copy(alpha = 0.7f),
    onSurfaceVariant = SpotifyWhite.copy(alpha = 0.8f),
    
    error = Color(0xFFCF6679),
    onError = SpotifyWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

)

@Composable
fun PurrytifyTheme(
    darkTheme: Boolean = true, // Default to dark theme for Spotify-like experience
    dynamicColor: Boolean = false, // Turn off dynamic color to ensure Spotify branding
    content: @Composable () -> Unit
) {
    // Always use the Spotify color scheme
    val colorScheme = SpotifyColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SpotifyBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}