package ai.devchat.storage

import ai.devchat.common.PathUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.nio.file.Paths

class ServerConfig(
    private val configPath: String = Paths.get(PathUtils.workPath, "server_config.yml").toString()
) {
    private var data: MutableMap<String, Any?> = mutableMapOf()

    init { load() }

    fun load(): Map<String, Any?> {
        val mapper = ObjectMapper(YAMLFactory()).apply { registerKotlinModule() }
        val configFile =  File(configPath)
        data = if (configFile.isFile) {
            try {
                mapper.readValue(configFile, dataType)
            } catch (e: Exception) {
                e.printStackTrace()
                mutableMapOf()
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

    fun get(): Any {
        return data
    }

    fun replaceAll(newData: Map<String, Any?>) {
        data = newData.toMutableMap()
        save()
    }

    companion object {
        val dataType = object : TypeReference<MutableMap<String, Any?>>() {}
    }
}

val SERVER_CONFIG: ServerConfig = ServerConfig()
