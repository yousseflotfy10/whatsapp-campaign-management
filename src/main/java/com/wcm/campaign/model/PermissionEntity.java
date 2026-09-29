package com.wcm.campaign.model;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions", uniqueConstraints = {@UniqueConstraint(columnNames = "permission_name")})
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_name", nullable = false, unique = true, length = 100)
    private Permission permissionName;

    @Column(name = "description", length = 255)
    private String description;

    public PermissionEntity() {}

    public PermissionEntity(Permission permissionName, String description) {
        this.permissionName = permissionName;
        this.description = description;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public Permission getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(Permission permissionName) {
        this.permissionName = permissionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}