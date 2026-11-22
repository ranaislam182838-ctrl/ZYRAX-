package com.zyrax.ai.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zyrax.ai.data.Persona

val Bg = Color(0xFF0A0A12)
val Surface1 = Color(0xFF14141F)
val Surface2 = Color(0xFF1E1E2E)
val TextMuted = Color(0xFFA6A6BD)

private val Scheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    secondary = Color(0xFF22D3EE),
    background = Bg,
    onBackground = Color(0xFFEDEDF7),
    surface = Surface1,
    onSurface = Color(0xFFEDEDF7),
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF3A3A52)
)

@Composable
fun ZyraxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

/** Two gradient colours for each assistant. */
fun Persona.colors(): Pair<Color, Color> = when (this) {
    Persona.ZYRAX -> Pair(Color(0xFFD946EF), Color(0xFF7C3AED))
    Persona.JARVIS -> Pair(Color(0xFF0EA5E9), Color(0xFF2563EB))
}

@Composable
fun ScreenHeader(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel)
        }
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}
