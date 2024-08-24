package ai.devchat.common

import ai.devchat.common.Constants.ASSISTANT_NAME_ZH
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications

object Notifier {
    fun error(content: String) {
        val notification = Notification(
            "Custom Notification Group", ASSISTANT_NAME_ZH, content, NotificationType.ERROR
        )
        Notifications.Bus.notify(notification)
    }
    fun info(content: String) {
        val notification = Notification(
            "Custom Notification Group", ASSISTANT_NAME_ZH, content, NotificationType.INFORMATION
        )
        Notifications.Bus.notify(notification)
    }

    fun stickyError(content: String) {
        val notification = Notification(
            "stickyBalloon", ASSISTANT_NAME_ZH, content, NotificationType.ERROR
        )
        Notifications.Bus.notify(notification)
    }
}
