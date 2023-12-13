package ai.devchat.common

object DevChatPathUtil {
    @JvmStatic
    val workPath: String
        get() = System.getProperty("user.home") + "/.chat"
    val devchatBinPath: String
        get() = "$workPath/mamba/envs/devchat/bin/devchat"
}
