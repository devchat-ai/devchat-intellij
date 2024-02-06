package ai.devchat.idea.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

const val defaultModel = "gpt-3.5-turbo"
enum class DevChatProvider(val value: String) {
    DEVCHAT("devchat.ai"),
    GENERAL("general")
}

val supportedModels = mapOf(
    "gpt-3.5-turbo" to ModelConfig(provider = DevChatProvider.DEVCHAT.value),
    "gpt-4" to ModelConfig(provider = DevChatProvider.DEVCHAT.value),
    "gpt-4-turbo-preview" to ModelConfig(provider = DevChatProvider.DEVCHAT.value),
    "claude-2.1" to ModelConfig(provider = DevChatProvider.GENERAL.value),
    "xinghuo-3.5" to ModelConfig(provider = DevChatProvider.GENERAL.value),
    "GLM-4" to ModelConfig(provider = DevChatProvider.GENERAL.value),
    "ERNIE-Bot-4.0" to ModelConfig(provider = DevChatProvider.GENERAL.value),
    "llama-2-70b-chat" to ModelConfig(provider = DevChatProvider.GENERAL.value),
    "togetherai/codellama/CodeLlama-70b-Instruct-hf" to ModelConfig(provider = DevChatProvider.DEVCHAT.value),
    "togetherai/mistralai/Mixtral-8x7B-Instruct-v0.1" to ModelConfig(provider = DevChatProvider.DEVCHAT.value),
    "minimax/abab6-chat" to ModelConfig(provider = DevChatProvider.DEVCHAT.value),
)

data class ModelConfig(val provider: String?, val isStream: Boolean = true)
class DevChatConfig(private val configPath: String) {
    var default_model: String? = defaultModel
    var models: Map<String, ModelConfig>? = supportedModels

    fun load(): DevChatConfig {
        val mapper = ObjectMapper(YAMLFactory())
        return try {
            mapper.readValue(File(configPath), DevChatConfig::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to load config", e)
        }
    }

    fun save() {
        val mapper = ObjectMapper(YAMLFactory())
        try {
            mapper.writeValue(File(configPath), this)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to save config", e)
        }
    }
}
