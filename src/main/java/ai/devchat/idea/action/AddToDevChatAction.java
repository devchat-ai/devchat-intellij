package ai.devchat.idea.action;

import ai.devchat.idea.ActionHandler;
import ai.devchat.idea.Actions;
import com.alibaba.fastjson.JSONObject;

public class AddToDevChatAction {
    private ActionHandler actionHandler;

    public AddToDevChatAction() {
        actionHandler = ActionHandler.getInstance();
    }

    public void execute(String filePath, String fileContent) {
        JSONObject payload = new JSONObject();
        payload.put("file", filePath);
        payload.put("content", fileContent);
        actionHandler.initialize(null, payload);
        actionHandler.executeAction(Actions.ADD_CONTEXT_REQUEST);
    }
}
