package ai.devchat.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

/*
* default_model: gpt-3.5-turbo
* models:
*   gpt-3.5-turbo:
*     provider: devchat.ai
*     stream: true
*/
class DevChatConfig {
    private var configPath: String

    // Getters and Setters
    var default_model: String? = null
    var models: Map<String, ModelConfig>? = null

    constructor() {
        // default config path
        configPath = System.getProperty("user.home") + "/.chat/config.yml"
    }

    constructor(configPath: String) {
        this.configPath = configPath
    }

    internal open class ModelConfig {
        // getters and setters
        var provider: String? = null
        var isStream = false
    }

    fun writeDefaultConfig() {
        default_model = "gpt-3.5-turbo"
        models = java.util.Map.of(
            "gpt-3.5-turbo",
            object : ModelConfig() {
                init {
                    provider = "devchat.ai"
                    isStream = true
                }
            },
            "gpt-3.5-turbo-16k",
            object : ModelConfig() {
                init {
                    provider = "devchat.ai"
                    isStream = true
                }
            },
            "gpt-4",
            object : ModelConfig() {
                init {
                    provider = "devchat.ai"
                    isStream = true
                }
            },
            "claude-2",
            object : ModelConfig() {
                init {
                    provider = "general"
                    isStream = true
                }
            })
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
