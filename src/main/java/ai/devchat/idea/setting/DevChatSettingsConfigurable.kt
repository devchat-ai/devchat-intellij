package ai.devchat.idea.setting

import ai.devchat.idea.storage.SensitiveDataStorage
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Provides controller functionality for DevChat settings.
 */
class DevChatSettingsConfigurable : Configurable {
    private var devChatSettingsComponent: DevChatSettingsComponent? = null

    // A default constructor with no arguments is required because this implementation
    // is registered in an applicationConfigurable EP
    override fun getDisplayName(): @Nls(capitalization = Nls.Capitalization.Title) String? {
        return "DevChat"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return devChatSettingsComponent.getPreferredFocusedComponent()
    }

    override fun createComponent(): JComponent? {
        devChatSettingsComponent = DevChatSettingsComponent()
        return devChatSettingsComponent.getPanel()
    }

    override fun isModified(): Boolean {
        val settings: DevChatSettingsState = DevChatSettingsState.Companion.getInstance()
        return devChatSettingsComponent.getApiBase() != settings.apiBase ||
                devChatSettingsComponent.getApiKey() != settings.apiKey ||
                devChatSettingsComponent.getDefaultModel() != settings.defaultModel
    }

    override fun apply() {
        val settings: DevChatSettingsState = DevChatSettingsState.Companion.getInstance()
        settings.apiBase = devChatSettingsComponent.getApiBase()
        settings.apiKey = devChatSettingsComponent.getApiKey()
        settings.defaultModel = devChatSettingsComponent.getDefaultModel()
        SensitiveDataStorage.setKey(settings.apiKey)
    }

    override fun reset() {
        val settings: DevChatSettingsState = DevChatSettingsState.Companion.getInstance()
        devChatSettingsComponent.setApiBase(settings.apiBase)
        devChatSettingsComponent.setApiKey(settings.apiKey)
        devChatSettingsComponent.setDefaultModel(settings.defaultModel)
        SensitiveDataStorage.setKey(settings.apiKey)
    }

    override fun disposeUIResources() {
        devChatSettingsComponent = null
    }
}
