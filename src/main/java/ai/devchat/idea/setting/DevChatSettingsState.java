package ai.devchat.idea.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supports storing the DevChat settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent DevChat settings are stored.
 */
@State(
        name = "org.intellij.sdk.settings.DevChatSettingsState",
        storages = @Storage("DevChatSettings.xml")
)
public class DevChatSettingsState implements PersistentStateComponent<DevChatSettingsState> {

  public String apiBase = "https://api.devchat.ai/v1";

  public String apiKey = "change_me";

  public static DevChatSettingsState getInstance() {
    return ApplicationManager.getApplication().getService(DevChatSettingsState.class);
  }

  @Nullable
  @Override
  public DevChatSettingsState getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull DevChatSettingsState state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
