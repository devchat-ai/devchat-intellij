package ai.devchat.idea.action;

import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.handler.AddContextRequestHandler;
import com.alibaba.fastjson.JSONObject;

public class AddToDevChatAction {
    private DevChatActionHandler actionHandler;

    public AddToDevChatAction() {
        actionHandler = DevChatActionHandler.getInstance();
    }

    public void execute(String filePath, String fileContent) {
        JSONObject payload = new JSONObject();
        payload.put("file", filePath);
        payload.put("content", fileContent);

        AddContextRequestHandler addContextRequestHandler = new AddContextRequestHandler(actionHandler);
        addContextRequestHandler.setPayload(payload);
        addContextRequestHandler.executeAction();
    }
}
