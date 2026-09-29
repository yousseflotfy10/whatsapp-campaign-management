package com.wcm.campaign.repository;

import com.wcm.campaign.model.User;
import com.wcm.campaign.model.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findByUser(User user);

    void deleteByUser(User user);
}