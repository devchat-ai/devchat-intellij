package ai.devchat.idea;

import ai.devchat.cli.DevChatConfig;
import ai.devchat.cli.DevChatInstallationManager;
import ai.devchat.common.DevChatPathUtil;
import ai.devchat.common.Log;

public class DevChatThread extends Thread {
    @Override
    public void run() {
        String workPath = DevChatPathUtil.getWorkPath();
        Log.info("Work path is: " + workPath);

        DevChatInstallationManager dim = new DevChatInstallationManager(workPath, "0.2.9");
        dim.setup();

        DevChatConfig config = new DevChatConfig();
        config.writeDefaultConfig();
    }
}
