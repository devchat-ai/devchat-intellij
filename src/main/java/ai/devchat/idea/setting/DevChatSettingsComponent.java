package ai.devchat.idea.setting;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class DevChatSettingsComponent {

    private final JPanel jPanel;
    private final JBTextField apiBaseText = new JBTextField();
    private final JBTextField apiKeyText = new JBTextField();
    private final JBTextField defaultModelText = new JBTextField();

    public DevChatSettingsComponent() {
        jPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("api_base"), apiBaseText, 1, false)
                .addLabeledComponent(new JBLabel("api_key"), apiKeyText, 2, false)
                .addLabeledComponent(new JBLabel("default_model"), defaultModelText, 3, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return jPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return apiBaseText;
    }

    @NotNull
    public String getApiBase() {
        return apiBaseText.getText();
    }

    public String getApiKey() {
        return apiKeyText.getText();
    }

    public String getDefaultModel() {
        return defaultModelText.getText();
    }

    public void setApiBase(@NotNull String apiBase) {
        apiBaseText.setText(apiBase);
    }

    public void setApiKey(@NotNull String apiKey) {
        apiKeyText.setText(apiKey);
    }

    public void setDefaultModel(@NotNull String defaultModel) {
        defaultModelText.setText(defaultModel);
    }
}
