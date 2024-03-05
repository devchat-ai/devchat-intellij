package ai.devchat.plugin

import ai.devchat.common.OSInfo
import ai.devchat.storage.CONFIG
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
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
    private val apiBaseText = JBTextField(50)
    private val apiKeyText = JBTextField(50)
    private val maxLogCountText = JBTextField(50)
    private val defaultModelText = ComboBox((CONFIG["models"] as Map<*, *>).keys.toTypedArray())
    private val languageText = ComboBox(arrayOf("zh", "en"))
    private val pythonForChatField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(TextBrowseFolderListener(
            FileChooserDescriptorFactory.createSingleFileDescriptor(),
        ))
        this.textField.columns = 50
        this.text = if (OSInfo.isWindows) "python" else "python3"
    }
    private val pythonForCommandsField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(TextBrowseFolderListener(
            FileChooserDescriptorFactory.createSingleFileDescriptor(),
        ))
        this.textField.columns = 50
        this.text = if (OSInfo.isWindows) "python" else "python3"
    }

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("API base"), apiBaseText, 1, false)
            .addLabeledComponent(JBLabel("Access key"), apiKeyText, 2, false)
            .addLabeledComponent(JBLabel("Max log count"), maxLogCountText, 3, false)
            .addLabeledComponent(JBLabel("Default model"), defaultModelText, 4, false)
            .addLabeledComponent(JBLabel("Language"), languageText, 5, false)
            .addLabeledComponent(JBLabel("Python for chat"), pythonForChatField, 6, false)
            .addLabeledComponent(JBLabel("Python for commands"), pythonForCommandsField, 7, false)
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
            apiBaseText.text = apiBase
        }
    var apiKey: String
        get() = apiKeyText.text
        set(apiKey) {
            apiKeyText.text = apiKey
        }
    var defaultModel: String
        get() = defaultModelText.selectedItem?.toString() ?: "gpt-3.5-turbo"
        set(defaultModel) {
            defaultModelText.selectedItem = defaultModel
        }

    var maxLogCount: Int
        get() = maxLogCountText.text.toIntOrNull() ?: 20
        set(value) {
            maxLogCountText.text = value.toString()
        }

    var language: String
        get() = languageText.selectedItem?.toString() ?: (Locale.getDefault().language.takeIf { it == "zh" } ?: "en")
        set(language) {
            languageText.selectedItem = language
        }

    var pythonForChat: String
        get() = pythonForChatField.text
        set(newValue) {
            pythonForChatField.text = newValue
        }

    var pythonForCommands: String
        get() = pythonForCommandsField.text
        set(newValue) {
            pythonForCommandsField.text = newValue
        }
}