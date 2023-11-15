package ai.devchat.devchat.handler;

import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class AddContextRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public AddContextRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling add context request");

        String command = payload.getString("command");
        StringBuilder result = new StringBuilder();
        StringBuilder error = new StringBuilder();

        BufferedReader reader = null;
        BufferedReader errorReader = null;

        try {
            String projectDir = devChatActionHandler.getProject().getBasePath();
            Process process = Runtime.getRuntime().exec(command, null, new File(projectDir));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }
        } catch (IOException e) {
            error.append(e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (errorReader != null) {
                try {
                    errorReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String callbackFunc = metadata.getString("callback");

        Log.info("1111111");
        Log.info(result.toString());
        Log.info(error.toString());

        if (error.isEmpty()) {
            final String finalResult = result.toString();
            devChatActionHandler.sendResponse(DevChatActions.ADD_CONTEXT_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");
                payload.put("command", command);
                payload.put("content", finalResult);
            });
        } else {
            final String finalError = error.toString();
            devChatActionHandler.sendResponse(DevChatActions.ADD_CONTEXT_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", finalError);
                payload.put("command", command);
                payload.put("content", "");
            });
        }
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
