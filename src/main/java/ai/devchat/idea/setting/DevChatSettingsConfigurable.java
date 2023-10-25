package ai.devchat.idea.setting;

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
    return !devChatSettingsComponent.getApiBase().equals(settings.apiBase);
  }

  @Override
  public void apply() {
    DevChatSettingsState settings = DevChatSettingsState.getInstance();
    settings.apiBase = devChatSettingsComponent.getApiBase();
  }

  @Override
  public void reset() {
    DevChatSettingsState settings = DevChatSettingsState.getInstance();
    devChatSettingsComponent.setApiBase(settings.apiBase);
  }

  @Override
  public void disposeUIResources() {
    devChatSettingsComponent = null;
  }
}
