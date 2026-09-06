package com.healthcare.model;

public class Administrator extends User {
    private String adminId;
    private String staffDepartment;
    private String employmentStatus;

    public Administrator(String userId, String fullName, String email, String phoneNumber, String passwordHash, String accountStatus, String adminId, String staffDepartment, String employmentStatus) {
        super(userId, fullName, email, phoneNumber, passwordHash, accountStatus);
        this.adminId = adminId;
        this.staffDepartment = staffDepartment;
        this.employmentStatus = employmentStatus;
    }

    public String getAdminId() { return adminId; }
    public String getStaffDepartment() { return staffDepartment; }
    public String getEmploymentStatus() { return employmentStatus; }

    @Override
    public String getRoleLabel() { return "ADMIN"; }
}