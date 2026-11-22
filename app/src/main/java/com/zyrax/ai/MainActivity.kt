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

private val Bg = Color(0xFF0B0B16)
private val Panel = Color(0xFF19192B)
private val Purple = Color(0xFFB66CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Purple,
                    background = Bg,
                    surface = Panel
                )
            ) {
                ZyraxApp()
            }
        }
    }
}

@Composable
fun ZyraxApp() {
    var page by remember { mutableStateOf("Home") }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                val pages = listOf(
                    "Home", "Chat", "Agents", "Modules", "Settings"
                )
                val symbols = listOf("⌂", "✉", "◉", "▦", "⚙")

                pages.forEachIndexed { index, name ->
                    NavigationBarItem(
                        selected = page == name,
                        onClick = { page = name },
                        icon = { Text(symbols[index]) },
                        label = { Text(name) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (page) {
                "Home" -> HomeScreen(
                    onOpenChat = { page = "Chat" },
                    onOpenAgents = { page = "Agents" },
                    onOpenModules = { page = "Modules" }
                )
                "Chat" -> ChatScreen()
                "Agents" -> AgentsScreen()
                "Modules" -> ModulesScreen()
                "Settings" -> SettingsScreen()
            }
        }
    }
}

@Composable
fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
fun HomeScreen(
    onOpenChat: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenModules: () -> Unit
) {
    PageColumn {
        Text("ZYRAX AI", style = MaterialTheme.typography.headlineLarge,
            color = Color.White)
        Text("PERSONAL AI ECOSYSTEM", color = Purple)

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("SYSTEM DASHBOARD", color = Purple)
                Spacer(Modifier.height(10.dp))
                Text("ZYRAX Core", color = Color.White,
                    style = MaterialTheme.typography.titleLarge)
                Text("Dashboard is running", color = Color.LightGray)
                Text("AI connection: Not configured",
                    color = Color(0xFFFFC46B))
            }
        }

        Text("QUICK ACCESS", color = Color.White)
        ActionCard("Open AI Chat", "Start a conversation", onOpenChat)
        ActionCard("Manage AI Agents", "ZYRAX and JARVIS", onOpenAgents)
        ActionCard("Explore Modules", "View system components", onOpenModules)

        Text(
            "Voice, AI connection and phone control are not active yet.",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ActionCard(title: String, description: String, action: () -> Unit) {
    Card(
        onClick = action,
        colors = CardDefaults.cardColors(containerColor = Panel),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Color.White,
                style = MaterialTheme.typography.titleMedium)
            Text(description, color = Color.LightGray)
        }
    }
}

@Composable
fun ChatScreen() {
    val messages = remember { mutableStateListOf<String>() }
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(16.dp)
    ) {
        Text("AI CHAT", color = Purple,
            style = MaterialTheme.typography.headlineMedium)
        Text("AI provider connection will be added next.",
            color = Color.LightGray)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Panel
                    )
                ) {
                    Text(message, color = Color.White,
                        modifier = Modifier.padding(14.dp))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write a message...") },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (input.isNotBlank()) {
                    messages.add("You: $input")
                    input = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}

@Composable
fun AgentsScreen() {
    var selected by remember { mutableStateOf("ZYRAX") }

    PageColumn {
        Text("AI AGENTS", color = Purple,
            style = MaterialTheme.typography.headlineMedium)
        Text("Choose your assistant", color = Color.LightGray)

        listOf(
            "ZYRAX" to "Friendly, caring companion",
            "JARVIS" to "Professional technical assistant"
        ).forEach { (name, description) ->
            Card(
                onClick = { selected = name },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == name)
                        Color(0xFF34234B) else Panel
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(name, color = Color.White,
                        style = MaterialTheme.typography.titleLarge)
                    Text(description, color = Color.LightGray)
                    if (selected == name) {
                        Text("SELECTED", color = Purple)
                    }
                }
            }
        }

        Text("Custom Agent creation will be added in a later stage.",
            color = Color.LightGray)
    }
}

@Composable
fun ModulesScreen() {
    val modules = listOf(
        "Live Voice" to "Speech recognition and spoken replies",
        "Phone Control" to "Android app and screen interaction",
        "Memory" to "Save and retrieve personal information",
        "Phone Doctor" to "Device and app diagnostics",
        "Evolution Studio" to "Create and manage extensions",
        "Reminders" to "Scheduled notifications",
        "Plugin System" to "Add new assistant capabilities"
    )

    PageColumn {
        Text("SYSTEM MODULES", color = Purple,
            style = MaterialTheme.typography.headlineMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(modules) { module ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(module.first, color = Color.White,
                            style = MaterialTheme.typography.titleMedium)
                        Text(module.second, color = Color.LightGray)
                        Text("NOT IMPLEMENTED",
                            color = Color(0xFFFFC46B))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    var language by remember { mutableStateOf("বাংলা") }

    PageColumn {
        Text("SETTINGS", color = Purple,
            style = MaterialTheme.typography.headlineMedium)

        Text("App language", color = Color.White)
        listOf("বাংলা", "English", "العربية").forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = language == option,
                    onClick = { language = option }
                )
                Text(option, color = Color.White)
            }
        }

        HorizontalDivider(color = Color.DarkGray)

        Text("AI Provider", color = Color.White)
        Text("Not connected", color = Color(0xFFFFC46B))

        Text("Voice settings and API configuration will be added next.",
            color = Color.LightGray)
    }
}
