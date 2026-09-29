package com.wcm.campaign.dto;

import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.Role;

import java.util.List;

public class UserRequest {

    private String username;

    private String email;

    private String password;

    private Role role;

    private List<Permission> permissions;

    public UserRequest() {
    }

    public UserRequest(
            String username,
            String email,
            String password,
            Role role,
            List<Permission> permissions
    ) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.permissions = permissions;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
}