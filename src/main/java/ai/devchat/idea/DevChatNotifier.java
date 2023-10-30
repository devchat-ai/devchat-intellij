package ai.devchat.idea;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class DevChatNotifier {

    public static void notifyInfo(Project project, String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Custom Notification Group")
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }

    public static void notifyError(Project project, String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Custom Notification Group")
                .createNotification(content, NotificationType.ERROR)
                .notify(project);
    }
}
