package com.healthcare.model;

public class Patient extends User {
    private String patientNo;
    private String dateOfBirth;
    private String gender;
    private String address;

    public Patient(String userId, String fullName, String email, String phoneNumber, String passwordHash, String accountStatus, String patientNo, String dateOfBirth, String gender, String address) {
        super(userId, fullName, email, phoneNumber, passwordHash, accountStatus);
        this.patientNo = patientNo;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.address = address;
    }

    public String getPatientNo() { return patientNo; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
    public String getAddress() { return address; }

    @Override
    public String getRoleLabel() { return "PATIENT"; }
}