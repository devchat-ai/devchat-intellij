package ai.devchat.devchat;

import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;

public class DevChatToolWindowContent {

    private final JPanel content;

    public DevChatToolWindowContent() {
        this.content = new JPanel(new BorderLayout());
        // Check if JCEF is supported
        if (!JBCefApp.isSupported()) {
            this.content.add(new JLabel("JCEF is not supported", SwingConstants.CENTER));
            return;
        }

        JBCefBrowser jbCefBrowser = new JBCefBrowser();
        this.content.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);

        // Read static files
        String htmlContent = readStaticFile("/static/main.html");
        if (htmlContent.isEmpty()) {
            htmlContent = "<html><body><h1>Error: main.html not found</h1></body></html>";
        }
        String jsContent = readStaticFile("/static/main.js");
        if (jsContent.isEmpty()) {
            jsContent = "console.log('Error: main.js not found')";
        }

        String HtmlWithJsContent = insertJStoHTML(htmlContent, jsContent);

        // enable dev tools
        CefBrowser myDevTools = jbCefBrowser.getCefBrowser().getDevTools();
        JBCefBrowser myDevToolsBrowser = JBCefBrowser.createBuilder()
                .setCefBrowser(myDevTools)
                .setClient(jbCefBrowser.getJBCefClient())
                .build();

        // initialize JSJavaBridge
        JSJavaBridge jsJavaBridge = new JSJavaBridge(jbCefBrowser);
        jsJavaBridge.registerToBrowser();

        jbCefBrowser.loadHTML(HtmlWithJsContent);
    }

    public JPanel getContent() {
        return content;
    }

    private String readStaticFile(String fileName) {
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

    private String insertJStoHTML(String html, String js) {
        int index = html.lastIndexOf("<script>");
        int endIndex = html.lastIndexOf("</script>");
        if (index != -1 && endIndex != -1) {
            // 除了js内容，我们还要添加<script>标签
            // 所以js内容的开始要+ "<script>".length()
            html = html.substring(0, index + "<script>".length()) + "\n" + js + html.substring(endIndex);
        }
        return html;
    }

}
