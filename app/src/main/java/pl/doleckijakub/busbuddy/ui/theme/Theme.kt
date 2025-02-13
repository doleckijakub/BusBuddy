package pl.doleckijakub.busbuddy.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BusBuddyTheme(content: @Composable () -> Unit) {
    val darkColorScheme = lightColorScheme(
        primary = Color(0xFF8B0000),
        primaryContainer = Color(0xFF5A0000),
        onPrimary = Color(0xFFFFFFFF),

        secondary = Color(0xFF6A0DAD),
        secondaryContainer = Color(0xFF450A74),
        onSecondary = Color(0xFF000000),

        background = Color(0xFF1A001A),
        onBackground = Color(0xFF000000),

        surface = Color(0xFF2E002E),
        onSurface = Color(0xFF000000),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF),
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography,
        content = content
    )
}
