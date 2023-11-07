package ai.devchat.devchat.handler;

import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;

import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

public class InsertCodeRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public InsertCodeRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling insert code request");
        Project project = devChatActionHandler.getProject();
        String contentText = payload.getString("content");
        String callbackFunc = metadata.getString("callback");

        ApplicationManager.getApplication().invokeLater(() -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            Document document = editor.getDocument();
            int offset = editor.getCaretModel().getOffset();
            CommandProcessor.getInstance()
                    .executeCommand(project,
                            () -> ApplicationManager.getApplication()
                                    .runWriteAction(() -> document.insertString(offset, contentText)),
                            "InsertText", null);

            devChatActionHandler.sendResponse(DevChatActions.INSERT_CODE_RESPONSE, callbackFunc,
                    (metadata, payload) -> {
                        metadata.put("status", "success");
                    });
        });
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
