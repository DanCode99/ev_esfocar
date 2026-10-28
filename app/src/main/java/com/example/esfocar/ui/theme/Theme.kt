package com.example.esfocar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Esquema de Colores Oficial Esfocar (Fondo Blanco Puro y Verdes Institucionales)
private val InstitutionalColorScheme = lightColorScheme(
    primary = GreenPrimary,               // Verde Carabineros #00563B
    onPrimary = Color.White,              // Texto blanco sobre verde
    primaryContainer = Color(0xFFE2F0E8), // Verde claro institucional para Cards
    onPrimaryContainer = Color(0xFF003824),// Texto oscuro sobre verde claro

    secondary = GoldAccent,               // Dorado Institucional #C5A059
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDF8EE),
    onSecondaryContainer = Color(0xFF423200),

    tertiary = GreenSecondary,            // Verde Oscuro #1E3A2B
    onTertiary = Color.White,

    background = Color.White,             // FONDO SIEMPRE BLANCO PURO #FFFFFF
    onBackground = Color(0xFF111C16),     // Texto oscuro principal

    surface = Color.White,                // Superficie blanca
    onSurface = Color(0xFF111C16),

    surfaceVariant = Color(0xFFF2F7F4),   // Verde muy suave para inputs
    onSurfaceVariant = Color(0xFF2E4035),

    outline = Color(0xFF00563B),          // Bordes verdes institucionales
    outlineVariant = Color(0xFFD3E2D9),

    error = RedError,
    onError = Color.White,
    errorContainer = RedErrorContainer,
    onErrorContainer = OnRedErrorContainer
)

@Composable
fun EsfocarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Forzar el tema institucional claro con fondo blanco y verde oficial
    val colorScheme = InstitutionalColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GreenPrimaryDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
