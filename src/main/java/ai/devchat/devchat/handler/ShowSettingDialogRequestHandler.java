package ai.devchat.devchat.handler;

import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ShowSettingDialogRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public ShowSettingDialogRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling show setting dialog request.");

        DataContext dataContext = new DataContext() {
            @Nullable
            @Override
            public Object getData(@NotNull String dataId) {
                if (CommonDataKeys.PROJECT.getName().equals(dataId)) {
                    return devChatActionHandler.getProject();
                }
                return null;
            }
        };
        final ActionManager actionManager = ActionManager.getInstance();
        final AnAction settingsAction = actionManager.getAction("ShowSettings");
        final AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext);
        ApplicationManager.getApplication().invokeLater(() -> settingsAction.actionPerformed(event));

        Project project = devChatActionHandler.getProject();
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "DevChat");
    }

    @Override
    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    @Override
    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
