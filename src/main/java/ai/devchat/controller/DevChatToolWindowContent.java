package ai.devchat.controller;

import ai.devchat.cli.DevChat;
import ai.devchat.exception.DevChatSetupException;
import ai.devchat.util.Log;
import com.intellij.openapi.application.PathManager;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;

public class DevChatToolWindowContent {

    private final JPanel content;

    public DevChatToolWindowContent() {
        Log.setLevelInfo();
        this.content = new JPanel(new BorderLayout());
        // Check if JCEF is supported
        if (!JBCefApp.isSupported()) {
            Log.error("JCEF is not supported.");
            this.content.add(new JLabel("JCEF is not supported", SwingConstants.CENTER));
            return;
        }

        JBCefBrowser jbCefBrowser = new JBCefBrowser();
        this.content.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);

        // Read static files
        String htmlContent = readStaticFile("/static/main.html");
        if (htmlContent.isEmpty()) {
            Log.error("main.html is missing.");
            htmlContent = "<html><body><h1>Error: main.html is missing.</h1></body></html>";
        }
        String jsContent = readStaticFile("/static/main.js");
        if (jsContent.isEmpty()) {
            Log.error("main.js is missing.");
            jsContent = "console.log('Error: main.js not found')";
        }

        String HtmlWithJsContent = insertJStoHTML(htmlContent, jsContent);
        Log.info("main.html and main.js are loaded.");

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

        String workPath = PathManager.getPluginsPath()+"/devchat";
        Log.info("Work path is: " + workPath);

        try {
            DevChat devchat = new DevChat(workPath);
            devchat.setup();
        } catch (DevChatSetupException e){
            e.printStackTrace();
        }
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
            html = html.substring(0, index + "<script>".length()) + "\n" + js + html.substring(endIndex);
        }
        return html;
    }
}
