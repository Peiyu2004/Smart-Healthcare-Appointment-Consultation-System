package com.healthcare.service;

import com.healthcare.model.Administrator;
import com.healthcare.model.Doctor;
import com.healthcare.model.Patient;
import com.healthcare.model.User;

import java.util.UUID;

public class UserService {
    private DatabaseStore db;

    public UserService() {
        this.db = DatabaseStore.getInstance();
    }

    public User authenticate(String email, String passwordHash) {
        User user = db.getUsers().get(email);
        if (user != null && user.getPasswordHash().equals(passwordHash)) {
            return user;
        }
        return null;
    }

    public Patient registerPatient(String fullName, String email, String phoneNumber, String passwordHash, String dateOfBirth, String gender, String address) {
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return null;
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String patientNo = "PAT-" + UUID.randomUUID().toString().substring(0, 6);

        Patient patient = new Patient(userId, fullName, email, phoneNumber, passwordHash, "ACTIVE", patientNo, dateOfBirth, gender, address);
        db.getUsers().put(email, patient);
        db.saveUsers();

        return patient;
    }

    public Doctor registerDoctor(String fullName, String email, String phoneNumber, String passwordHash, String specialization, String department, double fee) {
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return null;
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String doctorId = "DOC-" + UUID.randomUUID().toString().substring(0, 6);

        Doctor doctor = new Doctor(userId, fullName, email, phoneNumber, passwordHash, "ACTIVE", doctorId, specialization, department, fee);
        db.getUsers().put(email, doctor);
        db.saveUsers();

        return doctor;
    }

    public Administrator registerAdmin(String fullName, String email, String phoneNumber, String passwordHash, String staffDepartment, String employmentStatus) {
        if (db.getUsers().containsKey(email)) {
            System.out.println("Email already registered.");
            return null;
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String adminId = "ADM-" + UUID.randomUUID().toString().substring(0, 6);

        Administrator admin = new Administrator(userId, fullName, email, phoneNumber, passwordHash, "ACTIVE", adminId, staffDepartment, employmentStatus);
        db.getUsers().put(email, admin);
        db.saveUsers();

        return admin;
    }

    public User getUserByEmail(String email) {
        return db.getUsers().get(email);
    }
}