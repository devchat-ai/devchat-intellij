package ai.devchat.storage

import ai.devchat.common.PathUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.nio.file.Paths

val defaultModelConfig = mapOf("provider" to "devchat", "stream" to true)

val supportedModels = listOf(
    "hzwxai/Mixtral-8x7B-Instruct-v0.1-GPTQ",
)

class DevChatConfig(
    private val configPath: String = Paths.get(PathUtils.workPath, "config.yml").toString()
) {
    private var data: MutableMap<String, Any?> = mutableMapOf()

    init {
        load()
        migrate()
    }

    private fun migrate() {
        if (this["migrated"] as? Boolean == true) return
        val oldSettings = DevChatSettingsState.instance
        mapOf(
            "providers.devchat.api_base" to oldSettings.apiBase,
            "providers.devchat.api_key" to oldSettings.apiKey,
            "providers.devchat.client" to "general",
            "default_model" to oldSettings.defaultModel,
            "max_log_count" to oldSettings.maxLogCount,
            "language" to oldSettings.language,
            "python_for_chat" to oldSettings.pythonForChat,
            "python_for_command" to oldSettings.pythonForCommands,
            "models" to supportedModels.associateBy({it}, { defaultModelConfig })
        ).forEach { (key, value) ->
            if (this[key] == null) {
                this[key] = value
            }
        }
        this["migrated"] = true
    }

    fun load(): Map<String, Any?> {
        val mapper = ObjectMapper(YAMLFactory()).apply { registerKotlinModule() }
        val configFile =  File(configPath)
        data = if (configFile.isFile) {
            try {
                mapper.readValue(configFile, dataType)
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("Failed to load config", e)
            }
        } else { mutableMapOf() }
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

    operator fun set(key: String, value: Any?) {
        set(key, value, DEFAULT_KEY_DELIMITER)
    }

    @Suppress("UNCHECKED_CAST")
    fun set(key: String, value: Any?, delimiter: String= DEFAULT_KEY_DELIMITER) {
        val keys = key.split(delimiter)
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

val CONFIG: DevChatConfig = DevChatConfig()
