package ai.devchat.devchat;

import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;

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
        String htmlContent = readHTMLFile("/static/main.html");
        if (htmlContent.isEmpty()) {
            htmlContent = "<html><body><h1>Error: main.html not found</h1></body></html>";
        }
        jbCefBrowser.loadHTML(htmlContent);
    }

    public JPanel getContent() {
        return content;
    }

    private String readHTMLFile(String fileName) {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            URL url = getClass().getResource(fileName);
            if (url == null) {
                System.out.println("File not found: " + fileName);
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }
}
