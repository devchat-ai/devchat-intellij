package ai.devchat.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

data class ModelConfig(val provider: String?, val isStream: Boolean)
class DevChatConfig(private val configPath: String) {
    var default_model: String? = null
    var models: Map<String, ModelConfig>? = null

    fun writeDefaultConfig() {
        default_model = "gpt-3.5-turbo"
        models = mapOf(
            "gpt-3.5-turbo" to ModelConfig(provider = "devchat.ai", isStream = true),
            "gpt-3.5-turbo-16k" to ModelConfig(provider = "devchat.ai", isStream = true),
            "gpt-4" to ModelConfig(provider = "devchat.ai", isStream = true),
            "gpt-3.5-turbo-1106" to ModelConfig(provider = "devchat.ai", isStream = true),
            "gpt-4-1106-preview" to ModelConfig(provider = "devchat.ai", isStream = true),
            "claude-2" to ModelConfig(provider = "general", isStream = true),
            "xinghuo-2" to ModelConfig(provider = "general", isStream = true),
            "chatglm_pro" to ModelConfig(provider = "general", isStream = true),
            "ERNIE-Bot" to ModelConfig(provider = "general", isStream = true),
        )
        save()
    }

    fun load(): DevChatConfig {
        return try {
            val mapper =
                ObjectMapper(YAMLFactory())
            mapper.readValue(File(configPath), DevChatConfig::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load config", e)
        }
    }

    fun save() {
        try {
            val mapper = ObjectMapper(YAMLFactory())
            mapper.writeValue(File(configPath), this)
        } catch (e: Exception) {
            throw RuntimeException("Failed to save config", e)
        }
    }
}