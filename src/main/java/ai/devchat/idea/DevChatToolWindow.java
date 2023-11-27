package ai.devchat.idea;

import ai.devchat.common.Log;
import ai.devchat.devchat.DevChatActionHandler;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColors;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class DevChatToolWindow implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(
                new DevChatToolWindowContent(project).getContent(),
                "",
                false);
        contentManager.addContent(content);

        DevChatSetupThread devChatThread = new DevChatSetupThread(project);
        devChatThread.start();
    }
}

class DevChatToolWindowContent {

    private final JPanel content;
    private Project project;

    public DevChatToolWindowContent(Project project) {
        Log.setLevelInfo();
        this.project = project;
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


        String HtmlWithCssContent = insertCSSToHTML(htmlContent);

        String HtmlWithJsContent = insertJStoHTML(HtmlWithCssContent, jsContent);
        Log.info("main.html and main.js are loaded.");

        // enable dev tools
        CefBrowser myDevTools = jbCefBrowser.getCefBrowser().getDevTools();
        JBCefBrowser.createBuilder()
                .setCefBrowser(myDevTools)
                .setClient(jbCefBrowser.getJBCefClient())
                .build();

        // initialize DevChatActionHandler
        CefBrowser cefBrowser = jbCefBrowser.getCefBrowser();
        DevChatActionHandler handler = DevChatActionHandler.getInstance();
        handler.initialize(cefBrowser, project);

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
            html = html.substring(0, index + "<script>".length()) + "\n" + js + html.substring(endIndex);
        }
        return html;
    }

    private String insertCSSToHTML(String html){
        int index = html.lastIndexOf("<head>");
        int endIndex = html.lastIndexOf("</head>");
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Color editorBgColor = scheme.getColor(EditorColors.CARET_ROW_COLOR);
        Color foregroundColor = scheme.getDefaultForeground();

        String styleTag = "<style>" + ":root{" +
                "--vscode-sideBar-background:"+colorToCssRgb(editorBgColor) + ";" +
                "--vscode-menu-background:"+colorToCssRgb(editorBgColor) + ";" +
                "--vscode-editor-foreground:"+colorToCssRgb(foregroundColor) + ";" +
                "--vscode-menu-foreground:"+colorToCssRgb(foregroundColor) + ";" +
                "--vscode-foreground:"+colorToCssRgb(foregroundColor) + ";" +
                "}" + "</style>";
        if(index != -1 && endIndex != -1){
            html = html.substring(0,index + "<head>".length()) + "\n" + styleTag +  html.substring(endIndex);
        }
        return html;
    }

    public String colorToCssRgb(Color color) {
        if(color != null) return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
        return "";
    }


}
