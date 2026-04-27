package com.misfinanzas.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.misfinanzas.app.data.preferences.AppThemeMode
import com.misfinanzas.app.data.preferences.ThemePreferences
import com.misfinanzas.app.ui.navigation.AppNavigation
import com.misfinanzas.app.ui.screens.splash.SplashScreen
import com.misfinanzas.app.ui.theme.MisFinanzasTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    private val permisoNotificaciones = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val oledDark = themeMode == AppThemeMode.OLED
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK, AppThemeMode.OLED -> true
            }

            MisFinanzasTheme(
                darkTheme = darkTheme,
                dynamicColor = themeMode == AppThemeMode.SYSTEM,
                oledDark = oledDark
            ) {
                val view = LocalView.current
                val background = MaterialTheme.colorScheme.background.toArgb()
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as ComponentActivity).window
                        window.statusBarColor = background
                        window.navigationBarColor = background
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var splashVisible by remember { mutableStateOf(true) }
                    Crossfade(
                        targetState = splashVisible,
                        animationSpec = tween(durationMillis = 450),
                        label = "splash_to_app",
                    ) { showingSplash ->
                        if (showingSplash) {
                            SplashScreen(onFinished = { splashVisible = false })
                        } else {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }
}
