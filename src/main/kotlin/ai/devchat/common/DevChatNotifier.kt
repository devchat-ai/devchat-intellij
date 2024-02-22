package ai.devchat.common

import com.intellij.notification.NotificationType
import com.intellij.notification.Notification
import com.intellij.notification.Notifications

object DevChatNotifier {
    fun error(content: String) {
        val notification = Notification(
            "Custom Notification Group", "DevChat", content, NotificationType.ERROR
        )
        Notifications.Bus.notify(notification)
    }
    fun info(content: String) {
        val notification = Notification(
            "Custom Notification Group", "DevChat", content, NotificationType.INFORMATION
        )
        Notifications.Bus.notify(notification)
    }

    fun stickyError(content: String) {
        val notification = Notification(
            "stickyBalloon", "DevChat error", content, NotificationType.ERROR
        )
        Notifications.Bus.notify(notification)
    }
}
