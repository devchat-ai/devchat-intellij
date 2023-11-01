package ai.devchat.idea.action;

import ai.devchat.idea.DevChatActionHandler;
import ai.devchat.idea.DevChatActions;
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
        actionHandler.initialize(null, payload);
        actionHandler.executeAction(DevChatActions.ADD_CONTEXT_REQUEST);
    }
}
