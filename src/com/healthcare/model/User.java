package com.healthcare.model;

import java.util.HashSet;
import java.util.Set;

public abstract class User {
    protected String userId;
    protected String fullName;
    protected String email;
    protected String phoneNumber;
    protected String passwordHash;
    protected String accountStatus;
    protected Set<Role> roles = new HashSet<>();

    public User(String userId, String fullName, String email, String phoneNumber, String passwordHash, String accountStatus) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.accountStatus = accountStatus;
    }

    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPasswordHash() { return passwordHash; }
    public String getAccountStatus() { return accountStatus; }
    public Set<Role> getRoles() { return roles; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public boolean hasPermission(String permissionName) {
        if (permissionName == null) return false;
        for (Role role : roles) {
            if (role.hasPermission(permissionName)) return true;
        }
        return false;
    }
    
    public abstract String getRoleLabel();
}
