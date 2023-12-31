package ai.devchat.idea.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.util.*
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
    private val languageText = ComboBox(arrayOf("zh", "en"))

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("API base"), apiBaseText, 1, false)
            .addLabeledComponent(JBLabel("API key"), apiKeyText, 2, false)
            .addLabeledComponent(JBLabel("Max log count"), maxLogCountText, 3, false)
            .addLabeledComponent(JBLabel("Default model"), defaultModelText, 4, false)
            .addLabeledComponent(JBLabel("Language"), languageText, 5, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        defaultModelText.selectedItem = "gpt-3.5-turbo"
        languageText.selectedItem = Locale.getDefault().language.takeIf { it == "zh" } ?: "en"
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

    var language: String
        get() = languageText.selectedItem?.toString() ?: (Locale.getDefault().language.takeIf { it == "zh" } ?: "en")
        set(language) {
            languageText.selectedItem = language
        }
}
