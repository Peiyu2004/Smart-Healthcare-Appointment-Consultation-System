//package com.healthcare.model;
//
//public class Administrator extends User {
//    private String adminId;
//    private String staffDepartment;
//    private String employmentStatus;
//
//    public Administrator(String userId, String fullName, String email, String phoneNumber, String passwordHash, String accountStatus, String adminId, String staffDepartment, String employmentStatus) {
//        super(userId, fullName, email, phoneNumber, passwordHash, accountStatus);
//        this.adminId = adminId;
//        this.staffDepartment = staffDepartment;
//        this.employmentStatus = employmentStatus;
//    }
//
//    public String getAdminId() { return adminId; }
//    public String getStaffDepartment() { return staffDepartment; }
//    public String getEmploymentStatus() { return employmentStatus; }
//
//    public void setStaffDepartment(String staffDepartment) { this.staffDepartment = staffDepartment; }
//    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
//
//    @Override
//    public String getRoleLabel() { return "ADMIN"; }
//}

package com.healthcare.model;

import java.io.Serializable;

public class Administrator extends User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String adminId;
    private String staffDepartment;
    private String employmentStatus;

    public Administrator(String userId, String fullName, String email, String phoneNumber, 
                         String passwordHash, String accountStatus, String adminId, 
                         String staffDepartment, String employmentStatus) {
        super(userId, fullName, email, phoneNumber, passwordHash, accountStatus);
        this.adminId = adminId;
        this.staffDepartment = staffDepartment;
        this.employmentStatus = employmentStatus;
    }

    // Getters
    public String getAdminId() { return adminId; }
    public String getStaffDepartment() { return staffDepartment; }
    public String getEmploymentStatus() { return employmentStatus; }

    // Setters
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public void setStaffDepartment(String staffDepartment) { this.staffDepartment = staffDepartment; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }

    @Override
    public String getRoleLabel() { 
        return "ADMIN"; 
    }
}