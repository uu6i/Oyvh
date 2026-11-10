package com.vamps.music

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Bg = Color(0xFF090A0D)
internal val Glass = Color(0xFF191C23)
internal val Elevated = Color(0xFF222630)
internal val Txt = Color(0xFFF5F5F7)
internal val Sub = Color(0xFFA1A5AE)
internal val Dim = Color(0xFF737985)
internal val Accent = Color(0xFFC6CAD2)
internal val Line = Color(0x14FFFFFF)
internal val Good = Color(0xFF80B99A)
internal val Bad = Color(0xFFD77D82)

@Composable
internal fun VampsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Bg,
            background = Bg,
            onBackground = Txt,
            surface = Glass,
            onSurface = Txt,
            secondary = Sub,
            outline = Line,
            surfaceVariant = Elevated,
            onSurfaceVariant = Sub,
            error = Bad,
        ),
        content = content,
    )
}
