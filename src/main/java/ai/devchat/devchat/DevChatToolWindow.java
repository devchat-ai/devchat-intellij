package ai.devchat.devchat;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class DevChatToolWindow implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DevChatToolWindowContent toolWindowContent = new DevChatToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class DevChatToolWindowContent {
        private final JPanel contentPanel = new JPanel();

        public DevChatToolWindowContent(ToolWindow toolWindow) {
            contentPanel.setLayout(new BorderLayout(0, 20));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
            contentPanel.add(createDevChatPanel(), BorderLayout.PAGE_START);
        }

        @NotNull
        private JPanel createDevChatPanel() {
            JPanel devchatPanel = new JPanel();
            return devchatPanel;
        }


        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}
