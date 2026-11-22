package com.zyrax.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zyrax.ai.data.ChatMessage
import com.zyrax.ai.data.Persona

@Composable
fun ChatScreen(vm: ZyraxViewModel) {
    val s = Strings.of(vm.settings.language)
    val persona = vm.persona
    val (c1, c2) = persona.colors()
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startListening() else vm.showError(s.micDenied)
    }

    val greeting = if (persona == Persona.ZYRAX) s.greetingZyrax else s.greetingJarvis
    val total = (if (vm.messages.isEmpty()) 1 else vm.messages.size) + (if (vm.busy) 1 else 0)
    LaunchedEffect(total) { listState.animateScrollToItem(total - 1) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(persona.displayName, s.back, vm::goBack) {
            IconButton(onClick = vm::clearChat) {
                Icon(Icons.Filled.Delete, contentDescription = s.clearChat)
            }
        }

        if (vm.settings.demo) {
            Text(
                s.demoBanner,
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface2)
                    .padding(10.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (vm.messages.isEmpty()) {
                item { Bubble(ChatMessage(0, "assistant", greeting), c1, c2) }
            }
            items(vm.messages, key = { it.id }) { Bubble(it, c1, c2) }
            if (vm.busy) {
                item { Text(s.thinking, color = TextMuted, modifier = Modifier.padding(start = 6.dp)) }
            }
        }

        vm.errorText?.let {
            Text(
                it,
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        if (vm.listening) {
            Text(
                "${s.listening} ${vm.partial}",
                color = c1,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(s.typeMessage) },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            RoundButton(c1, c2, onClick = {
                if (vm.listening) {
                    vm.stopListening()
                } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    vm.startListening()
                } else {
                    permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }) {
                Icon(
                    if (vm.listening) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
            RoundButton(c1, c2, onClick = {
                vm.send(input)
                input = ""
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun RoundButton(c1: Color, c2: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(c1, c2)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun Bubble(m: ChatMessage, c1: Color, c2: Color) {
    val isUser = m.role == "user"
    val bg: Brush = if (isUser) Brush.linearGradient(listOf(c1, c2)) else SolidColor(Surface2)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(m.text, color = Color.White, fontSize = 16.sp)
        }
    }
}
