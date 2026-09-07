package com.healthcare.service;

import com.healthcare.model.Administrator;
import com.healthcare.model.Doctor;
import com.healthcare.model.Patient;
import com.healthcare.model.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class UserService {
    private DatabaseStore db;

    public UserService() {
        this.db = DatabaseStore.getInstance();
    }

    /**
     * Hashes plain text using SHA-256.
     */
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password; // Fallback to raw string if hashing algorithm is unavailable
        }
    }

    /**
     * Authenticates user with support for both plain-text and hashed passwords.
     */
    public User authenticate(String email, String inputPassword) {
        User user = db.getUsers().get(email);
        if (user == null) {
            return null;
        }

        String storedPassword = user.getPasswordHash();
        String hashedInput = hashPassword(inputPassword);

        // Allow match if input matches stored hash OR input matches raw plain text
        if (storedPassword.equals(hashedInput) || storedPassword.equals(inputPassword)) {
            return user;
        }

        return null;
    }

    public Patient registerPatient(String fullName, String email, String phoneNumber, String password, String dateOfBirth, String gender, String address) {
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return null;
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String patientNo = "PAT-" + UUID.randomUUID().toString().substring(0, 6);
        String hashedPassword = hashPassword(password);

        Patient patient = new Patient(userId, fullName, email, phoneNumber, hashedPassword, "ACTIVE", patientNo, dateOfBirth, gender, address);
        db.getUsers().put(email, patient);
        db.saveUsers();

        return patient;
    }

    public Doctor registerDoctor(String fullName, String email, String phoneNumber, String password, String specialization, String department, double fee) {
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return null;
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String doctorId = "DOC-" + UUID.randomUUID().toString().substring(0, 6);
        String hashedPassword = hashPassword(password);

        Doctor doctor = new Doctor(userId, fullName, email, phoneNumber, hashedPassword, "ACTIVE", doctorId, specialization, department, fee);
        db.getUsers().put(email, doctor);
        db.saveUsers();

        return doctor;
    }

    public Administrator registerAdmin(String fullName, String email, String phoneNumber, String password, String staffDepartment, String employmentStatus) {
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return null;
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String adminId = "ADM-" + UUID.randomUUID().toString().substring(0, 6);
        String hashedPassword = hashPassword(password);

        Administrator admin = new Administrator(userId, fullName, email, phoneNumber, hashedPassword, "ACTIVE", adminId, staffDepartment, employmentStatus);
        db.getUsers().put(email, admin);
        db.saveUsers();

        return admin;
    }

    public User getUserByEmail(String email) {
        return db.getUsers().get(email);
    }
}