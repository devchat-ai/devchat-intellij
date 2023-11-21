package ai.devchat.idea.setting;

import ai.devchat.idea.storage.SensitiveDataStorage;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides controller functionality for DevChat settings.
 */
public class DevChatSettingsConfigurable implements Configurable {

    private DevChatSettingsComponent devChatSettingsComponent;

    // A default constructor with no arguments is required because this implementation
    // is registered in an applicationConfigurable EP

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "DevChat";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return devChatSettingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        devChatSettingsComponent = new DevChatSettingsComponent();
        return devChatSettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        DevChatSettingsState settings = DevChatSettingsState.getInstance();
        return !devChatSettingsComponent.getApiBase().equals(settings.apiBase) ||
                !devChatSettingsComponent.getApiKey().equals(settings.apiKey) ||
                !devChatSettingsComponent.getDefaultModel().equals(settings.defaultModel);
    }

    @Override
    public void apply() {
        DevChatSettingsState settings = DevChatSettingsState.getInstance();
        settings.apiBase = devChatSettingsComponent.getApiBase();
        settings.apiKey = devChatSettingsComponent.getApiKey();
        settings.defaultModel = devChatSettingsComponent.getDefaultModel();
        SensitiveDataStorage.setKey(settings.apiKey);
    }

    @Override
    public void reset() {
        DevChatSettingsState settings = DevChatSettingsState.getInstance();
        devChatSettingsComponent.setApiBase(settings.apiBase);
        devChatSettingsComponent.setApiKey(settings.apiKey);
        devChatSettingsComponent.setDefaultModel(settings.defaultModel);
        SensitiveDataStorage.setKey(settings.apiKey);
    }

    @Override
    public void disposeUIResources() {
        devChatSettingsComponent = null;
    }
}
