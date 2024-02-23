package ai.devchat.storage

import ai.devchat.common.PathUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.nio.file.Paths

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
