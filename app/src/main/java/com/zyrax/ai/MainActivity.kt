package com.zyrax.ai

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zyrax.ai.ui.ChatScreen
import com.zyrax.ai.ui.HomeScreen
import com.zyrax.ai.ui.MemoryScreen
import com.zyrax.ai.ui.Screen
import com.zyrax.ai.ui.SettingsScreen
import com.zyrax.ai.ui.ZyraxTheme
import com.zyrax.ai.ui.ZyraxViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            ZyraxTheme { ZyraxApp() }
        }
    }
}

@Composable
fun ZyraxApp(vm: ZyraxViewModel = viewModel()) {
    val direction = if (vm.settings.language.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                BackHandler(enabled = vm.screen != Screen.HOME) { vm.goBack() }
                when (vm.screen) {
                    Screen.HOME -> HomeScreen(vm)
                    Screen.CHAT -> ChatScreen(vm)
                    Screen.MEMORY -> MemoryScreen(vm)
                    Screen.SETTINGS -> SettingsScreen(vm)
                }
            }
        }
    }
}
