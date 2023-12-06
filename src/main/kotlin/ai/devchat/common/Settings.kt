package ai.devchat.common

import ai.devchat.idea.settings.DevChatSettingsState
import ai.devchat.idea.storage.SensitiveDataStorage

object Settings {
    fun getAPISettings() : Triple<String?, String?, String?> {
        val settings = DevChatSettingsState.instance
        val apiKey = SensitiveDataStorage.key
        if (settings.apiBase.isEmpty()) {
            settings.apiBase = when {
                apiKey?.startsWith("sk-") == true -> "https://api.openai.com/v1"
                apiKey?.startsWith("DC.") == true -> "https://api.devchat.ai/v1"
                else -> settings.apiBase
            }
        }
        return Triple(apiKey, settings.apiBase, settings.defaultModel)
    }
}