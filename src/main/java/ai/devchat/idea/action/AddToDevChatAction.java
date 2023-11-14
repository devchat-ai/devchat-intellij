package ai.devchat.idea.action;

import ai.devchat.common.Log;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.handler.AddContextRequestHandler;
import com.alibaba.fastjson.JSONObject;

public class AddToDevChatAction {
    private DevChatActionHandler actionHandler;

    public AddToDevChatAction() {
        actionHandler = DevChatActionHandler.getInstance();
    }

    public void execute(String filePath, String fileContent, String language, int startLine) {
        Log.info("Add to DevChat -> path: " + filePath +
                " content: " + fileContent +
                " language: " + language +
                " startLine: " + startLine);

        JSONObject payload = new JSONObject();
        payload.put("path", filePath);
        payload.put("content", fileContent);
        payload.put("languageId", language);
        payload.put("startLine", startLine);

        AddContextRequestHandler addContextRequestHandler = new AddContextRequestHandler(actionHandler);
        addContextRequestHandler.setPayload(payload);
        addContextRequestHandler.executeAction();
    }
}
