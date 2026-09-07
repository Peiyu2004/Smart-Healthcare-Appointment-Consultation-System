package com.healthcare.service;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;

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
        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) {
            System.out.println("Appointment ID not found.");
            return false;
        }

        String formattedStatus = newStatus.trim().toUpperCase();
        app.setStatus(formattedStatus);

        // If completed or cancelled, automatically free up the slot
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

    public String getAppointmentStatus(String appointmentId) {
        Appointment app = db.getAppointments().get(appointmentId);
        return (app != null) ? app.getStatus() : "NOT_FOUND";
    }

    public List<Appointment> getAppointmentsByStatus(String status) {
        return db.getAppointments().values().stream()
                .filter(app -> status.equalsIgnoreCase(app.getStatus()))
                .collect(Collectors.toList());
    }
}