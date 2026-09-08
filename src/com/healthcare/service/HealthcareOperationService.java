package com.healthcare.service;

import com.healthcare.model.Appointment;
import com.healthcare.model.Consultation;
import com.healthcare.model.ScheduleSlot;
import com.healthcare.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class HealthcareOperationService {
    private DatabaseStore db;
    private NotificationService notificationService;

    public HealthcareOperationService() {
        this.db = DatabaseStore.getInstance();
        this.notificationService = new NotificationService(new com.healthcare.model.ConsoleNotifier()); 
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
    // MANAGE CONSULTATIONS AND PATIENT HISTORY
    // =========================================================================

    /**
     * Retrieves completed clinical history logs for a specific patient by matching patient ID or full name.
     */
    public List<Consultation> getPatientHistory(String patientIdentifier) {
        if (patientIdentifier == null || patientIdentifier.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Retrieve actual patient user object to enable cross-checking ID and Name
        User patient = db.getUsers().get(patientIdentifier);
        if (patient == null) {
            patient = db.getUserByEmail(patientIdentifier);
        }
        final String pId = (patient != null) ? patient.getUserId() : patientIdentifier;
        final String pName = (patient != null) ? patient.getFullName() : patientIdentifier;

        return db.getConsultations().values().stream()
                .filter(c -> pId.equalsIgnoreCase(c.getPatientName()) 
                          || pName.equalsIgnoreCase(c.getPatientName())
                          || patientIdentifier.equalsIgnoreCase(c.getPatientName())
                          || patientIdentifier.equalsIgnoreCase(c.getConsultationId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Retrieves all consultation records for a given patient ID.
     */
    public List<Consultation> getConsultationsForPatient(String patientId) {
        return getPatientHistory(patientId);
    }

    /**
     * Retrieves a consultation entry by its associated appointment ID.
     */
    public Consultation getConsultationByAppointment(String appointmentId) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            return null;
        }
        return db.getConsultations().values().stream()
                .filter(c -> appointmentId.equalsIgnoreCase(c.getAppointmentId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Legacy Overload for backwards compatibility.
     */
    public boolean saveConsultationRecord(User actor, String appointmentId, String clinicalNotes, String diagnosis) {
        return saveConsultationRecord(actor, appointmentId, clinicalNotes, diagnosis, "");
    }

    /**
     * Creates or updates a consultation entry, validates inputs, stores prescription separately,
     * and updates appointment status.
     */
    public boolean saveConsultationRecord(User actor, String appointmentId, String clinicalNotes, String diagnosis, String prescription) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Only Doctors and Administrators can manage consultation entries.");
            return false;
        }

        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) {
            System.err.println("Error: Associated appointment not found.");
            return false;
        }

        // Validate mandatory medical fields
        if (clinicalNotes == null || clinicalNotes.trim().isEmpty() || diagnosis == null || diagnosis.trim().isEmpty()) {
            System.err.println("[VALIDATION ERROR] Mandatory fields (Clinical Notes and Diagnosis) cannot be empty.");
            return false;
        }

        // Search for an existing consultation bound to this appointment
        Consultation consultation = db.getConsultations().values().stream()
                .filter(c -> appointmentId.equalsIgnoreCase(c.getAppointmentId()))
                .findFirst()
                .orElse(null);

        String today = LocalDate.now().toString();
        String safePrescription = (prescription != null) ? prescription.trim() : "";

        if (consultation == null) {
            // Calculate next numerical Consultation ID (C001, C002...)
            int maxId = 0;
            for (String id : db.getConsultations().keySet()) {
                if (id != null && id.toUpperCase().startsWith("C")) {
                    try {
                        int num = Integer.parseInt(id.replaceAll("[^0-9]", ""));
                        if (num > maxId) maxId = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
            String consultationId = String.format("C%03d", maxId + 1);

            // Save patient ID / name and doctor name
            String patientIdentifier = app.getPatientId();
            String doctorName = actor.getFullName();

            consultation = new Consultation(
                    consultationId,
                    appointmentId,
                    patientIdentifier,
                    doctorName,
                    clinicalNotes.trim(),
                    diagnosis.trim(),
                    today
            );
            
            // Set prescription field explicitly on Consultation model if setter exists
            try {
                consultation.setPrescription(safePrescription);
            } catch (Exception ignored) {}

            db.getConsultations().put(consultationId, consultation);
        } else {
            consultation.setClinicalNotes(clinicalNotes.trim());
            consultation.setDiagnosis(diagnosis.trim());
            consultation.setRecordDate(today);
            
            try {
                consultation.setPrescription(safePrescription);
            } catch (Exception ignored) {}
        }

        // Save explicit prescription to DatabaseStore table if available
        if (!safePrescription.isEmpty()) {
            try {
                db.savePrescriptionRecord(consultation.getConsultationId(), app.getPatientId(), actor.getUserId(), safePrescription);
            } catch (Exception ignored) {}
        }

        // Mark appointment as completed upon final submission
        app.setStatus("COMPLETED");

        db.saveAppointments();
        db.saveConsultations();
        db.saveData();
        return true;
    }

    // =========================================================================
    // SCHEDULE SLOT OPERATIONS
    // =========================================================================

    public ScheduleSlot addScheduleSlot(User actor, String doctorName, String date, String startTime, String endTime, String mode) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Only Doctors and Administrators can manage consultation slots.");
            return null;
        }

        int maxId = 0;
        for (String id : db.getSlots().keySet()) {
            if (id != null && id.toUpperCase().startsWith("SLT")) {
                try {
                    int num = Integer.parseInt(id.replaceAll("[^0-9]", ""));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        String slotId = String.format("SLT%03d", maxId + 1);
        ScheduleSlot newSlot = new ScheduleSlot(slotId, doctorName, date, startTime, endTime, mode, "AVAILABLE");

        db.getSlots().put(slotId, newSlot);
        db.saveSlots();
        db.saveData(); // Synchronize all data stores
        return newSlot;
    }
    
    public List<ScheduleSlot> getAllScheduleSlots(User actor) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Only Doctors and Administrators can retrieve all slots.");
            return List.of();
        }
        return new ArrayList<>(db.getSlots().values());
    }

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

    public Queue<Appointment> getDoctorQueue(User actor, String doctorIdentifier) {
        Queue<Appointment> queue = new LinkedList<>();
        if (!isAuthorized(actor)) {
            return queue;
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(actor.getRoleLabel()) || "ADMINISTRATOR".equalsIgnoreCase(actor.getRoleLabel());

        List<Appointment> sortedQueue = db.getAppointments().values().stream()
                .filter(app -> {
                    if (isAdmin) {
                        if (doctorIdentifier == null || doctorIdentifier.trim().isEmpty()) {
                            return true;
                        }
                        return doctorIdentifier.equalsIgnoreCase(app.getDoctorId());
                    }
                    return actor.getUserId().equalsIgnoreCase(app.getDoctorId()) 
                        || actor.getFullName().equalsIgnoreCase(app.getDoctorId())
                        || (doctorIdentifier != null && doctorIdentifier.equalsIgnoreCase(app.getDoctorId()));
                })
                .filter(app -> {
                    String st = app.getStatus() != null ? app.getStatus().toUpperCase() : "";
                    return "WAITING".equals(st);
                })
                .sorted(Comparator.comparing(Appointment::getAppointmentId))
                .collect(Collectors.toList());

        queue.addAll(sortedQueue);
        return queue;
    }

    public Appointment callNextPatient(User actor, String doctorIdentifier) {
        if (!isAuthorized(actor)) {
            System.err.println("Access Denied: Unauthorized role.");
            return null;
        }

        Queue<Appointment> queue = getDoctorQueue(actor, doctorIdentifier);
        
        Appointment nextApp = null;
        for (Appointment app : queue) {
            String st = app.getStatus() != null ? app.getStatus().toUpperCase() : "";
            if ("WAITING".equals(st)) {
                nextApp = app;
                break;
            }
        }

        if (nextApp == null) {
            return null;
        }

        nextApp.setStatus("IN_CONSULTATION");
        db.saveAppointments();
        db.saveData();

        sendAppointmentReminder(nextApp);
        return nextApp;
    }

    public boolean markPatientAbsent(User actor, String appointmentId) {
        if (!isAuthorized(actor)) return false;

        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) return false;

        app.setStatus("CANCELLED");
        db.saveAppointments();
        db.saveData();
        return true;
    }

    public boolean checkInPatient(User actor, String appointmentId) {
        if (!isAuthorized(actor)) return false;

        Appointment app = db.getAppointments().get(appointmentId);
        if (app == null) return false;

        app.setStatus("WAITING");
        db.saveAppointments();
        db.saveData();
        return true;
    }

    private void sendAppointmentReminder(Appointment app) {
        // Fetch doctor information
        User doctor = db.getUsers().get(app.getDoctorId());
        if (doctor == null) {
            doctor = db.getUserByEmail(app.getDoctorId());
        }
        String doctorFullName = (doctor != null) ? doctor.getFullName() : app.getDoctorId();

        // Fetch patient information to get patient name
        User patient = db.getUsers().get(app.getPatientId());
        if (patient == null) {
            patient = db.getUserByEmail(app.getPatientId());
        }
        String patientName = (patient != null) ? patient.getFullName() : app.getPatientId();

        String location = "the Consultation Room";

        // Pass patient name along with doctor full name to the notification service
        notificationService.sendAppointmentReminder(
                app.getPatientId(),
                patientName,
                app.getAppointmentId(),
                doctorFullName,
                location
        );
    }
}