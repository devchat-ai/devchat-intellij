package ai.devchat.idea;

import ai.devchat.cli.DevChatConfig;
import ai.devchat.cli.DevChatInstallationManager;
import ai.devchat.common.DevChatPathUtil;
import ai.devchat.common.Log;
import ai.devchat.idea.balloon.DevChatNotifier;
import com.intellij.openapi.project.Project;

public class DevChatThread extends Thread {
    private Project project;

    public DevChatThread(Project project) {
        this.project = project;
    }

    @Override
    public void run() {
        String workPath = DevChatPathUtil.getWorkPath();
        Log.info("Work path is: " + workPath);

        DevChatNotifier.notifyInfo(project, "Starting DevChat initialization...");

        try {
            DevChatInstallationManager dim = new DevChatInstallationManager(workPath, "0.2.9");
            dim.setup();

            DevChatConfig config = new DevChatConfig();
            config.writeDefaultConfig();

            DevChatNotifier.notifyInfo(project, "DevChat initialization has completed successfully.");

        } catch (Exception e) {
            Log.error("Failed to install DevChat CLI: " + e.getMessage());

            DevChatNotifier.notifyError(project,
                    "DevChat initialization has failed. Please check the logs for more details.");
        }
    }
}
