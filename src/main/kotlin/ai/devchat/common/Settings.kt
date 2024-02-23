package ai.devchat.common

import ai.devchat.storage.CONFIG

object Settings {
    fun getAPISettings() : Triple<String?, String?, String?> {
        val apiBaseK = "providers.devchat.api_base"
        val apiKey = CONFIG["providers.devchat.api_key"] as? String
        val defaultModel = CONFIG["default_model"] as? String
        var apiBase = CONFIG[apiBaseK] as String
        if (apiBase.isEmpty()) {
            apiBase = when {
                apiKey?.startsWith("sk-") == true -> "https://api.openai.com/v1"
                apiKey?.startsWith("DC.") == true -> "https://api.devchat.ai/v1"
                else -> apiBase
            }
            CONFIG[apiBaseK] = apiBase
        }
        return Triple(apiKey, apiBase, defaultModel)
    }
}