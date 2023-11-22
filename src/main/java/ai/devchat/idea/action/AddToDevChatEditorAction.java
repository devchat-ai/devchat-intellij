package ai.devchat.idea.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AddToDevChatEditorAction extends AnAction {
    private AddToDevChatAction addToDevChatAction;

    public AddToDevChatEditorAction() {
        addToDevChatAction = new AddToDevChatAction();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(true);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        Project project = e.getProject();
        String projectPath = project.getBasePath();
        String absoluteFilePath = virtualFile.getPath();
        String relativePath = absoluteFilePath;
        if (projectPath != null && absoluteFilePath.startsWith(projectPath)) {
            relativePath = absoluteFilePath.substring(projectPath.length() + 1);
        }

        FileType fileType = virtualFile.getFileType();
        String language = fileType.getName();

        if (editor != null) {
            final SelectionModel selectionModel = editor.getSelectionModel();
            String selectedText = selectionModel.getSelectedText();
            if (selectedText == null || selectedText.isEmpty()) {
                Document document = editor.getDocument();
                selectedText = document.getText();
            }

            int startOffset = selectionModel.getSelectionStart();
            Document document = editor.getDocument();
            int startLine = document.getLineNumber(startOffset) + 1;

            addToDevChatAction.execute(relativePath, selectedText, language, startLine);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
