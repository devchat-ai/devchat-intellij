package ai.devchat.idea;

import ai.devchat.common.Log;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AddToDevChatFileAction extends AnAction {

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

        if (virtualFile != null && !virtualFile.isDirectory()) {
            Log.info("File path: " + virtualFile.getPath());
            try {
                byte[] bytes = virtualFile.contentsToByteArray();
                String content = new String(bytes, StandardCharsets.UTF_8);
                Log.info("Content: " + content);

            } catch (IOException ex) {
                // handle the exception
            }
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
