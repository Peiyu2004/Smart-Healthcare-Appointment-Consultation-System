package com.healthcare.service;

import com.healthcare.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class DatabaseStore {
    private static DatabaseStore instance;

    private Map<String, User> users = new HashMap<>();
    private Map<String, Role> roles = new HashMap<>();
    private Map<String, Permission> permissions = new HashMap<>();
    private Map<String, ScheduleSlot> slots = new HashMap<>();
    private Map<String, Appointment> appointments = new HashMap<>();
    private Map<String, QueueTicket> queueTickets = new HashMap<>();
    private Map<String, Consultation> consultations = new HashMap<>();
    private Map<String, Notification> notifications = new HashMap<>();

    private DatabaseStore() {
        loadPermissions();
        loadRoles();
        loadUsers();
        loadSlots();
        loadAppointments();
        loadQueueTickets();
        loadConsultations();
        loadNotifications();
    }

    public static synchronized DatabaseStore getInstance() {
        if (instance == null) {
            instance = new DatabaseStore();
        }
        return instance;
    }

    // ==================== LOAD METHODS ====================

    private void loadPermissions() {
        File file = new File("permissions.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                permissions.put(p[0], new Permission(p[0], p[1], p[2], p[3]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadRoles() {
        File file = new File("roles.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                Role role = new Role(p[0], p[1], p[2]);
                permissions.values().forEach(role::addPermission);
                roles.put(p[0], role);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        File file = new File("users.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                String type = p[0];
                User user = null;
                if (type.equals("PATIENT")) {
                    user = new Patient(p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10]);
                    roles.values().stream().filter(r -> r.getRoleName().equals("PATIENT")).findFirst().ifPresent(user.getRoles()::add);
                } else if (type.equals("DOCTOR")) {
                    user = new Doctor(p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], Double.parseDouble(p[10]));
                    roles.values().stream().filter(r -> r.getRoleName().equals("DOCTOR")).findFirst().ifPresent(user.getRoles()::add);
                } else if (type.equals("ADMIN")) {
                    user = new Administrator(p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9]);
                    roles.values().stream().filter(r -> r.getRoleName().equals("ADMIN")).findFirst().ifPresent(user.getRoles()::add);
                }
                if (user != null) {
                    users.put(user.getEmail(), user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSlots() {
        File file = new File("scheduleSlots.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                if (p.length >= 7) {
                    ScheduleSlot slot = new ScheduleSlot(p[0], p[1], p[2], p[3], p[4], p[5], p[6]);
                    slots.put(p[0], slot);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAppointments() {
        File file = new File("appointments.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                appointments.put(p[0], new Appointment(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadQueueTickets() {
        File file = new File("queue_tickets.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                queueTickets.put(p[0], new QueueTicket(p[0], p[1], Integer.parseInt(p[2]), p[3], p[4], p[5], p[6]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConsultations() {
        File file = new File("consultations.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                consultations.put(p[0], new Consultation(p[0], p[1], p[2], p[3], p[4], p[5], p[6]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadNotifications() {
        File file = new File("notifications.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int id = 1;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                
                // Expected format in text file: recipientName|NOTIFICATION_TYPE|message
                if (p.length >= 3) {
                    NotificationType type = NotificationType.valueOf(p[1].trim().toUpperCase());
                    Notification notification = new Notification(p[0].trim(), type, p[2].trim());
                    notifications.put(String.valueOf(id++), notification);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== SAVE METHODS ====================

    public void saveUsers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("users.txt"))) {
            for (User u : users.values()) {
                if (u instanceof Patient) {
                    Patient p = (Patient) u;
                    pw.println("PATIENT|" + p.getUserId() + "|" + p.getFullName() + "|" + p.getEmail() + "|" + p.getPhoneNumber() + "|" + p.getPasswordHash() + "|" + p.getAccountStatus() + "|" + p.getPatientNo() + "|" + p.getDateOfBirth() + "|" + p.getGender() + "|" + p.getAddress());
                } else if (u instanceof Doctor) {
                    Doctor d = (Doctor) u;
                    pw.println("DOCTOR|" + d.getUserId() + "|" + d.getFullName() + "|" + d.getEmail() + "|" + d.getPhoneNumber() + "|" + d.getPasswordHash() + "|" + d.getAccountStatus() + "|" + d.getDoctorId() + "|" + d.getSpecialization() + "|" + d.getDepartment() + "|" + d.getConsultationFee());
                } else if (u instanceof Administrator) {
                    Administrator a = (Administrator) u;
                    pw.println("ADMIN|" + a.getUserId() + "|" + a.getFullName() + "|" + a.getEmail() + "|" + a.getPhoneNumber() + "|" + a.getPasswordHash() + "|" + a.getAccountStatus() + "|" + a.getAdminId() + "|" + a.getStaffDepartment() + "|" + a.getEmploymentStatus());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAppointments() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("appointments.txt"))) {
            for (Appointment a : appointments.values()) {
                pw.println(a.getAppointmentId() + "|" + a.getPatientId() + "|" + a.getDoctorId() + "|" + a.getSlotId() + "|" + a.getBookedAt() + "|" + a.getReasonForVisit() + "|" + a.getStatus() + "|" + a.getCheckInTime() + "|" + a.getCancellationReason());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveSlots() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("scheduleSlots.txt"))) {
            for (ScheduleSlot s : slots.values()) {
                pw.println(s.getSlotId() + "|" + s.getDoctorId() + "|" + s.getSlotDate() + "|" +
                           s.getStartTime() + "|" + s.getEndTime() + "|" + s.getMode() + "|" + s.getStatus());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void saveNotifications() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("notifications.txt"))) {
            for (Notification n : notifications.values()) {
                pw.println(n.getRecipientName() + "|" + n.getType() + "|" + n.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== GETTERS ====================

    public Map<String, User> getUsers() { return users; }
    public Map<String, ScheduleSlot> getSlots() { return slots; }
    public Map<String, Appointment> getAppointments() { return appointments; }
    public Map<String, QueueTicket> getQueueTickets() { return queueTickets; }
    public Map<String, Consultation> getConsultations() { return consultations; }
    public Map<String, Notification> getNotifications() { return notifications; }
}