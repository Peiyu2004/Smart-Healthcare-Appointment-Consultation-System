package com.healthcare.service;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;
import com.healthcare.model.User;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
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
    // 3.1 SCHEDULE SLOT OPERATIONS (CREATE, UPDATE, DELETE)
    // =========================================================================

    // Flow 3.1.1: Create consultation slot with ordered ID (SLT112, SLT113, etc.)
    public ScheduleSlot addScheduleSlot(User actor, String doctorName, String date, String startTime, String endTime, String mode) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Only Doctors and Administrators can manage consultation slots.");
            return null;
        }

        // Find the max existing numeric ID to generate the next sequential ID
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
        // Uses doctorName directly and defaults to SCHEDULED status
        ScheduleSlot newSlot = new ScheduleSlot(slotId, doctorName, date, startTime, endTime, mode, "SCHEDULED");

        db.getSlots().put(slotId, newSlot);
        db.saveSlots();
        return newSlot;
    }
    
    // View all consultation slots
    public List<ScheduleSlot> getAllScheduleSlots(User actor) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Only Doctors and Administrators can retrieve all slots.");
            return List.of();
        }
        return new java.util.ArrayList<>(db.getSlots().values());
    }

    // Flow 3.1.2: Update consultation slot
    public boolean updateScheduleSlot(User actor, String slotId, String newDate, String newStartTime, String newEndTime, String newMode, String newStatus) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Unauthorized role.");
            return false;
        }

        ScheduleSlot slot = db.getSlots().get(slotId);
        if (slot == null) {
            return false;
        }

        slot.setSlotDate(newDate);
        slot.setStartTime(newStartTime);
        slot.setEndTime(newEndTime);
        slot.setMode(newMode);
        slot.setStatus(newStatus);

        db.saveSlots();
        return true;
    }

    // Flow 3.1.3: Delete consultation slot
    public boolean deleteScheduleSlot(User actor, String slotId) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Unauthorized role.");
            return false;
        }

        // Flow 3.1.3.3: Check if there are any appointments assigned to this slot
        boolean hasAppointments = db.getAppointments().values().stream()
                .anyMatch(app -> slotId.equals(app.getSlotId()) && !"CANCELLED".equalsIgnoreCase(app.getStatus()));

        if (hasAppointments) {
            System.out.println("Deletion Rejected: Cannot delete slot with active appointments.");
            return false; // Flow 3.1.3.3.1
        }

        // Flow 3.1.3.2: Delete slot if no appointments exist
        db.getSlots().remove(slotId);
        db.saveSlots();
        return true;
    }

    // Flow 3.3.1: Block schedule slot
    public boolean blockScheduleSlot(User actor, String slotId) {
        if (!isAuthorized(actor)) return false;
        
        ScheduleSlot slot = db.getSlots().get(slotId);
        if (slot == null) return false;

        slot.setStatus("BLOCKED");
        db.saveSlots();
        return true;
    }

    // =========================================================================
    // 3.2 & FLOW 5: PATIENT QUEUE OPERATIONS
    // =========================================================================

    // Flow 5: Sort active patient appointments by scheduled time slot
    public Queue<Appointment> getDoctorQueue(User actor, String doctorId) {
        Queue<Appointment> queue = new LinkedList<>();
        if (!isAuthorized(actor)) {
            return queue;
        }

        List<Appointment> sortedQueue = db.getAppointments().values().stream()
                .filter(app -> doctorId.equals(app.getDoctorId()))
                .filter(app -> "WAITING".equalsIgnoreCase(app.getStatus()) || "SCHEDULED".equalsIgnoreCase(app.getStatus()))
                .sorted(Comparator.comparing(Appointment::getAppointmentId)) // Sorts sequence
                .collect(Collectors.toList());

        queue.addAll(sortedQueue);
        return queue;
    }

    // Flow 3.2.1 & Flow 6: Call patient into consultation
    public Appointment callNextPatient(User actor, String doctorId) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Unauthorized role.");
            return null;
        }

        Queue<Appointment> queue = getDoctorQueue(actor, doctorId);
        if (queue.isEmpty()) {
            return null;
        }

        Appointment nextApp = queue.poll();
        nextApp.setStatus("IN_CONSULTATION");
        db.saveAppointments();

        // Flow 3.2.2 & Flow 7: Included Use Case Execution
        sendAppointmentReminder(nextApp);

        return nextApp;
    }

    // Flow 3.3: Mark patient absent
    public boolean markPatientAbsent(User actor, String appointmentId) {
        if (!isAuthorized(actor)) return false;

        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) return false;

        app.setStatus("CANCELLED");
        db.saveAppointments();
        return true;
    }

    // Check-in helper
    public boolean checkInPatient(User actor, String appointmentId) {
        if (!isAuthorized(actor)) return false;

        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) return false;

        app.setStatus("WAITING");
        db.saveAppointments();
        return true;
    }

    // Included Use Case: Send Appointment Reminder
    private void sendAppointmentReminder(Appointment app) {
        System.out.println("[REMINDER SENT] Notification sent to Patient ID: " 
                + app.getPatientId() + " for Appointment ID: " + app.getAppointmentId());
    }
}