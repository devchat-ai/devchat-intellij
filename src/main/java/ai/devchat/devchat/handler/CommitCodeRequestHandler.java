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
import java.util.function.BiConsumer;

public class CommitCodeRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public CommitCodeRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling commit code request");

        String callbackFunc = metadata.getString("callback");
        String message = payload.getString("message");
        String[] commitCommand = {"git", "commit", "-m", message};

        StringBuilder result = new StringBuilder();
        StringBuilder error = new StringBuilder();

        BufferedReader reader = null;
        BufferedReader errorReader = null;

        try {
            String projectDir = devChatActionHandler.getProject().getBasePath();
            Log.info("Preparing to execute command: git commit -m " + message + " in " + projectDir);
            Process process = Runtime.getRuntime().exec(commitCommand, null, new File(projectDir));
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

        BiConsumer<JSONObject, JSONObject> processCommitResponse = (metadata, payload) -> {
            if (error.isEmpty()) {
                metadata.put("status", "success");
                metadata.put("error", "");
            } else {
                metadata.put("status", "error");
                metadata.put("error", error.toString());
            }
        };

        devChatActionHandler.sendResponse(DevChatActions.COMMIT_CODE_RESPONSE, callbackFunc, processCommitResponse);
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
