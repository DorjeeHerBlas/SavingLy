package com.misfinanzas.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Primary, onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer, onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary, onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer, onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary, tertiaryContainer = TertiaryContainer,
    background = Background, onBackground = OnBackground,
    surface = Surface, onSurface = OnSurface,
    surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant,
    error = Error, onError = OnError
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark, onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark, onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark, onSecondary = OnSecondaryDark,
    background = BackgroundDark, onBackground = OnBackgroundDark,
    surface = SurfaceDark, onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark
)

private val OledColors = darkColorScheme(
    primary = OledPrimary, onPrimary = OledOnPrimary,
    primaryContainer = OledPrimaryContainer, onPrimaryContainer = OledOnPrimaryContainer,
    secondary = OledSecondary, onSecondary = OledOnSecondary,
    secondaryContainer = OledSecondaryContainer, onSecondaryContainer = OledOnSecondaryContainer,
    tertiary = OledTertiary, tertiaryContainer = OledTertiaryContainer,
    background = OledBackground, onBackground = OledOnSurface,
    surface = OledSurface, onSurface = OledOnSurface,
    surfaceVariant = OledSurfaceVariant, onSurfaceVariant = OledOnSurfaceVariant,
    surfaceContainerLowest = OledBackground,
    surfaceContainerLow = OledSurface,
    surfaceContainer = OledSurface,
    surfaceContainerHigh = OledSurfaceBright,
    surfaceContainerHighest = OledSurfaceBright,
    outline = OledOutline,
    error = OledError,
    errorContainer = OledErrorContainer
)

@Composable
fun MisFinanzasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    oledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        oledDark -> OledColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
