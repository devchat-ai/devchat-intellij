package ai.devchat.common;

public class DevChatPathUtil {
    public static String getWorkPath() {
//        return PathManager.getPluginsPath() + "/devchat";
//        return System.getProperty("user.home") + "/.chat";

        // TODO: change this to the .chat after testing
        return System.getProperty("user.home") + "/.chat-intellij";
    }

    public static String getDevchatBinPath() {
        return getWorkPath() + "/mamba" + "/envs/devchat/bin/devchat";
    }
}
