package ai.devchat.controller;

import ai.devchat.cli.DevChat;
import ai.devchat.exception.DevChatSetupException;
import ai.devchat.util.Log;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

public class DevChatToolWindow implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(
                new DevChatToolWindowContent().getContent(),
                "",
                false);
        contentManager.addContent(content);

        String workPath = PathManager.getPluginsPath() + "/devchat";
        Log.info("Work path is: " + workPath);

        try {
            DevChat devchat = new DevChat(workPath, "0.2.9");
            devchat.setup();
        } catch (DevChatSetupException e) {
            e.printStackTrace();
        }
    }
}
