package com.healthcare.service;

import com.healthcare.model.Administrator;
import com.healthcare.model.Doctor;
import com.healthcare.model.Patient;
import com.healthcare.model.Role;
import com.healthcare.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserService {
    private final DatabaseStore db;

    public UserService() {
        this.db = DatabaseStore.getInstance();
    }

    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public User authenticate(String email, String inputPassword) {
        if (email == null || inputPassword == null) return null;

        User user = db.getUsers().get(email.trim());
        if (user == null) return null;

        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            return null;
        }

        String storedPassword = user.getPasswordHash();
        String hashedInput = hashPassword(inputPassword);

        if (storedPassword.equals(hashedInput) || storedPassword.equals(inputPassword)) {
            return user;
        }
        return null;
    }

    /**
     * Generic method to add a pre-constructed User object (Patient, Doctor, or Administrator)
     */
    public boolean addUser(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return false;
        }

        String email = user.getEmail().trim();
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return false;
        }

        // Ensure password is stored as hash
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty() && user.getPasswordHash().length() < 64) {
            user.setPasswordHash(hashPassword(user.getPasswordHash()));
        }

        // Assign role based on instance type
        if (user instanceof Patient) {
            assignRole(user, "PATIENT");
        } else if (user instanceof Doctor) {
            assignRole(user, "DOCTOR");
        } else if (user instanceof Administrator) {
            assignRole(user, "ADMIN");
        }

        db.getUsers().put(email, user);
        db.saveUsers();
        return true;
    }

    public Patient registerPatient(String fullName, String email, String phoneNumber,
                                   String password, String dateOfBirth, String gender, String address) {
        if (!isRegistrationInputValid(fullName, email, phoneNumber, password)) return null;
        email = email.trim();
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return null;
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String patientNo = "PAT-" + UUID.randomUUID().toString().substring(0, 6);
        Patient patient = new Patient(userId, fullName.trim(), email, phoneNumber.trim(),
                hashPassword(password), "ACTIVE", patientNo, dateOfBirth, gender, address);
        assignRole(patient, "PATIENT");
        db.getUsers().put(email, patient);
        db.saveUsers();
        return patient;
    }

    public Doctor registerDoctor(String fullName, String email, String phoneNumber,
                                 String password, String specialization, String department, double fee) {
        if (!isRegistrationInputValid(fullName, email, phoneNumber, password)) return null;
        email = email.trim();
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return null;
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String doctorId = "DOC-" + UUID.randomUUID().toString().substring(0, 6);
        Doctor doctor = new Doctor(userId, fullName.trim(), email, phoneNumber.trim(),
                hashPassword(password), "ACTIVE", doctorId, specialization, department, fee);
        assignRole(doctor, "DOCTOR");
        db.getUsers().put(email, doctor);
        db.saveUsers();
        return doctor;
    }

    public Administrator registerAdmin(String fullName, String email, String phoneNumber,
                                       String password, String staffDepartment, String employmentStatus) {
        if (!isRegistrationInputValid(fullName, email, phoneNumber, password)) return null;
        email = email.trim();
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return null;
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String adminId = "ADM-" + UUID.randomUUID().toString().substring(0, 6);
        Administrator admin = new Administrator(userId, fullName.trim(), email, phoneNumber.trim(),
                hashPassword(password), "ACTIVE", adminId, staffDepartment, employmentStatus);
        assignRole(admin, "ADMIN");
        db.getUsers().put(email, admin);
        db.saveUsers();
        return admin;
    }

    private boolean isRegistrationInputValid(String fullName, String email, String phoneNumber, String password) {
        return fullName != null && !fullName.trim().isEmpty()
                && email != null && !email.trim().isEmpty()
                && phoneNumber != null && !phoneNumber.trim().isEmpty()
                && password != null && !password.isEmpty();
    }

    public User getUserByEmail(String email) {
        if (email == null) return null;
        return db.getUsers().get(email.trim());
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(db.getUsers().values());
    }

    public boolean updateProfile(User actor, String currentEmail, String newEmail,
                                String fullName, String phoneNumber) {
        if (actor == null || currentEmail == null || newEmail == null
                || fullName == null || phoneNumber == null) return false;

        User target = db.getUsers().get(currentEmail.trim());
        if (target == null) return false;

        boolean isSelf = actor.getUserId().equals(target.getUserId());
        if (!isSelf && !actor.hasPermission("PERM_MANAGE_USER")) return false;

        newEmail = newEmail.trim();
        fullName = fullName.trim();
        phoneNumber = phoneNumber.trim();

        if (newEmail.isEmpty() || fullName.isEmpty() || phoneNumber.isEmpty()) return false;

        if (!newEmail.equalsIgnoreCase(target.getEmail())) {
            User existing = db.getUsers().get(newEmail);
            if (existing != null && !existing.getUserId().equals(target.getUserId())) {
                return false;
            }
            db.getUsers().remove(target.getEmail());
            target.setEmail(newEmail);
            db.getUsers().put(newEmail, target);
        }

        target.setFullName(fullName);
        target.setPhoneNumber(phoneNumber);
        db.saveUsers();
        return true;
    }

    public boolean deactivateAccount(User actor, String targetEmail) {
        if (actor == null || targetEmail == null) return false;

        User target = db.getUsers().get(targetEmail.trim());
        if (target == null) return false;

        boolean isSelf = actor.getUserId().equals(target.getUserId());
        if (!isSelf && !actor.hasPermission("PERM_MANAGE_USER")) return false;

        target.setAccountStatus("INACTIVE");
        db.saveUsers();
        return true;
    }

    public boolean changePassword(User user, String oldPassword, String newPassword) {
        if (user == null || oldPassword == null || newPassword == null || newPassword.isEmpty()) {
            return false;
        }

        String storedPassword = user.getPasswordHash();
        if (!(storedPassword.equals(hashPassword(oldPassword)) || storedPassword.equals(oldPassword))) {
            return false;
        }

        user.setPasswordHash(hashPassword(newPassword));
        db.saveUsers();
        return true;
    }

    public boolean resetPassword(User actor, String targetEmail, String newPassword) {
        if (actor == null || targetEmail == null || newPassword == null || newPassword.isEmpty()) {
            return false;
        }
        if (!actor.hasPermission("PERM_MANAGE_USER")) return false;

        User target = db.getUsers().get(targetEmail.trim());
        if (target == null) return false;

        target.setPasswordHash(hashPassword(newPassword));
        db.saveUsers();
        return true;
    }

    public boolean hasPermission(User user, String permissionName) {
        return user != null && user.hasPermission(permissionName);
    }

    private void assignRole(User user, String roleName) {
        for (Role role : db.getRoles().values()) {
            if (role.getRoleName().equalsIgnoreCase(roleName)) {
                user.getRoles().add(role);
                return;
            }
        }
    }
}