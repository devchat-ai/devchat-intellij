package ai.devchat.devchat;

import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;

import java.util.function.BiConsumer;

/**
 * DevChatActionHandler class uses singleton pattern.
 */
public class DevChatActionHandler {
    private static DevChatActionHandler instance;
    private CefBrowser cefBrowser;
    private Project project;

    private DevChatActionHandler() {

    }

    public static synchronized DevChatActionHandler getInstance() {
        if (instance == null) {
            instance = new DevChatActionHandler();
        }
        return instance;
    }

    public void initialize(CefBrowser cefBrowser, Project project) {
        this.cefBrowser = cefBrowser;
        this.project = project;
    }

    public void sendResponse(String action, String responseFunc, BiConsumer<JSONObject, JSONObject> callback) {
        JSONObject response = new JSONObject();
        response.put("action", action);

        JSONObject metadata = new JSONObject();
        JSONObject payload = new JSONObject();

        response.put("metadata", metadata);
        response.put("payload", payload);

        callback.accept(metadata, payload);

        cefBrowser.executeJavaScript(responseFunc + "(" + response.toString() + ")", "", 0);
    }

    public Project getProject() {
        return project;
    }
}
