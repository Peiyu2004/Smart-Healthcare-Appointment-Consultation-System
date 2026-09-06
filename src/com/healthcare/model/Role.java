package com.healthcare.model;

import java.util.HashSet;
import java.util.Set;

public class Role {
    private String roleId;
    private String roleName;
    private String description;
    private Set<Permission> permissions = new HashSet<>();

    public Role(String roleId, String roleName, String description) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = description;
    }

    public String getRoleId() { return roleId; }
    public String getRoleName() { return roleName; }
    public String getDescription() { return description; }
    public Set<Permission> getPermissions() { return permissions; }

    public void addPermission(Permission permission) { permissions.add(permission); }
}