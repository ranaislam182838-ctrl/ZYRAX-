package com.zyrax.ai.agents

data class AgentProfile(
    val id: String,
    val name: String,
    val personality: String,
    val language: String = "bn",
    val voiceId: String = "default",
    val enabled: Boolean = true
)

object AgentRegistry {

    val defaultAgents = listOf(
        AgentProfile(
            id = "zyrax",
            name = "ZYRAX",
            personality = "Friendly, caring and companion-like"
        ),
        AgentProfile(
            id = "jarvis",
            name = "JARVIS",
            personality = "Professional, technical and precise",
            voiceId = "professional"
        )
    )
}
