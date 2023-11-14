package ai.devchat.devchat.handler;

import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import com.alibaba.fastjson.JSONObject;
import com.intellij.psi.PsiFileFactory;

public class ViewDiffRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public ViewDiffRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling view diff request");
        String callbackFunc = metadata.getString("callback");
        String diffContent = payload.getString("content");

        PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(devChatActionHandler.getProject());
//        PsiFile psiFile = psiFileFactory.createFileFromText("yourFileName.java", , diffContent);
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
