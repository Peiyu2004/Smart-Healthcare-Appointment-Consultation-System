package appointment.notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private List<Notification> notificationHistory;
    private Notifiable notifier;

    public NotificationService(Notifiable notifier) {
        this.notifier = notifier;
        this.notificationHistory = new ArrayList<>();
    }

    public void notifyUser(String recipientName, NotificationType type, String message) {
        Notification notification = new Notification(recipientName, type, message);
        notificationHistory.add(notification);
        notifier.send(notification);
    }

    public List<Notification> getNotificationHistory() {
        return notificationHistory;
    }
}