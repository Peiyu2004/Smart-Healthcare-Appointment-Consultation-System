package com.healthcare.service;

import java.util.ArrayList;
import java.util.List;

import com.healthcare.model.Notifiable;
import com.healthcare.model.Notification;
import com.healthcare.model.NotificationType;

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

    public boolean sendAppointmentReminder(String patientId, String queueNumber, String doctorFullName, String location) {
        String message = String.format("Ticket #%s: %s is ready for you in %s", queueNumber, doctorFullName, location);
        Notification notification = new Notification(patientId, NotificationType.APPOINTMENT_REMINDER, message);

        boolean delivered = attemptDispatch(notification, "PRIMARY");

        if (!delivered) {
            delivered = attemptDispatch(notification, "FALLBACK");
        }

        notificationHistory.add(notification);

        if (!delivered) {
            System.out.println("[QUEUE ALERT] Notification delivery failed for Patient " + patientId
                    + " (Ticket #" + queueNumber + ") - flagged for clinic staff.");
        }

        return delivered;
    }

    private boolean attemptDispatch(Notification notification, String gatewayLabel) {
        try {
            notifier.send(notification);
            System.out.println("[" + gatewayLabel + " GATEWAY] Delivered at " + notification.getTimestamp());
            return true;
        } catch (Exception e) {
            System.out.println("[" + gatewayLabel + " GATEWAY] Failed at " + notification.getTimestamp());
            return false;
        }
    }

    public List<Notification> getNotificationHistory() {
        return notificationHistory;
    }
}