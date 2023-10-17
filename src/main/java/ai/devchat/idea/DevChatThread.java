package ai.devchat.idea;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.intellij.openapi.application.PathManager;

import ai.devchat.cli.*;
import ai.devchat.common.Log;

public class DevChatThread extends Thread {
    @Override
    public void run() {
        String workPath = PathManager.getPluginsPath() + "/devchat";
        Log.info("Work path is: " + workPath);

        DevChatInstallationManager dim = new DevChatInstallationManager(workPath, "0.2.9");
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
    }
}
