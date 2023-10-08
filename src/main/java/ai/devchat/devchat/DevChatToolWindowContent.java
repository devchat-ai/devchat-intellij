package ai.devchat.devchat;

import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;

import javax.swing.*;
import java.awt.*;

public class DevChatToolWindowContent {

    private final JPanel content;

    public DevChatToolWindowContent() {
        this.content = new JPanel(new BorderLayout());
        if (!JBCefApp.isSupported()) {
            this.content.add(new JLabel("JCEF is not supported", SwingConstants.CENTER));
            return;
        }

        JBCefBrowser jbCefBrowser = new JBCefBrowser();
        this.content.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
        String htmlContent = "<html><body><h1>Hello World!</h1></body></html>";
        jbCefBrowser.loadHTML(htmlContent);
    }

    public JPanel getContent() {
        return content;
    }
}
