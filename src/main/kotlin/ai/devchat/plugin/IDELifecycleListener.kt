package ai.devchat.plugin

import ai.devchat.core.DevChatWrapper
import com.intellij.ide.AppLifecycleListener

class IDELifecycleListener: AppLifecycleListener {
    override fun appWillBeClosed(isRestart: Boolean) {
        super.appWillBeClosed(isRestart)
        DevChatWrapper.activeChannel?.close()
    }
}