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

    public DevChatSettingsComponent() {
        jPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("api_base"), apiBaseText, 1, false)
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

    public void setApiBase(@NotNull String apiBase) {
        apiBaseText.setText(apiBase);
    }
}
