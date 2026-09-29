package com.wcm.campaign.repository;

import com.wcm.campaign.model.Role;
import com.wcm.campaign.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @EntityGraph(attributePaths = {"userPermissions", "userPermissions.permission"})
    Optional<User> findById(Long userId);

    @Override
    @EntityGraph(attributePaths = {"userPermissions", "userPermissions.permission"})
    List<User> findAll();

    @EntityGraph(attributePaths = {"userPermissions", "userPermissions.permission"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"userPermissions", "userPermissions.permission"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"userPermissions", "userPermissions.permission"})
    Optional<User> findByAccessToken(String accessToken);

    @EntityGraph(attributePaths = {"userPermissions", "userPermissions.permission"})
    Optional<User> findByRefreshToken(String refreshToken);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

}