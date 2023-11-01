package ai.devchat.idea;

import ai.devchat.cli.DevChatResponse;
import ai.devchat.cli.DevChatResponseConsumer;
import ai.devchat.cli.DevChatWrapper;
import ai.devchat.common.DevChatPathUtil;
import ai.devchat.common.Log;
import ai.devchat.idea.setting.DevChatSettingsState;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DevChatActionHandler {
    private static DevChatActionHandler instance;
    private CefBrowser cefBrowser;
    private JSONObject metadata;
    private JSONObject payload;

    private Map<String, Runnable> actionMap;

    private int currentChunkId = 0;

    private DevChatActionHandler() {
        actionMap = new HashMap<>();
        registerActions();
    }

    public static synchronized DevChatActionHandler getInstance() {
        if (instance == null) {
            instance = new DevChatActionHandler();
        }
        return instance;
    }

    public void initialize(CefBrowser cefBrowser) {
        this.cefBrowser = cefBrowser;
    }

    public void initialize(JSONObject metadata, JSONObject payload) {
        this.metadata = metadata;
        this.payload = payload;
    }

    private void sendResponse(String action, String responseFunc, BiConsumer<JSONObject, JSONObject> callback) {
        JSONObject response = new JSONObject();
        response.put("action", action);

        JSONObject metadata = new JSONObject();
        JSONObject payload = new JSONObject();

        response.put("metadata", metadata);
        response.put("payload", payload);

        callback.accept(metadata, payload);

        cefBrowser.executeJavaScript(responseFunc + "('" + response.toString() + "')", "", 0);
    }

    private void registerActions() {
        actionMap.put(DevChatActions.SEND_MESSAGE_REQUEST, this::handleSendMessageRequest);
        actionMap.put(DevChatActions.SET_OR_UPDATE_KEY_REQUEST, this::handleSetOrUpdateKeyRequest);
        actionMap.put(DevChatActions.ADD_CONTEXT_REQUEST, this::handleAddContextRequest);
        actionMap.put(DevChatActions.LIST_COMMANDS_REQUEST, this::handleListCommandsRequest);
        actionMap.put(DevChatActions.LIST_CONVERSATIONS_REQUEST, this::handleListConversationsRequest);
        actionMap.put(DevChatActions.LIST_TOPICS_REQUEST, this::handleListTopicsRequest);
    }

    private void handleListCommandsRequest() {
        Log.info("Handling list commands request");

        String callbackFunc = metadata.getString("callback");
        try {
            DevChatWrapper devchatWrapper = new DevChatWrapper(DevChatPathUtil.getDevchatBinPath());
            JSONArray commandList = devchatWrapper.getCommandList();

            sendResponse(DevChatActions.LIST_COMMANDS_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");

                payload.put("commands", commandList);
            });
        } catch (Exception e) {
            Log.error("Exception occrred while executing DevChat command. Exception message: " + e.getMessage());

            sendResponse(DevChatActions.LIST_COMMANDS_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", e.getMessage());
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

    private void handleSendMessageRequest() {
        Log.info("Handling send message request");

        String message = payload.getString("message");
        String context = payload.getString("context");
        String parent = metadata.getString("parent");
        String callbackFunc = metadata.getString("callback");

        try {
            Map<String, String> flags = new HashMap<>();
            if (context != null && !context.isEmpty()) {
                flags.put("context", context);
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

            sendResponse(DevChatActions.SEND_MESSAGE_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("currentChunkId", 0);
                metadata.put("isFinalChunk", true);
                metadata.put("finishReason", "error");
                metadata.put("error", "Exception occurred while executing 'devchat' command.");
            });
        }
    }

    private void handleSetOrUpdateKeyRequest() {
        Log.info("Handling set or update key request");

        String key = payload.getString("key");
        String callbackFunc = metadata.getString("callback");
        if (key == null || key.isEmpty()) {
            Log.error("Key is empty");
            sendResponse(DevChatActions.SET_OR_UPDATE_KEY_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", "key is empty");
            });
        } else {
            SensitiveDataStorage.setKey(key);
            sendResponse(DevChatActions.SET_OR_UPDATE_KEY_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");
            });
        }
    }

    private void handleAddContextRequest() {
        sendResponse("addContext/request", "AddContextFromEditor", (metadata, payload) -> {
            payload.put("file", this.payload.getString("file"));
            payload.put("content", this.payload.getString("content"));
        });
    }

    private void handleListConversationsRequest() {
        Log.info("Handling list conversations request");

        String callbackFunc = metadata.getString("callback");
        String topicHash = metadata.getString("topicHash");
        try {
            DevChatWrapper devchatWrapper = new DevChatWrapper(DevChatPathUtil.getDevchatBinPath());
            /* conversations' format:
            [
              {
                "user": "Daniel Hu <tao.hu@merico.dev>",
                "date": 1698828867,
                "context": [
                  {
                    "content": "{\"command\":\"ls -l\",\"content\":\"total 8\\n-rw-r--r--@ 1 danielhu  staff  7 Nov  1 16:49 a.py\\n\"}",
                    "role": "system"
                  }
                ],
                "request": "hello again",
                "responses": [
                  "Hello! How can I assist you today?"
                ],
                "request_tokens": 135,
                "response_tokens": 19,
                "hash": "ccbbd3f8c892277d3ea566545bb64b68ba3e34257f9b324551a52449f8f19e17",
                "parent": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b"
              },
              {
                "user": "Daniel Hu <tao.hu@merico.dev>",
                "date": 1698828624,
                "context": [
                  {
                    "content": "{\"languageId\":\"python\",\"path\":\"a.py\",\"startLine\":0,\"content\":\"adkfjj\\n\"}",
                    "role": "system"
                  }
                ],
                "request": "hello",
                "responses": [
                  "Hi there! How can I assist you with Python today?"
                ],
                "request_tokens": 46,
                "response_tokens": 22,
                "hash": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b",
                "parent": null
              }
            ]
            */
            JSONArray conversations = devchatWrapper.listConversationsInOneTopic(topicHash);
            // remove request_tokens and response_tokens in the conversations object
            for (int i = 0; i < conversations.size(); i++) {
                JSONObject conversation = conversations.getJSONObject(i);
                conversation.remove("request_tokens");
                conversation.remove("response_tokens");
            }

            sendResponse(DevChatActions.LIST_CONVERSATIONS_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");

                payload.put("conversations", conversations);
            });
        } catch (Exception e) {
            Log.error("Exception occrred while executing DevChat command. Exception message: " + e.getMessage());

            sendResponse(DevChatActions.LIST_CONVERSATIONS_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", e.getMessage());
            });
        }
    }

    private void handleListTopicsRequest() {
        Log.info("Handling list topics request");

        String callbackFunc = metadata.getString("callback");
        try {
            DevChatWrapper devchatWrapper = new DevChatWrapper(DevChatPathUtil.getDevchatBinPath());
            /* topics format:
            [
              {
                "root_prompt": {
                  "user": "Daniel Hu <tao.hu@merico.dev>",
                  "date": 1698828624,
                  "context": [
                    {
                      "content": "{\"languageId\":\"python\",\"path\":\"a.py\",\"startLine\":0,\"content\":\"adkfjj\\n\"}",
                      "role": "system"
                    }
                  ],
                  "request": "hello",
                  "responses": [
                    "Hi there! How can I assist you with Python today?"
                  ],
                  "request_tokens": 46,
                  "response_tokens": 22,
                  "hash": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b",
                  "parent": null
                },
                "latest_time": 1698828867,
                "title": null,
                "hidden": false
              }
            ]
             */
            JSONArray topics = devchatWrapper.listTopics();
            // remove request_tokens and response_tokens in the topics object, then update title field.
            for (int i = 0; i < topics.size(); i++) {
                JSONObject topic = topics.getJSONObject(i);
                topic.remove("latest_time");
                topic.remove("hidden");
                // set title = root_prompt.request + "-" + root_prompt.responses[0]
                JSONObject rootPrompt = topic.getJSONObject("root_prompt");
                String title = rootPrompt.getString("request") + "-" + rootPrompt.getJSONArray("responses").getString(0);
                rootPrompt.put("title", title);
            }

            sendResponse(DevChatActions.LIST_TOPICS_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");

                payload.put("topics", topics);
            });
        } catch (Exception e) {
            Log.error("Exception occrred while executing DevChat command. Exception message: " + e.getMessage());

            sendResponse(DevChatActions.LIST_TOPICS_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", e.getMessage());
            });
        }
    }

    public void executeAction(String action) {
        Runnable actionHandler = actionMap.get(action);
        if (actionHandler != null) {
            actionHandler.run();
        }
    }

    @NotNull
    private DevChatResponseConsumer getResponseConsumer(String responseFunc) {
        Consumer<DevChatResponse> jsCallback = response -> {
            sendResponse(DevChatActions.SEND_MESSAGE_RESPONSE, responseFunc, (metadata, payload) -> {
                currentChunkId += 1;
                metadata.put("currentChunkId", currentChunkId);
                metadata.put("isFinalChunk", response.getPromptHash() == null);
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

    public JSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
