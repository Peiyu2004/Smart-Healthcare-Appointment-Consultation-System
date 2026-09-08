package com.healthcare.model;

import java.io.Serializable;
import java.util.Objects;

public class Permission implements Serializable {
    private static final long serialVersionUID = 1L;

    private String permissionId;
    private String permissionName;
    private String module;
    private String action;

    public Permission(String permissionId, String permissionName, String module, String action) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.module = module;
        this.action = action;
    }

    public Permission(String permissionName, String module, String action) {
        this(permissionName, permissionName, module, action);
    }

    // Getters
    public String getPermissionId() { return permissionId; }
    public String getPermissionName() { return permissionName; }
    public String getModule() { return module; }
    public String getAction() { return action; }

    // Setters
    public void setPermissionId(String permissionId) { this.permissionId = permissionId; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
    public void setModule(String module) { this.module = module; }
    public void setAction(String action) { this.action = action; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(permissionName, that.permissionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionName);
    }
}