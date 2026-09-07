package com.healthcare.service;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;
import com.healthcare.model.User;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class HealthcareOperationService {
    private DatabaseStore db;

    public HealthcareOperationService() {
        this.db = DatabaseStore.getInstance();
    }

    /**
     * Verifies that the primary actor is a Doctor or Administrator.
     */
    private boolean isAuthorized(User actor) {
        if (actor == null || actor.getRoleLabel() == null) {
            return false;
        }
        String role = actor.getRoleLabel().toUpperCase();
        return role.equals("DOCTOR") || role.equals("ADMIN") || role.equals("ADMINISTRATOR");
    }

    // =========================================================================
    // SCHEDULE SLOT OPERATIONS (CREATE, UPDATE, DELETE)
    // =========================================================================

    public ScheduleSlot addScheduleSlot(User actor, String doctorName, String date, String startTime, String endTime, String mode) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Only Doctors and Administrators can manage consultation slots.");
            return null;
        }

        int maxId = 100;
        for (String id : db.getSlots().keySet()) {
            if (id.startsWith("SLT")) {
                try {
                    int num = Integer.parseInt(id.replaceAll("[^0-9]", ""));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        String slotId = "SLT" + (maxId + 1);
        ScheduleSlot newSlot = new ScheduleSlot(slotId, doctorName, date, startTime, endTime, mode, "SCHEDULED");

        db.getSlots().put(slotId, newSlot);
        db.saveSlots();
        return newSlot;
    }
    
    public List<ScheduleSlot> getAllScheduleSlots(User actor) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Only Doctors and Administrators can retrieve all slots.");
            return List.of();
        }
        return new java.util.ArrayList<>(db.getSlots().values());
    }

    // UPDATED: Added newDoctorName parameter to allow updating doctor assignments
    public boolean updateScheduleSlot(User actor, String slotId, String newDoctorName, String newDate, String newStartTime, String newEndTime, String newMode, String newStatus) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Unauthorized role.");
            return false;
        }

        ScheduleSlot slot = db.getSlots().get(slotId);
        if (slot == null) {
            return false;
        }

        if (newDoctorName != null && !newDoctorName.trim().isEmpty()) {
            slot.setDoctorId(newDoctorName);
        }
        slot.setSlotDate(newDate);
        slot.setStartTime(newStartTime);
        slot.setEndTime(newEndTime);
        slot.setMode(newMode);
        slot.setStatus(newStatus);

        db.saveSlots();
        return true;
    }

    public boolean deleteScheduleSlot(User actor, String slotId) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Unauthorized role.");
            return false;
        }

        boolean hasAppointments = db.getAppointments().values().stream()
                .anyMatch(app -> slotId.equals(app.getSlotId()) && !"CANCELLED".equalsIgnoreCase(app.getStatus()));

        if (hasAppointments) {
            System.out.println("Deletion Rejected: Cannot delete slot with active appointments.");
            return false;
        }

        db.getSlots().remove(slotId);
        db.saveSlots();
        return true;
    }

    public boolean blockScheduleSlot(User actor, String slotId) {
        if (!isAuthorized(actor)) return false;
        
        ScheduleSlot slot = db.getSlots().get(slotId);
        if (slot == null) return false;

        slot.setStatus("BLOCKED");
        db.saveSlots();
        return true;
    }

    // =========================================================================
    // PATIENT QUEUE OPERATIONS
    // =========================================================================

    // Admin & Doctor Queue Access
    public Queue<Appointment> getDoctorQueue(User actor, String doctorIdentifier) {
        Queue<Appointment> queue = new LinkedList<>();
        if (!isAuthorized(actor)) {
            return queue;
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(actor.getRoleLabel()) || "ADMINISTRATOR".equalsIgnoreCase(actor.getRoleLabel());

        List<Appointment> sortedQueue = db.getAppointments().values().stream()
                .filter(app -> {
                    // Admin can filter by target doctor OR retrieve all waiting patients if doctorIdentifier is empty/null
                    if (isAdmin) {
                        if (doctorIdentifier == null || doctorIdentifier.trim().isEmpty()) {
                            return true;
                        }
                        return doctorIdentifier.equalsIgnoreCase(app.getDoctorId());
                    }
                    // Doctor view: match actor's User ID or Full Name against appointment doctor field
                    return actor.getUserId().equalsIgnoreCase(app.getDoctorId()) 
                        || actor.getFullName().equalsIgnoreCase(app.getDoctorId())
                        || (doctorIdentifier != null && doctorIdentifier.equalsIgnoreCase(app.getDoctorId()));
                })
                .filter(app -> "WAITING".equalsIgnoreCase(app.getStatus()) || "CHECKED_IN".equalsIgnoreCase(app.getStatus()))
                .sorted(Comparator.comparing(Appointment::getAppointmentId))
                .collect(Collectors.toList());

        queue.addAll(sortedQueue);
        return queue;
    }

    // Call next patient into consultation (Supports Admin & Doctor execution)
    public Appointment callNextPatient(User actor, String doctorIdentifier) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Unauthorized role.");
            return null;
        }

        Queue<Appointment> queue = getDoctorQueue(actor, doctorIdentifier);
        if (queue.isEmpty()) {
            return null;
        }

        Appointment nextApp = queue.poll();
        nextApp.setStatus("IN_CONSULTATION");
        db.saveAppointments();

        sendAppointmentReminder(nextApp);
        return nextApp;
    }

    public boolean markPatientAbsent(User actor, String appointmentId) {
        if (!isAuthorized(actor)) return false;

        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) return false;

        app.setStatus("CANCELLED");
        db.saveAppointments();
        return true;
    }

    public boolean checkInPatient(User actor, String appointmentId) {
        if (!isAuthorized(actor)) return false;

        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) return false;

        app.setStatus("WAITING");
        db.saveAppointments();
        return true;
    }

    private void sendAppointmentReminder(Appointment app) {
        System.out.println("[REMINDER SENT] Notification sent to Patient: " 
                + app.getPatientId() + " for Appointment ID: " + app.getAppointmentId());
    }
}