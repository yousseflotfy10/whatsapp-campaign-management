package com.wcm.campaign.dto;

import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.User;
import com.wcm.campaign.model.UserPermission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class UserResponse {

    private Long userId;
    private String username;
    private String email;
    private String role;
    private List<String> permissions;
    private LocalDateTime createdAt;
    private User.AccountStatus status;
    private String accessToken;
    private String refreshToken;

    public UserResponse() {
    }

    public UserResponse(
            Long userId,
            String username,
            String email,
            String role,
            List<String> permissions,
            LocalDateTime createdAt,
            User.AccountStatus status
    ) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.permissions = permissions;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static UserResponse fromUser(User user) {

        List<String> permissions;

        if (user.getRole() != null && user.getRole().name().equals("ADMIN")) {
            permissions = List.of(
                            Permission.values()
                    ).stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
        } else {
            permissions = user.getUserPermissions().stream()
                    .map(UserPermission::getPermission)
                    .map(permission -> permission.getPermissionName().name())
                    .sorted()
                    .collect(Collectors.toList());
        }

        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                permissions,
                user.getCreatedAt(),
                user.getStatus()
        );
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User.AccountStatus getStatus() {
        return status;
    }

    public void setStatus(User.AccountStatus status) {
        this.status = status;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}