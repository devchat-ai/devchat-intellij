package ai.devchat.idea.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class DevChatSettingsComponent {
    val panel: JPanel
    private val apiBaseText = JBTextField()
    private val apiKeyText = JBTextField()
    private val maxLogCountText = JBTextField()
    private val defaultModelText = ComboBox(supportedModels.toTypedArray())

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("api_base"), apiBaseText, 1, false)
            .addLabeledComponent(JBLabel("api_key"), apiKeyText, 2, false)
            .addLabeledComponent(JBLabel("max_log_count"), maxLogCountText, 3, false)
            .addLabeledComponent(JBLabel("default_model"), defaultModelText, 4, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        defaultModelText.selectedItem = "gpt-3.5-turbo"
    }

    val preferredFocusedComponent: JComponent
        get() = apiBaseText
    var apiBase: String
        get() = apiBaseText.text
        set(apiBase) {
            apiBaseText.setText(apiBase)
        }
    var apiKey: String
        get() = apiKeyText.text
        set(apiKey) {
            apiKeyText.setText(apiKey)
        }
    var defaultModel: String
        get() = defaultModelText.selectedItem?.toString() ?: "gpt-3.5-turbo"
        set(defaultModel) {
            defaultModelText.selectedItem = defaultModel
        }

    var maxLogCount: Int
        get() = maxLogCountText.text.toIntOrNull() ?: 20
        set(value) {
            maxLogCountText.setText(value.toString())
        }
}
