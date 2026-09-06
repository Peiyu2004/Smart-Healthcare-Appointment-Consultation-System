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

    public List<Notification> getNotificationHistory() {
        return notificationHistory;
    }
}