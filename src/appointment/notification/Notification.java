package appointment.notification;

import java.time.LocalDateTime;

public class Notification {
    private String recipientName;
    private NotificationType type;
    private String message;
    private LocalDateTime timestamp;

    public Notification(String recipientName, NotificationType type, String message) {
        this.recipientName = recipientName;
        this.type = type;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getRecipientName() {
        return recipientName;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] To: " + recipientName + " | " + type + " | " + message;
    }
}