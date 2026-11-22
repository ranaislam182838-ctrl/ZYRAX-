package com.zyrax.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zyrax.ai.agents.AgentRegistry
import com.zyrax.ai.core.ZyraxCore

private val Background = Color(0xFF0B0B16)
private val Panel = Color(0xFF19192B)
private val Accent = Color(0xFFB66CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val core = ZyraxCore()
        core.initialize()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Background,
                    surface = Panel
                )
            ) {
                ZyraxDashboard(core)
            }
        }
    }
}

@Composable
fun ZyraxDashboard(core: ZyraxCore) {
    var selectedAgent by remember { mutableStateOf("zyrax") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "ZYRAX AI",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Text(
                    "PERSONAL AI ECOSYSTEM",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CORE ENGINE", color = Accent)
                        Spacer(Modifier.height(8.dp))
                        Text("System initialized", color = Color.White)
                        Text(
                            "${core.modules.size} planned modules",
                            color = Color.LightGray
                        )
                    }
                }
            }

            item {
                Text("YOUR AI ASSISTANTS", color = Color.White,
                    style = MaterialTheme.typography.titleMedium)
            }

            items(AgentRegistry.defaultAgents) { agent ->
                Card(
                    onClick = { selectedAgent = agent.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAgent == agent.id)
                            Color(0xFF34234B) else Panel
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(agent.name, color = Color.White,
                            style = MaterialTheme.typography.titleLarge)
                        Text(agent.personality, color = Color.LightGray)
                        if (selectedAgent == agent.id) {
                            Text("SELECTED", color = Accent)
                        }
                    }
                }
            }

            item {
                Text("SYSTEM MODULES", color = Color.White,
                    style = MaterialTheme.typography.titleMedium)
            }

            items(core.modules) { module ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(module.name, color = Color.White)
                            Text(module.description, color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            if (module.enabled) "ACTIVE" else "PLANNED",
                            color = if (module.enabled) Color.Green else Accent,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            item {
                Text(
                    "New architecture foundation. Phone control and live AI are not active yet.",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
