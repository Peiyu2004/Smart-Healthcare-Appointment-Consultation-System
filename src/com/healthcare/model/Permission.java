package com.healthcare.model;

public class Permission {
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

    public String getPermissionId() { return permissionId; }
    public String getPermissionName() { return permissionName; }
    public String getModule() { return module; }
    public String getAction() { return action; }
}