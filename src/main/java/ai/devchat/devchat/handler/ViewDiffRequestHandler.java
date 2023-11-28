package ai.devchat.devchat.handler;

import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import com.alibaba.fastjson.JSONObject;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class ViewDiffRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public ViewDiffRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }


    @Override
    public void executeAction() {
        String diffContent = payload.getString("content");
        Project project = devChatActionHandler.getProject();

        ApplicationManager.getApplication().invokeLater(() -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) {
                // Handle the case when no editor is opened
                return;
            }
            Document document = editor.getDocument();
            VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file == null) {
                // Handle the case when no file corresponds to the document
                return;
            }

            FileType fileType = file.getFileType();
            SelectionModel selectionModel = editor.getSelectionModel();
            String localContent = selectionModel.hasSelection()
                    ? selectionModel.getSelectedText()
                    : document.getText();

            DiffContentFactory contentFactory = DiffContentFactory.getInstance();
            SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                    "Code Diff",
                    contentFactory.create(localContent, fileType),
                    contentFactory.create(diffContent, fileType),
                    "Current Code",
                    "New Code"
            );

            DiffManager.getInstance().showDiff(project, diffRequest);
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
