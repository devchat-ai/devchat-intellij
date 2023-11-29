package ai.devchat.common

object DevChatPathUtil {
    val workPath: String
        get() =//        return PathManager.getPluginsPath() + "/devchat";
//        return System.getProperty("user.home") + "/.chat";

            // TODO: change this to the .chat after testing
            System.getProperty("user.home") + "/.chat-intellij"
    val devchatBinPath: String
        get() = workPath + "/mamba" + "/envs/devchat/bin/devchat"
}
