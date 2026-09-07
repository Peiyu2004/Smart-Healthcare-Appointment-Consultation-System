package com.healthcare.model;

public class Doctor extends User {
    private String doctorId;
    private String specialization;
    private String department;
    private double consultationFee;

    public Doctor(String userId, String fullName, String email, String phoneNumber, String passwordHash, String accountStatus, String doctorId, String specialization, String department, double consultationFee) {
        super(userId, fullName, email, phoneNumber, passwordHash, accountStatus);
        this.doctorId = doctorId;
        this.specialization = specialization;
        this.department = department;
        this.consultationFee = consultationFee;
    }

    public String getDoctorId() { return doctorId; }
    public String getSpecialization() { return specialization; }
    public String getDepartment() { return department; }
    public double getConsultationFee() { return consultationFee; }
    
    @Override
    public String getRoleLabel() { return "DOCTOR"; }
    
    public void setSpecialization(String specialization) {
    this.specialization = specialization;
}

public void setDepartment(String department) {
    this.department = department;
}

public void setConsultationFee(double consultationFee) {
    this.consultationFee = consultationFee;
}
}
