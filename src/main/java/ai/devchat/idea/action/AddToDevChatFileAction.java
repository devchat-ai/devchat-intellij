package ai.devchat.idea.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AddToDevChatFileAction extends AnAction {

    private AddToDevChatAction addToDevChatAction;

    public AddToDevChatFileAction() {
        addToDevChatAction = new AddToDevChatAction();
    }

    public void update(@NotNull AnActionEvent e) {
        final DataContext context = e.getDataContext();
        final VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(context);
        boolean enabled = virtualFile != null && !virtualFile.isDirectory() && virtualFile.exists();
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final DataContext context = e.getDataContext();
        final VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(context);

        Project project = e.getProject();
        String projectPath = project.getBasePath();
        String absoluteFilePath = virtualFile.getPath();
        String relativePath = absoluteFilePath;
        if (projectPath != null && absoluteFilePath.startsWith(projectPath)) {
            relativePath = absoluteFilePath.substring(projectPath.length() + 1);
        }

        FileType fileType = virtualFile.getFileType();
        String language = fileType.getName();

        if (virtualFile != null && !virtualFile.isDirectory()) {
            try {
                byte[] bytes = virtualFile.contentsToByteArray();
                String content = new String(bytes, StandardCharsets.UTF_8);
                addToDevChatAction.execute(relativePath, content, language, 0);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            throw new RuntimeException("invalid virtualFile.");
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
