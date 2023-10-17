package ai.devchat.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import ai.devchat.cli.DevchatInstallationManager;
import ai.devchat.cli.DevChatConfig;
import ai.devchat.cli.DevChatResponse;
import ai.devchat.cli.DevChatResponseConsumer;
import ai.devchat.cli.DevChatWrapper;
import ai.devchat.util.Log;

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

        new Thread(() -> {
            DevchatInstallationManager dim = new DevchatInstallationManager(workPath, "0.2.9");
            dim.setup();

            DevChatConfig config = new DevChatConfig();
            config.writeDefaultConfig();

            Map<String, String> flags = new HashMap<>();
            // flags.put("flag_key", "flag_value");

            Consumer<DevChatResponse> responseCallback = response -> System.out.println(response.toString());
            DevChatResponseConsumer responseConsumer = new DevChatResponseConsumer(responseCallback);

            String devchatCommandPath = dim.getDevchatBinPath();
            String apiKey = "your_api_key_here";
            DevChatWrapper devchatWrapper = new DevChatWrapper(apiKey, devchatCommandPath);
            devchatWrapper.runPromptCommand(flags, "hello", responseConsumer);

        }).start();
    }
}
