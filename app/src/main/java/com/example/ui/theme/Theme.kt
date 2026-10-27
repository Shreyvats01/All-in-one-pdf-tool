package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val FolioDarkColorScheme = darkColorScheme(
    primary = FolioIndigoDark,
    onPrimary = FolioOnIndigoDark,
    primaryContainer = FolioIndigoContainerDark,
    onPrimaryContainer = FolioOnIndigoContainerDark,
    secondary = FolioIndigoDark,
    onSecondary = FolioOnIndigoDark,
    secondaryContainer = FolioDarkSurfaceHigh,
    onSecondaryContainer = FolioDarkOnSurface,
    tertiary = FolioIndigoDark,
    onTertiary = FolioOnIndigoDark,
    background = FolioDarkBackground,
    onBackground = FolioDarkOnBackground,
    surface = FolioDarkSurface,
    onSurface = FolioDarkOnSurface,
    surfaceVariant = FolioDarkSurfaceVariant,
    onSurfaceVariant = FolioDarkOnSurfaceVariant,
    surfaceContainerLowest = FolioDarkBackground,
    surfaceContainerLow = FolioDarkSurface,
    surfaceContainer = FolioDarkSurfaceVariant,
    surfaceContainerHigh = FolioDarkSurfaceHigh,
    surfaceContainerHighest = FolioDarkOutline,
    outline = FolioDarkOutline,
    outlineVariant = FolioDarkOutlineVariant,
    error = FolioErrorDark,
    onError = FolioOnErrorDark,
    errorContainer = FolioErrorContainerDark,
    onErrorContainer = FolioOnErrorContainerDark
)

private val FolioLightColorScheme = lightColorScheme(
    primary = FolioIndigoLight,
    onPrimary = FolioOnIndigoLight,
    primaryContainer = FolioIndigoContainerLight,
    onPrimaryContainer = FolioOnIndigoContainerLight,
    secondary = FolioIndigoLight,
    onSecondary = FolioOnIndigoLight,
    secondaryContainer = FolioLightSurfaceHigh,
    onSecondaryContainer = FolioLightOnSurface,
    tertiary = FolioIndigoLight,
    onTertiary = FolioOnIndigoLight,
    background = FolioLightBackground,
    onBackground = FolioLightOnBackground,
    surface = FolioLightSurface,
    onSurface = FolioLightOnSurface,
    surfaceVariant = FolioLightSurfaceVariant,
    onSurfaceVariant = FolioLightOnSurfaceVariant,
    surfaceContainerLowest = FolioLightBackground,
    surfaceContainerLow = FolioLightSurface,
    surfaceContainer = FolioLightSurfaceVariant,
    surfaceContainerHigh = FolioLightSurfaceHigh,
    surfaceContainerHighest = FolioLightOutline,
    outline = FolioLightOutline,
    outlineVariant = FolioLightOutlineVariant,
    error = FolioErrorLight,
    onError = FolioOnErrorLight,
    errorContainer = FolioErrorContainerLight,
    onErrorContainer = FolioOnErrorContainerLight
)

val FolioShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FolioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FolioDarkColorScheme
        else -> FolioLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FolioShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FolioTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
