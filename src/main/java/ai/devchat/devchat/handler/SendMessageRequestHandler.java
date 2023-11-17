package ai.devchat.devchat.handler;

import ai.devchat.cli.DevChatResponse;
import ai.devchat.cli.DevChatResponseConsumer;
import ai.devchat.cli.DevChatWrapper;
import ai.devchat.common.DevChatPathUtil;
import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;
import ai.devchat.idea.setting.DevChatSettingsState;
import ai.devchat.idea.storage.SensitiveDataStorage;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SendMessageRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;
    private int currentChunkId = 0;

    public SendMessageRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling send message request");

        String message = payload.getString("message");
        String parent = metadata.getString("parent");
        String callbackFunc = metadata.getString("callback");

        try {
            Map<String, String> flags = new HashMap<>();

            JSONArray contextArray = payload.getJSONArray("contexts");
            if (contextArray != null) {
                List<String> contextFilePaths = new ArrayList<>();
                for (int i = 0; i < contextArray.size(); i++) {
                    JSONObject context = contextArray.getJSONObject(i);
                    String contextType = context.getString("type");
                    String contextPath = null;

                    if ("code".equals(contextType)) {
                        String path = context.getString("path");
                        String filename = path.substring(path.lastIndexOf("/") + 1, path.length());
                        contextPath = createTempFileFromContext(context, filename);
                    } else if ("command".equals(contextType)) {
                        contextPath = createTempFileFromContext(context, "custom.txt");
                    }

                    if (contextPath != null) {
                        contextFilePaths.add(contextPath);
                        Log.info("Context file path: " + contextPath);
                    }
                }
                flags.put("context", String.join(",", contextFilePaths));
            }

            if (parent != null && !parent.isEmpty()) {
                flags.put("parent", parent);
            }

            Log.info("Preparing to retrieve the command in the message...");
            message = handleCommandAndInstruct(message, flags);
            Log.info("Message is: " + message);

            String devchatCommandPath = DevChatPathUtil.getDevchatBinPath();
            String apiKey = SensitiveDataStorage.getKey();
            String apiBase = "";
            if (apiKey.startsWith("sk-")) {
                apiBase = "https://api.openai.com/v1";
            } else if (apiKey.startsWith("DC.")) {
                apiBase = "https://api.devchat.ai/v1";
            }

            DevChatSettingsState settings = DevChatSettingsState.getInstance();
            if (settings.apiBase != null && !settings.apiBase.isEmpty()) {
                apiBase = settings.apiBase;
            }

            DevChatResponseConsumer responseConsumer = getResponseConsumer(callbackFunc);
            DevChatWrapper devchatWrapper = new DevChatWrapper(apiBase, apiKey, devchatCommandPath);
            devchatWrapper.runPromptCommand(flags, message, responseConsumer);
        } catch (Exception e) {
            Log.error("Exception occurred while executing DevChat command. Exception message: " + e.getMessage());

            devChatActionHandler.sendResponse(DevChatActions.SEND_MESSAGE_RESPONSE, callbackFunc,
                    (metadata, payload) -> {
                        metadata.put("currentChunkId", 0);
                        metadata.put("isFinalChunk", true);
                        metadata.put("finishReason", "error");
                        metadata.put("error", "Exception occurred while executing 'devchat' command.");
                    });
        }
    }

    private String handleCommandAndInstruct(String message, Map<String, String> flags) throws IOException {
        DevChatWrapper devchatWrapper = new DevChatWrapper(DevChatPathUtil.getDevchatBinPath());
        String[] commandNamesList = devchatWrapper.getCommandNamesList();
        Log.info("Command names list: " + String.join(", ", commandNamesList));
        String runResult = null;

        // Loop through the command names and check if message starts with it
        for (String command : commandNamesList) {
            if (message.startsWith("/" + command + " ")) {
                message = message.substring(command.length() + 2); // +2 to take into account the '/' and the space ' '
                runResult = devchatWrapper.runRunCommand(command, null);
                break;
            }
        }
        // If we didn't find a matching command, assume the default behavior
        if (runResult != null) {
            // Write the result to a temporary file
            Path tempFile = Files.createTempFile("devchat_", ".tmp");
            Files.write(tempFile, runResult.getBytes());

            // Add the temporary file path to the flags with key --instruct
            flags.put("instruct", tempFile.toString());
        }

        return message;
    }

    @NotNull
    private DevChatResponseConsumer getResponseConsumer(String responseFunc) {
        Consumer<DevChatResponse> jsCallback = response -> {
            devChatActionHandler.sendResponse(DevChatActions.SEND_MESSAGE_RESPONSE, responseFunc,
                    (metadata, payload) -> {
                        currentChunkId += 1;
                        metadata.put("currentChunkId", currentChunkId);
                        metadata.put("isFinalChunk", response.getPromptHash() != null);
                        metadata.put("finishReason", response.getPromptHash() != null ? "success" : "");
                        metadata.put("error", "");

                        payload.put("message", response.getMessage());
                        payload.put("user", response.getUser());
                        payload.put("date", response.getDate());
                        payload.put("promptHash", response.getPromptHash());
                    });
        };
        return new DevChatResponseConsumer(jsCallback);
    }

    private String createTempFileFromContext(JSONObject context, String filename) {
        File tempFile;

        try {
            tempFile = File.createTempFile("devchat-tmp-", "-" + filename);
        } catch (IOException e) {
            Log.error("Failed to create a temporary file." + e.getMessage());
            return null;
        }

        JSONObject newJson = new JSONObject();
        if (context.getString("type").equals("code")) {
            newJson.put("languageId", context.getString("languageId"));
            newJson.put("path", context.getString("path"));
            newJson.put("startLine", context.getInteger("startLine"));
            newJson.put("content", context.getString("content"));
        } else if (context.getString("type").equals("command")) {
            newJson.put("command", context.getString("command"));
            newJson.put("content", context.getString("content"));
        }

        try (FileWriter fileWriter = new FileWriter(tempFile)) {
            fileWriter.write(newJson.toJSONString());
        } catch (IOException e) {
            Log.error("Failed to write to the temporary file." + e.getMessage());
        }

        return tempFile.getAbsolutePath();
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
