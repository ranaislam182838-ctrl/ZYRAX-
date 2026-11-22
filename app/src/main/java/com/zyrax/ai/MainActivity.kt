package com.zyrax.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zyrax.ai.agents.AgentProfile
import com.zyrax.ai.agents.AgentStore
import com.zyrax.ai.voice.VoiceAssistantService

import com.zyrax.ai.ai.GeminiClient

import kotlinx.coroutines.launch

import androidx.compose.ui.text.input.PasswordVisualTransformation

import androidx.compose.ui.text.input.VisualTransformation

private val Bg = Color(0xFF0B0B16)
private val Panel = Color(0xFF19192B)
private val Purple = Color(0xFFB66CFF)
private val Cyan = Color(0xFF55DDF5)

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
    val context = LocalContext.current

    var page by remember { mutableStateOf("Home") }
    var voiceOn by remember {
        mutableStateOf(
            context.getSharedPreferences("zyrax_voice", 0)
                .getBoolean("running", false)
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            val micGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

            if (micGranted) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, VoiceAssistantService::class.java)
                )
                voiceOn = true
            }
        }

    val toggleVoice: () -> Unit = {
        if (voiceOn) {
            context.stopService(
                Intent(context, VoiceAssistantService::class.java)
            )
            context.getSharedPreferences("zyrax_voice", 0)
                .edit().putBoolean("running", false).apply()
            voiceOn = false
        } else {
            val permissions = mutableListOf(
                Manifest.permission.RECORD_AUDIO
            )

            if (Build.VERSION.SDK_INT >= 33) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            val missing = permissions.filter {
                ContextCompat.checkSelfPermission(context, it) !=
                    PackageManager.PERMISSION_GRANTED
            }

            if (missing.isEmpty()) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, VoiceAssistantService::class.java)
                )
                voiceOn = true
            } else {
                permissionLauncher.launch(missing.toTypedArray())
            }
        }
    }

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
                    voiceOn = voiceOn,
                    onToggleVoice = toggleVoice,
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
fun HomeScreen(
    voiceOn: Boolean,
    onToggleVoice: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenModules: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "zyrax_core")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "ZYRAX AI",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Text("PERSONAL AI ECOSYSTEM", color = Purple)

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(174.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .background(Color(0xFF211638), CircleShape)
                .border(2.dp, Purple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .background(Color(0xFF38205A), CircleShape)
                    .border(2.dp, Cyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Z",
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            if (voiceOn) "VOICE SYSTEM ACTIVE" else "ZYRAX CORE STANDBY",
            color = if (voiceOn) Cyan else Color.LightGray,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onToggleVoice,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (voiceOn)
                    Color(0xFF9E3347) else Purple
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (voiceOn) "VOICE OFF  ■" else "VOICE ON  ▶",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(22.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("SYSTEM DASHBOARD", color = Purple)
                Spacer(Modifier.height(8.dp))
                Text("ZYRAX Core", color = Color.White,
                    style = MaterialTheme.typography.titleLarge)
                Text(
                    if (voiceOn) "Voice service requested"
                    else "Voice service is stopped",
                    color = Color.LightGray
                )
                Text("AI connection: Not configured",
                    color = Color(0xFFFFC46B))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("QUICK ACCESS", color = Color.White)

        ActionCard("Open AI Chat", "Start a conversation", onOpenChat)
        ActionCard("Manage AI Agents", "ZYRAX and JARVIS", onOpenAgents)
        ActionCard("Explore Modules", "System components", onOpenModules)

        Spacer(Modifier.height(12.dp))
        Text(
            "Voice recognition is an initial version. Full AI replies and reliable wake-word detection are not connected yet.",
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<String>() }
    val history = remember { mutableStateListOf<Pair<String, String>>() }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(16.dp)
    ) {
        Text("AI CHAT", color = Purple,
            style = MaterialTheme.typography.headlineMedium)
        Text(
            if (GeminiClient.hasApiKey(context))
                "Gemini AI connected configuration found"
            else "Settings থেকে Gemini API Key যোগ করুন",
            color = Color.LightGray
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Text(
                        message,
                        color = Color.White,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            if (loading) {
                item {
                    Text("ZYRAX ভাবছে...", color = Cyan)
                }
            }
        }

        if (error.isNotBlank()) {
            Text(error, color = Color(0xFFFF7777))
            Spacer(Modifier.height(6.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("আপনার মেসেজ লিখুন...") },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = !loading && input.isNotBlank(),
                onClick = {
                    val prompt = input.trim()
                    if (prompt.isNotEmpty() && !loading) {
                        input = ""
                        error = ""
                        messages.add("আপনি: $prompt")
                        loading = true

                        scope.launch {
                            try {
                                val reply = GeminiClient.generateReply(
                                    context,
                                    prompt,
                                    history.toList(),
                                    AgentStore.getActive(context)
                                )
                                history.add("user" to prompt)
                                history.add("model" to reply)
                                messages.add("${AgentStore.getActive(context).name}: $reply")
                            } catch (e: Exception) {
                                error = e.message ?: "Gemini সংযোগে সমস্যা হয়েছে।"
                                messages.add("সমস্যা: $error")
                            } finally {
                                loading = false
                            }
                        }
                    }
                }
            ) {
                Text(if (loading) "..." else "Send")
            }
        }
    }
}

@Composable
fun AgentsScreen() {
    val context = LocalContext.current
    var agents by remember { mutableStateOf(AgentStore.load(context)) }
    var selectedId by remember { mutableStateOf(AgentStore.getActive(context).id) }
    var name by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("bn") }
    var voiceId by remember { mutableStateOf("default") }
    var status by remember { mutableStateOf("") }

    PageColumn {
        Text("AI AGENTS", color = Purple,
            style = MaterialTheme.typography.headlineMedium)
        Text("Choose or create your assistant", color = Color.LightGray)

        agents.forEach { agent ->
            Card(
                onClick = { selectedId = agent.id; AgentStore.setActive(context, agent.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedId == agent.id)
                        Color(0xFF34234B) else Panel
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(agent.name, color = Color.White,
                        style = MaterialTheme.typography.titleLarge)
                    Text(agent.personality, color = Color.LightGray)
                    Text("Language: ${agent.language} | Voice: ${agent.voiceId}",
                        color = Color.LightGray)
                    if (selectedId == agent.id) {
                        Text("SELECTED", color = Purple)
                    }
                }
            }
        }

        Text("CREATE NEW AI AGENT", color = Purple,
            style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Agent Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = personality,
            onValueChange = { personality = it },
            label = { Text("Personality") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = language,
            onValueChange = { language = it },
            label = { Text("Language (bn / en / ar)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = voiceId,
            onValueChange = { voiceId = it },
            label = { Text("Voice ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val agent = AgentProfile(
                    id = "custom_${System.currentTimeMillis()}",
                    name = name.trim(),
                    personality = personality.trim(),
                    language = language.trim(),
                    voiceId = voiceId.trim()
                )
                if (AgentStore.create(context, agent)) {
                    agents = AgentStore.load(context)
                    selectedId = agent.id
                    AgentStore.setActive(context, agent.id)
                    name = ""
                    personality = ""
                    status = "Agent saved successfully!"
                } else {
                    status = "Name and personality are required."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CREATE AND SAVE AGENT")
        }

        Text(status, color = Purple)
    }
}
@Composable
fun ModulesScreen() {
    val modules = listOf(
        "Live Voice" to "Speech recognition and spoken replies",
        "Phone Control" to "Android app and screen interaction",
        "Memory" to "Save and retrieve information",
        "Phone Doctor" to "Device and app diagnostics",
        "Evolution Studio" to "Create and manage extensions",
        "Reminders" to "Scheduled notifications",
        "Plugin System" to "Add assistant capabilities"
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
                        Text("NOT CONNECTED",
                            color = Color(0xFFFFC46B))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var language by remember { mutableStateOf("বাংলা") }
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var hasKey by remember {
        mutableStateOf(GeminiClient.hasApiKey(context))
    }
    var status by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }

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

        Text("GEMINI AI PROVIDER", color = Purple,
            style = MaterialTheme.typography.titleLarge)

        Text(
            if (hasKey) "API Key saved on this device"
            else "API Key not configured",
            color = if (hasKey) Cyan else Color(0xFFFFC46B)
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                status = ""
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Gemini API Key") },
            placeholder = { Text("আপনার API Key লিখুন") },
            singleLine = true,
            visualTransformation = if (showKey)
                VisualTransformation.None
            else PasswordVisualTransformation()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = { showKey = !showKey }) {
                Text(if (showKey) "Hide Key" else "Show Key")
            }
            Button(
                enabled = apiKey.isNotBlank(),
                onClick = {
                    try {
                        GeminiClient.saveApiKey(context, apiKey)
                        apiKey = ""
                        hasKey = true
                        status = "API Key encrypted and saved."
                    } catch (e: Exception) {
                        status = e.message ?: "API Key save করা যায়নি।"
                    }
                }
            ) {
                Text("Save Key")
            }
        }

        Button(
            enabled = hasKey && !testing,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                testing = true
                status = "Gemini connection পরীক্ষা হচ্ছে..."
                scope.launch {
                    try {
                        val reply = GeminiClient.generateReply(
                            context,
                            "Reply only with: Gemini connected."
                        )
                        status = "Connection successful: $reply"
                    } catch (e: Exception) {
                        status = e.message ?: "Gemini connection failed."
                    } finally {
                        testing = false
                    }
                }
            }
        ) {
            Text(if (testing) "Testing..." else "Test Gemini Connection")
        }

        OutlinedButton(
            enabled = hasKey && !testing,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                GeminiClient.clearApiKey(context)
                apiKey = ""
                hasKey = false
                status = "Saved API Key cleared."
            }
        ) {
            Text("Clear Saved API Key")
        }

        if (status.isNotBlank()) {
            Text(status, color = Cyan)
        }

        Text(
            "API Key এই ডিভাইসে Android Keystore দিয়ে encrypt করে রাখা হয়। " +
                "Key কাউকে পাঠাবেন না বা Chat-এ লিখবেন না।",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall
        )
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
