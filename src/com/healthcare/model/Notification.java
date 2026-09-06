package com.healthcare.model;

public class Notification {
    private String notificationId;
    private String userId;
    private String type;
    private String message;
    private String channel;
    private String createdAt;
    private String sentAt;
    private String status;

    public Notification(String notificationId, String userId, String type, String message, String channel, String createdAt, String sentAt, String status) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.channel = channel;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.status = status;
    }

    public String getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getChannel() { return channel; }
    public String getCreatedAt() { return createdAt; }
    public String getSentAt() { return sentAt; }
    public String getStatus() { return status; }
}