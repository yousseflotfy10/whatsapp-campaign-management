package com.wcm.campaign.repository;

import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByPermissionName(Permission permission);
}