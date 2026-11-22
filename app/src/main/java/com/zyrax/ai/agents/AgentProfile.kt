package com.zyrax.ai.agents

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AgentProfile(
    val id: String,
    val name: String,
    val personality: String,
    val language: String = "bn",
    val voiceId: String = "default",
    val enabled: Boolean = true,
    val systemPrompt: String = "",
    val themeColor: String = "#6750A4"
)

object AgentRegistry {
    val defaultAgents = listOf(
        AgentProfile(
            id = "zyrax",
            name = "ZYRAX",
            personality = "Friendly, caring and companion-like",
            voiceId = "default",
            themeColor = "#6750A4"
        ),
        AgentProfile(
            id = "jarvis",
            name = "JARVIS",
            personality = "Professional, technical and precise",
            voiceId = "professional",
            themeColor = "#2196F3"
        )
    )
}

object AgentStore {
    private const val PREFS = "zyrax_agents"
    private const val KEY_AGENTS = "agents_json"
    private const val KEY_ACTIVE = "active_agent_id"

    fun load(context: Context): List<AgentProfile> {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val raw = prefs.getString(KEY_AGENTS, null)
            ?: return AgentRegistry.defaultAgents

        return try {
            val array = JSONArray(raw)
            val agents = mutableListOf<AgentProfile>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                agents.add(
                    AgentProfile(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        personality = item.optString("personality"),
                        language = item.optString("language", "bn"),
                        voiceId = item.optString("voiceId", "default"),
                        enabled = item.optBoolean("enabled", true),
                        systemPrompt = item.optString("systemPrompt", ""),
                        themeColor = item.optString("themeColor", "#6750A4")
                    )
                )
            }

            val savedIds = agents.map { it.id }.toSet()
            agents.addAll(
                AgentRegistry.defaultAgents.filter { it.id !in savedIds }
            )
            agents
        } catch (_: Exception) {
            AgentRegistry.defaultAgents
        }
    }

    fun save(context: Context, agents: List<AgentProfile>) {
        val array = JSONArray()

        agents.forEach { agent ->
            val item = JSONObject().apply {
                put("id", agent.id)
                put("name", agent.name)
                put("personality", agent.personality)
                put("language", agent.language)
                put("voiceId", agent.voiceId)
                put("enabled", agent.enabled)
                put("systemPrompt", agent.systemPrompt)
                put("themeColor", agent.themeColor)
            }
            array.put(item)
        }

        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AGENTS, array.toString())
            .apply()
    }

    fun getActive(context: Context): AgentProfile {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val savedId = prefs.getString(KEY_ACTIVE, null)
        val oldId = appContext
            .getSharedPreferences("zyrax_voice", Context.MODE_PRIVATE)
            .getString("active_agent", "ZYRAX")
            ?.lowercase()

        val activeId = savedId ?: when (oldId) {
            "jarvis" -> "jarvis"
            else -> "zyrax"
        }

        val agents = load(appContext)
        val active = agents.firstOrNull {
            it.id == activeId && it.enabled
        } ?: agents.firstOrNull { it.id == "zyrax" && it.enabled }
            ?: agents.firstOrNull { it.enabled }
            ?: AgentRegistry.defaultAgents.first()

        if (savedId != active.id) {
            prefs.edit().putString(KEY_ACTIVE, active.id).apply()
        }

        return active
    }

    fun setActive(context: Context, agentId: String): Boolean {
        val agent = load(context).firstOrNull {
            it.id == agentId && it.enabled
        } ?: return false

        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE, agent.id)
            .apply()

        return true
    }

    fun create(context: Context, agent: AgentProfile): Boolean {
        if (agent.id.isBlank() || agent.name.isBlank() ||
            agent.personality.isBlank()
        ) return false

        val agents = load(context).toMutableList()
        if (agents.any { it.id == agent.id }) return false

        agents.add(agent)
        save(context, agents)
        return true
    }

    fun update(context: Context, agent: AgentProfile): Boolean {
        val agents = load(context).toMutableList()
        val index = agents.indexOfFirst { it.id == agent.id }
        if (index < 0) return false

        agents[index] = agent
        save(context, agents)

        if (!agent.enabled && getActive(context).id == agent.id) {
            setActive(context, "zyrax")
        }
        return true
    }

    fun remove(context: Context, agentId: String): Boolean {
        if (agentId == "zyrax" || agentId == "jarvis") return false

        val agents = load(context).toMutableList()
        val removed = agents.removeAll { it.id == agentId }

        if (removed) {
            save(context, agents)
            if (getActive(context).id == agentId) {
                setActive(context, "zyrax")
            }
        }
        return removed
    }
}
