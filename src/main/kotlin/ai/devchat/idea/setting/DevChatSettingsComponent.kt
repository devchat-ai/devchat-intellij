package ai.devchat.idea.setting

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
    private val defaultModelText = JBTextField()

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("api_base"), apiBaseText, 1, false)
            .addLabeledComponent(JBLabel("api_key"), apiKeyText, 2, false)
            .addLabeledComponent(JBLabel("default_model"), defaultModelText, 3, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
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
        get() = defaultModelText.text
        set(defaultModel) {
            defaultModelText.setText(defaultModel)
        }
}
