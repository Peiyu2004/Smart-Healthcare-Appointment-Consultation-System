package com.healthcare.service;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppointmentService {
    private DatabaseStore db;

    public AppointmentService() {
        this.db = DatabaseStore.getInstance();
    }

    // Retrieve all appointments in the system (for Admin / Staff views)
    public List<Appointment> getAllAppointments() {
        return new ArrayList<>(db.getAppointments().values());
    }

    public List<ScheduleSlot> getAvailableSlots() {
        List<ScheduleSlot> available = new ArrayList<>();
        for (ScheduleSlot slot : db.getSlots().values()) {
            if (slot.isAvailable()) {
                available.add(slot);
            }
        }
        return available;
    }

    public Appointment bookAppointment(String patientId, String doctorId, String slotId, String reason) {
        ScheduleSlot slot = db.getSlots().get(slotId);
        if (slot == null || !slot.isAvailable()) {
            System.out.println("Slot is either invalid or already booked.");
            return null;
        }

        String appointmentId = "APP-" + UUID.randomUUID().toString().substring(0, 8);
        String bookedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Appointment appointment = new Appointment(
            appointmentId, patientId, doctorId, slotId, bookedAt, reason, "SCHEDULED", "NONE", "NONE"
        );

        slot.setStatus("SCHEDULED");
        db.getAppointments().put(appointmentId, appointment);

        db.saveSlots();
        db.saveAppointments();

        return appointment;
    }

    public boolean cancelAppointment(String appointmentId, String reason) {
        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null || "CANCELLED".equalsIgnoreCase(app.getStatus())) {
            return false;
        }

        app.setStatus("CANCELLED");
        app.setCancellationReason(reason);

        ScheduleSlot slot = db.getSlots().get(app.getSlotId());
        if (slot != null) {
            slot.setStatus("SCHEDULED");
            db.saveSlots();
        }

        db.saveAppointments();
        return true;
    }

    public List<Appointment> getAppointmentsByPatient(String patientId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment app : db.getAppointments().values()) {
            if (app.getPatientId().equals(patientId)) {
                result.add(app);
            }
        }
        return result;
    }

    public List<Appointment> getAppointmentsByDoctor(String doctorId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment app : db.getAppointments().values()) {
            if (app.getDoctorId().equals(doctorId)) {
                result.add(app);
            }
        }
        return result;
    }
}