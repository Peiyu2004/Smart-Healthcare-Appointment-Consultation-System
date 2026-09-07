package com.healthcare.service;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StatusTrackingService {
    private DatabaseStore db;

    public StatusTrackingService() {
        this.db = DatabaseStore.getInstance();
    }

    /**
     * Updates an appointment's status across valid states:
     * SCHEDULED, WAITING, IN_CONSULTATION, COMPLETED, CANCELLED
     */
    public boolean updateAppointmentStatus(String appointmentId, String newStatus) {
        if (appointmentId == null || newStatus == null) {
            return false;
        }

        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) {
            System.err.println("Error: Appointment ID not found.");
            return false;
        }

        String formattedStatus = newStatus.trim().toUpperCase();
        app.setStatus(formattedStatus);

        // If completed or cancelled, automatically free up the associated slot
        if ("COMPLETED".equals(formattedStatus) || "CANCELLED".equals(formattedStatus)) {
            ScheduleSlot slot = db.getSlots().get(app.getSlotId());
            if (slot != null) {
                slot.setStatus("AVAILABLE");
                db.saveSlots();
            }
        }

        db.saveAppointments();
        return true;
    }

    /**
     * Retrieves current status for a given appointment.
     */
    public String getAppointmentStatus(String appointmentId) {
        Appointment app = db.getAppointments().get(appointmentId);
        return (app != null) ? app.getStatus() : "NOT_FOUND";
    }

    /**
     * Retrieves specific appointment details by appointment ID.
     */
    public Appointment getAppointmentDetails(String appointmentId) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            return null;
        }
        return db.getAppointments().get(appointmentId);
    }

    /**
     * Filters appointments matching a given status label.
     */
    public List<Appointment> getAppointmentsByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return db.getAppointments().values().stream()
                .filter(app -> status.equalsIgnoreCase(app.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves appointments filtered by Doctor Name / ID.
     * If doctorIdentifier is empty/null, returns all appointments (for Admin view).
     */
    public List<Appointment> getDoctorAppointments(String doctorIdentifier) {
        if (doctorIdentifier == null || doctorIdentifier.trim().isEmpty()) {
            return new ArrayList<>(db.getAppointments().values());
        }
        return db.getAppointments().values().stream()
                .filter(app -> doctorIdentifier.equalsIgnoreCase(app.getDoctorId()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves appointments for a specific Patient ID / Name.
     */
    public List<Appointment> getPatientAppointments(String patientIdentifier) {
        if (patientIdentifier == null || patientIdentifier.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return db.getAppointments().values().stream()
                .filter(app -> patientIdentifier.equalsIgnoreCase(app.getPatientId()))
                .collect(Collectors.toList());
    }
}