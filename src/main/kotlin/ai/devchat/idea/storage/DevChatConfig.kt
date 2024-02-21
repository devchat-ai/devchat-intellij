package ai.devchat.idea.storage

import ai.devchat.common.PathUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.nio.file.Paths

const val DEFAULT_MODEL = "gpt-3.5-turbo"
enum class DevChatProvider(val value: String) {
    DEVCHAT("devchat.ai"),
    GENERAL("general")
}
data class ModelConfig(val provider: String?, val isStream: Boolean = true)

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

class DevChatConfig(
    private val configPath: String = Paths.get(PathUtils.workPath, "config.yml").toString()
) {
    private var data: MutableMap<String, Any?> = mutableMapOf()

    init { load() }

    fun load(): Map<String, Any?> {
        val mapper = ObjectMapper(YAMLFactory()).apply { registerKotlinModule() }
        data = try {
            mapper.readValue(File(configPath), dataType)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to load config", e)
        }
        return data
    }

    fun save() {
        val file = File(configPath)
        file.parentFile?.takeIf { !it.exists() }?.mkdirs() //Ensure parent directories exist
        val mapper = ObjectMapper(YAMLFactory())
        try {
            mapper.writeValue(file, data)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to save config", e)
        }
    }

    operator fun get(key: String? = null, delimiter: String= DEFAULT_KEY_DELIMITER): Any? {
        return if (key == null) data else {
            key.split(delimiter).fold(data as Any?) { current, k ->
                (current as? Map<*, *>)?.get(k)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun set(key: String, value: Any?) {
        val keys = key.split(".")
        val lastKey = keys.last()
        val mostNestedMap = keys.dropLast(1).fold(data) { current, k ->
            current.getOrPut(k) { mutableMapOf<String, Any?>() } as MutableMap<String, Any?>
        }
        mostNestedMap[lastKey] = value
        save()
    }

    fun replaceAll(newData: Map<String, Any?>) {
        data = newData.toMutableMap()
        save()
    }

    companion object {
        const val DEFAULT_KEY_DELIMITER: String = "."
        val dataType = object : TypeReference<MutableMap<String, Any?>>() {}
    }
}
