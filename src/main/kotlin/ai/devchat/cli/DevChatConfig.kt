package ai.devchat.cli

import ai.devchat.common.DevChatPathUtil.workPath
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

data class ModelConfig(var provider: String? = null, var isStream: Boolean = false)

class DevChatConfig {
    private var configPath: String

    // Getters and Setters
    private var defaultModel: String? = null
    private var models: Map<String, ModelConfig>? = null

    constructor() {
        // default config path
        configPath = "$workPath/config.yml"
    }

    constructor(configPath: String) {
        this.configPath = configPath
    }

    fun writeDefaultConfig() {
        defaultModel = "gpt-3.5-turbo"
        models = mapOf(
            "gpt-3.5-turbo" to ModelConfig(
                provider = "devchat.ai", isStream = true
            ), "gpt-3.5-turbo-16k" to ModelConfig(
                provider = "devchat.ai", isStream = true
            ), "gpt-4" to ModelConfig(
                provider = "devchat.ai", isStream = true
            ), "claude-2" to ModelConfig(
                provider = "general", isStream = true
            )
        )
        save()
    }

    fun load(): DevChatConfig {
        return try {
            ObjectMapper(YAMLFactory()).readValue(File(configPath), DevChatConfig::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load config", e)
        }
    }

    private fun save() {
        try {
            ObjectMapper(YAMLFactory()).writeValue(File(configPath), this)
        } catch (e: Exception) {
            throw RuntimeException("Failed to save config", e)
        }
    }
}
