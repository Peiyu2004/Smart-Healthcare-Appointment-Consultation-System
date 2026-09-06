package com.healthcare.model;

public class QueueTicket {
    private String ticketId;
    private String appointmentId;
    private int queueNumber;
    private String issuedAt;
    private String checkedInAt;
    private String priority;
    private String status;

    public QueueTicket(String ticketId, String appointmentId, int queueNumber, String issuedAt, String checkedInAt, String priority, String status) {
        this.ticketId = ticketId;
        this.appointmentId = appointmentId;
        this.queueNumber = queueNumber;
        this.issuedAt = issuedAt;
        this.checkedInAt = checkedInAt;
        this.priority = priority;
        this.status = status;
    }

    public String getTicketId() { return ticketId; }
    public String getAppointmentId() { return appointmentId; }
    public int getQueueNumber() { return queueNumber; }
    public String getIssuedAt() { return issuedAt; }
    public String getCheckedInAt() { return checkedInAt; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
}