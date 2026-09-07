package com.healthcare.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Appointment {
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String slotId;
    private String bookedAt;
    private String reasonForVisit;
    private String status;
    private String checkInTime;
    private String cancellationReason;

    // Full constructor with patient and doctor names
    public Appointment(String appointmentId, String patientId, String patientName, String doctorId, String doctorName, String slotId, String bookedAt, String reasonForVisit, String status, String checkInTime, String cancellationReason) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.slotId = slotId;
        this.bookedAt = bookedAt;
        this.reasonForVisit = reasonForVisit;
        this.status = status;
        this.checkInTime = checkInTime;
        this.cancellationReason = cancellationReason;
    }

    // Overloaded constructor for backward compatibility with existing code
    public Appointment(String appointmentId, String patientId, String doctorId, String slotId, String bookedAt, String reasonForVisit, String status, String checkInTime, String cancellationReason) {
        this(appointmentId, patientId, patientId, doctorId, doctorId, slotId, bookedAt, reasonForVisit, status, checkInTime, cancellationReason);
    }

    public String getAppointmentId() { return appointmentId; }
    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName != null ? patientName : patientId; }
    public String getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName != null ? doctorName : doctorId; }
    public String getSlotId() { return slotId; }
    public String getBookedAt() { return bookedAt; }
    public String getReasonForVisit() { return reasonForVisit; }
    public String getStatus() { return status; }
    public String getCheckInTime() { return checkInTime; }
    public String getCancellationReason() { return cancellationReason; }

    public void setStatus(String status) { this.status = status; }
    public void setSlotId(String slotId) { this.slotId = slotId; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    /**
     * Parses bookedAt timestamp string into LocalDateTime.
     * Compatible with DailyAppointmentReport calling a.getDate().toLocalDate()
     */
    public LocalDateTime getDate() {
        if (bookedAt == null || bookedAt.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(bookedAt, formatter);
        } catch (Exception e) {
            // Fallback for date-only formats like YYYY-MM-DD
            try {
                return LocalDateTime.parse(bookedAt + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }
}