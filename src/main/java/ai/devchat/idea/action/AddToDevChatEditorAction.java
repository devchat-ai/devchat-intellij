package ai.devchat.idea.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AddToDevChatEditorAction extends AnAction {
    private AddToDevChatAction addToDevChatAction;

    public AddToDevChatEditorAction() {
        addToDevChatAction = new AddToDevChatAction();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        SelectionModel selectionModel = editor.getSelectionModel();
        e.getPresentation().setEnabled(selectionModel.hasSelection());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            final SelectionModel selectionModel = editor.getSelectionModel();
            final String selectedText = selectionModel.getSelectedText();
            addToDevChatAction.execute(virtualFile.getPath(), selectedText);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
