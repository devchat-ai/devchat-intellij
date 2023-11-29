package ai.devchat.idea.balloon

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object DevChatNotifier {
    @JvmStatic
    fun notifyInfo(project: Project?, content: String?) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Custom Notification Group")
            .createNotification(content!!, NotificationType.INFORMATION)
            .notify(project)
    }

    @JvmStatic
    fun notifyError(project: Project?, content: String?) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Custom Notification Group")
            .createNotification(content!!, NotificationType.ERROR)
            .notify(project)
    }
}
