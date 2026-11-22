package com.zyrax.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zyrax.ai.data.AppLanguage
import com.zyrax.ai.data.AppSettings
import com.zyrax.ai.data.Persona
import com.zyrax.ai.data.Provider

// ---------------- HOME ----------------

@Composable
fun HomeScreen(vm: ZyraxViewModel) {
    val s = Strings.of(vm.settings.language)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("ZYRAX AI", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text(s.tagline, color = TextMuted, fontSize = 16.sp)

        if (vm.settings.demo) {
            Text(
                s.demoBanner,
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .clickable { vm.openSettings() }
                    .padding(14.dp)
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(s.choose, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

        PersonaCard(Persona.ZYRAX, s.zyraxDesc) { vm.openChat(Persona.ZYRAX) }
        PersonaCard(Persona.JARVIS, s.jarvisDesc) { vm.openChat(Persona.JARVIS) }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = vm::openMemory, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Memory, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(s.memory)
            }
            OutlinedButton(onClick = vm::openSettings, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(s.settings)
            }
        }
    }
}

@Composable
private fun PersonaCard(p: Persona, desc: String, onClick: () -> Unit) {
    val (c1, c2) = p.colors()
    val gradient = Brush.linearGradient(listOf(c1, c2))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Surface1)
            .border(1.dp, gradient, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Text(p.displayName.take(1), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(p.displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = TextMuted, fontSize = 14.sp)
        }
    }
}

// ---------------- MEMORY ----------------

@Composable
fun MemoryScreen(vm: ZyraxViewModel) {
    val s = Strings.of(vm.settings.language)
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(s.memory, s.back, vm::goBack)
        Text(s.memoryHint, color = TextMuted, modifier = Modifier.padding(horizontal = 20.dp))
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(s.memoryPlaceholder) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                vm.addMemory(text)
                text = ""
            }) { Text(s.addMemory) }
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (vm.memory.isEmpty()) {
                item { Text(s.emptyMemory, color = TextMuted) }
            }
            items(vm.memory, key = { it.id }) { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface1)
                        .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(m.text, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteMemory(m.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
            }
        }
    }
}

// ---------------- SETTINGS ----------------

@Composable
fun SettingsScreen(vm: ZyraxViewModel) {
    val cur = vm.settings
    val s = Strings.of(cur.language)
    var provider by remember { mutableStateOf(cur.provider) }
    var key by remember { mutableStateOf(cur.apiKey) }
    var model by remember { mutableStateOf(cur.model) }
    var lang by remember { mutableStateOf(cur.language) }
    var speak by remember { mutableStateOf(cur.speakReplies) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(s.settings, s.back, vm::goBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(s.provider, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Provider.values().forEach { p ->
                    FilterChip(
                        selected = provider == p,
                        onClick = { provider = p; saved = false },
                        label = { Text(p.label) }
                    )
                }
            }

            OutlinedTextField(
                value = key,
                onValueChange = { key = it; saved = false },
                label = { Text(s.apiKey) },
                placeholder = { Text(s.apiKeyHint) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) s.hide else s.show) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(s.apiKeyNote, fontSize = 12.sp, color = TextMuted)

            OutlinedTextField(
                value = model,
                onValueChange = { model = it; saved = false },
                label = { Text(s.model) },
                placeholder = { Text(provider.defaultModel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(s.language, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.values().forEach { l ->
                    FilterChip(
                        selected = lang == l,
                        onClick = { lang = l; saved = false },
                        label = { Text(l.label) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.speakReplies, modifier = Modifier.weight(1f))
                Switch(checked = speak, onCheckedChange = { speak = it; saved = false })
            }

            Button(
                onClick = {
                    vm.saveSettings(AppSettings(provider, key.trim(), model.trim(), lang, speak))
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saved) s.saved else s.save) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
