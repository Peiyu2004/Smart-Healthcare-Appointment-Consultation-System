package com.healthcare.service;

import com.healthcare.model.Appointment;
import com.healthcare.model.ScheduleSlot;
import com.healthcare.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AppointmentService {
    private DatabaseStore db;

    public AppointmentService() {
        this.db = DatabaseStore.getInstance();
    }

    public List<Appointment> getAllAppointments() {
        return new ArrayList<>(db.getAppointments().values());
    }

    public List<ScheduleSlot> getAvailableSlots() {
        List<ScheduleSlot> available = new ArrayList<>();
        for (ScheduleSlot slot : db.getSlots().values()) {
            if (isSlotAvailable(slot)) {
                available.add(slot);
            }
        }
        return available;
    }

    private boolean isSlotAvailable(ScheduleSlot slot) {
        if (slot == null) return false;
        String status = slot.getStatus() != null ? slot.getStatus().trim() : "";
        return "AVAILABLE".equalsIgnoreCase(status) || slot.isAvailable();
    }

    /**
     * Enhanced Doctor Matching Logic
     * Matches across User IDs (U008), Full Names (Dr. Sarah Jenkins), and raw Names (Sarah Jenkins).
     */
    private boolean matchesDoctor(String val1, String val2) {
        if (val1 == null || val2 == null) return false;

        String s1 = val1.trim();
        String s2 = val2.trim();

        // Direct comparison
        if (s1.equalsIgnoreCase(s2)) return true;

        // Strip "Dr." / "Dr " prefixes and compare
        String clean1 = s1.replaceAll("(?i)^dr\\.\\s*", "").replaceAll("(?i)^dr\\s*", "").trim();
        String clean2 = s2.replaceAll("(?i)^dr\\.\\s*", "").replaceAll("(?i)^dr\\s*", "").trim();
        if (!clean1.isEmpty() && clean1.equalsIgnoreCase(clean2)) return true;

        // Lookup against registered users in DatabaseStore
        for (User u : db.getUsers().values()) {
            String role = u.getRoleLabel() != null ? u.getRoleLabel().toUpperCase() : "";
            if ("DOCTOR".equals(role) || "STAFF".equals(role)) {
                String uId = u.getUserId() != null ? u.getUserId().trim() : "";
                String uName = u.getFullName() != null ? u.getFullName().trim() : "";
                String cleanUName = uName.replaceAll("(?i)^dr\\.\\s*", "").replaceAll("(?i)^dr\\s*", "").trim();

                boolean val1MatchesUser = s1.equalsIgnoreCase(uId) || clean1.equalsIgnoreCase(cleanUName) || s1.equalsIgnoreCase(uName);
                boolean val2MatchesUser = s2.equalsIgnoreCase(uId) || clean2.equalsIgnoreCase(cleanUName) || s2.equalsIgnoreCase(uName);

                if (val1MatchesUser && val2MatchesUser) return true;
            }
        }
        return false;
    }

    private String generateNextAppointmentId() {
        int maxId = 0;
        for (String id : db.getAppointments().keySet()) {
            if (id != null && id.toUpperCase().startsWith("APT")) {
                try {
                    int num = Integer.parseInt(id.replaceAll("[^0-9]", ""));
                    if (num > maxId) maxId = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("APT%03d", maxId + 1);
    }

    public Appointment bookAppointment(String patientId, String doctorId, String slotId, String reason) {
        ScheduleSlot slot = db.getSlots().get(slotId);
        if (slot == null || !isSlotAvailable(slot)) {
            System.out.println("Slot is either invalid or already booked.");
            return null;
        }

        String appointmentId = generateNextAppointmentId();
        String bookedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Determine target doctor reference from slot or input parameter
        String targetDoctorRef = (slot.getDoctorId() != null && !slot.getDoctorId().trim().isEmpty()) 
                                 ? slot.getDoctorId() : doctorId;

        // Resolve Patient details cleanly
        User patientUser = db.getUserById(patientId);
        String actualPatientId = patientUser != null ? patientUser.getUserId() : patientId;
        String patientName = patientUser != null ? patientUser.getFullName() : patientId;

        // Resolve Doctor details cleanly
        User doctorUser = db.getUserById(targetDoctorRef);
        String actualDoctorId = doctorUser != null ? doctorUser.getUserId() : targetDoctorRef;
        String doctorName = doctorUser != null ? doctorUser.getFullName() : targetDoctorRef;

        // Instantiate Appointment with all 11 explicit positional parameters
        Appointment appointment = new Appointment(
            appointmentId, 
            actualPatientId, 
            patientName, 
            actualDoctorId, 
            doctorName,
            slotId, 
            bookedAt, 
            reason, 
            "SCHEDULED", 
            "NONE", 
            "NONE"
        );

        slot.setStatus("SCHEDULED");
        db.getAppointments().put(appointmentId, appointment);

        db.saveSlots();
        db.saveAppointments();

        return appointment;
    }

    public boolean cancelAppointment(String appointmentId, String reason) {
        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) return false;

        String status = app.getStatus() != null ? app.getStatus().toUpperCase() : "";
        if ("CANCELLED".equals(status) || "COMPLETED".equals(status)) {
            return false;
        }

        app.setStatus("CANCELLED");
        app.setCancellationReason(reason);

        ScheduleSlot slot = db.getSlots().get(app.getSlotId());
        if (slot != null) {
            slot.setStatus("AVAILABLE");
            db.saveSlots();
        }

        db.saveAppointments();
        return true;
    }

    public List<Appointment> getAppointmentsByPatient(String patientId) {
        List<Appointment> result = new ArrayList<>();
        if (patientId == null) return result;

        for (Appointment app : db.getAppointments().values()) {
            if (patientId.equalsIgnoreCase(app.getPatientId())) {
                result.add(app);
            }
        }
        return result;
    }

    public List<Appointment> getAppointmentsByDoctor(String doctorIdOrName) {
        List<Appointment> result = new ArrayList<>();
        if (doctorIdOrName == null) return result;

        for (Appointment app : db.getAppointments().values()) {
            // Check against both doctorId and doctorName fields stored on appointment record
            if (matchesDoctor(app.getDoctorId(), doctorIdOrName) || matchesDoctor(app.getDoctorName(), doctorIdOrName)) {
                result.add(app);
            }
        }
        return result;
    }

    public Appointment getAppointmentById(String appointmentId) {
        return db.getAppointments().get(appointmentId);
    }

    public List<ScheduleSlot> getAvailableSlotsByDoctor(String doctorId) {
        List<ScheduleSlot> result = new ArrayList<>();
        if (doctorId == null || doctorId.trim().isEmpty()) {
            return getAvailableSlots();
        }

        for (ScheduleSlot slot : db.getSlots().values()) {
            if (matchesDoctor(slot.getDoctorId(), doctorId) && isSlotAvailable(slot)) {
                result.add(slot);
            }
        }

        if (result.isEmpty()) {
            return getAvailableSlots();
        }

        return result;
    }

    public boolean rescheduleAppointment(String appointmentId, String newSlotId) {
        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) return false;

        String currentStatus = app.getStatus() != null ? app.getStatus().toUpperCase() : "";
        if ("CANCELLED".equals(currentStatus) || "COMPLETED".equals(currentStatus) || "IN_CONSULTATION".equals(currentStatus)) {
            return false;
        }

        ScheduleSlot newSlot = db.getSlots().get(newSlotId);
        if (newSlot == null || !isSlotAvailable(newSlot)) {
            return false;
        }

        ScheduleSlot oldSlot = db.getSlots().get(app.getSlotId());
        if (oldSlot != null) {
            oldSlot.setStatus("AVAILABLE");
        }

        newSlot.setStatus("SCHEDULED");

        app.setSlotId(newSlotId);
        app.setStatus("SCHEDULED");

        db.saveSlots();
        db.saveAppointments();

        return true;
    }

    public ScheduleSlot getSlotById(String slotId) {
        return db.getSlots().get(slotId);
    }
}