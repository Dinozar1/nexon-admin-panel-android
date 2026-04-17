package com.example.nexonpayadminpanel.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Ciemny motyw (jeśli telefon użytkownika jest w trybie ciemnym)
private val DarkColorScheme = darkColorScheme(
    primary = AdminPurpleDark,
    secondary = AdminPurpleBase,
    tertiary = AdminPurpleLight,
    background = AdminPurpleText,
    surface = AdminPurpleText,
    onPrimary = AdminPurpleLight
)

// Jasny motyw (domyślny - fioletowy z Reacta)
private val LightColorScheme = lightColorScheme(
    primary = AdminPurpleDark,      // np. główne przyciski
    secondary = AdminPurpleText,    // np. tekst
    tertiary = AdminPurpleBase,     // akcenty
    background = AdminPurpleLight,  // jasno-fioletowe tło ekranu
    surface = AdminPurpleLight,     // tło kart
    onPrimary = AdminPurpleLight    // tekst na głównych przyciskach
)

@Composable
fun NexonPayAdminPanelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Opcja 'dynamicColor' używa kolorów tapety użytkownika (Android 12+).
    // Wyłączamy to (false), bo chcemy nasz sztywny, fioletowy motyw!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}