package com.zyrax.ai.data

enum class Persona(val displayName: String) {
    ZYRAX("ZYRAX"),
    JARVIS("JARVIS")
}

enum class AppLanguage(val tag: String, val label: String, val rtl: Boolean) {
    EN("en-US", "English", false),
    BN("bn-BD", "বাংলা", false),
    AR("ar-SA", "العربية", true)
}

enum class Provider(val label: String, val defaultModel: String) {
    CLAUDE("Claude (Anthropic)", "claude-sonnet-5-5"),
    OPENAI("OpenAI", "gpt-4o-mini")
}

data class ChatMessage(val id: Long, val role: String, val text: String)

data class MemoryItem(val id: Long, val text: String)

data class AppSettings(
    val provider: Provider = Provider.CLAUDE,
    val apiKey: String = "",
    val model: String = "",
    val language: AppLanguage = AppLanguage.EN,
    val speakReplies: Boolean = true
) {
    val effectiveModel: String get() = model.ifBlank { provider.defaultModel }
    val demo: Boolean get() = apiKey.isBlank()
}

fun Persona.systemPrompt(lang: AppLanguage, memory: List<MemoryItem>): String {
    val who = when (this) {
        Persona.ZYRAX ->
            "You are ZYRAX, a female AI assistant: warm, confident, sharp and creative. " +
                "Where the language has gendered forms, speak as a woman."
        Persona.JARVIS ->
            "You are JARVIS, a male AI assistant: calm, precise, courteous and subtly witty, " +
                "like a refined digital butler. Where the language has gendered forms, speak as a man."
    }
    val langRule = when (lang) {
        AppLanguage.EN -> "Reply in English."
        AppLanguage.BN -> "Reply in Bengali (বাংলা)."
        AppLanguage.AR -> "Reply in Arabic (العربية)."
    }
    val base = "$who You live inside a personal Android app called ZYRAX AI. " +
        "Keep replies short and natural to read aloud; do not use markdown, tables or long lists. " +
        "$langRule If the user clearly writes in another language, answer in that language."
    if (memory.isEmpty()) return base
    val facts = memory.joinToString("\n") { "- ${it.text}" }
    return "$base\n\nThings you must remember about the user:\n$facts"
}
