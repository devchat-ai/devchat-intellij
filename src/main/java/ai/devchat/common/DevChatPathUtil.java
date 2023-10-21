package ai.devchat.common;

import com.intellij.openapi.application.PathManager;

public class DevChatPathUtil {
    public static String getWorkPath() {
        return PathManager.getPluginsPath() + "/devchat";
    }

    public static String getDevchatBinPath() {
        return getWorkPath() + "/mamba" + "/envs/devchat/bin/devchat";
    }
}
