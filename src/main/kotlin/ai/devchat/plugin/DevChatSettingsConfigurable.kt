package ai.devchat.plugin

import ai.devchat.storage.*
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Provides controller functionality for DevChat settings.
 */
class DevChatSettingsConfigurable : Configurable {
    private var devChatSettingsComponent: DevChatSettingsComponent? = DevChatSettingsComponent()
    private val keyMap =   mapOf(
        "providers.devchat.api_base" to "apiBase",
        "providers.devchat.api_key" to "apiKey",
        "default_model" to "defaultModel",
        "max_log_count" to "maxLogCount",
        "language" to "language",
        "python_for_chat" to "pythonForChat",
        "python_for_commands" to "pythonForCommands",
    )

    // A default constructor with no arguments is required because this implementation
    // is registered in an applicationConfigurable EP
    override fun getDisplayName(): @Nls(capitalization = Nls.Capitalization.Title) String {
        return "DevChat"
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return devChatSettingsComponent!!.preferredFocusedComponent
    }

    override fun createComponent(): JComponent {
        return devChatSettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        return keyMap.any { (key, prop) ->
            DevChatSettingsComponent::class.memberProperties.firstOrNull {
                it.name == prop
            }?.call(devChatSettingsComponent) != CONFIG[key]
        }
    }

    override fun apply() {
        keyMap.forEach { (key, prop) ->
            CONFIG[key] = DevChatSettingsComponent::class.memberProperties.firstOrNull {
                it.name == prop
            }?.call(devChatSettingsComponent)
        }
    }

    override fun reset() {
        keyMap.forEach { (key, prop) ->
            val property = DevChatSettingsComponent::class.memberProperties.firstOrNull {
                it.name == prop
            }
            if (property is KMutableProperty1<*, *>) {
                property.isAccessible = true
                property.setter.call(devChatSettingsComponent, CONFIG[key])
            }
        }
    }

    override fun disposeUIResources() {
        devChatSettingsComponent = null
    }

    companion object {
        fun get(): DevChatSettingsConfigurable {
            return DevChatSettingsConfigurable()
        }
    }
}