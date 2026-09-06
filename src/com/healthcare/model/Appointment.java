package com.healthcare.model;

public class Appointment {
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private String slotId;
    private String bookedAt;
    private String reasonForVisit;
    private String status;
    private String checkInTime;
    private String cancellationReason;

    public Appointment(String appointmentId, String patientId, String doctorId, String slotId, String bookedAt, String reasonForVisit, String status, String checkInTime, String cancellationReason) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.slotId = slotId;
        this.bookedAt = bookedAt;
        this.reasonForVisit = reasonForVisit;
        this.status = status;
        this.checkInTime = checkInTime;
        this.cancellationReason = cancellationReason;
    }

    public String getAppointmentId() { return appointmentId; }
    public String getPatientId() { return patientId; }
    public String getDoctorId() { return doctorId; }
    public String getSlotId() { return slotId; }
    public String getBookedAt() { return bookedAt; }
    public String getReasonForVisit() { return reasonForVisit; }
    public String getStatus() { return status; }
    public String getCheckInTime() { return checkInTime; }
    public String getCancellationReason() { return cancellationReason; }

    public void setStatus(String status) { this.status = status; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}