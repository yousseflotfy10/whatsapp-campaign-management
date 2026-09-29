package com.wcm.campaign.service;

import com.wcm.campaign.dto.UserRequest;
import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.PermissionEntity;
import com.wcm.campaign.model.Role;
import com.wcm.campaign.model.User;
import com.wcm.campaign.model.UserPermission;
import com.wcm.campaign.repository.PermissionRepository;
import com.wcm.campaign.repository.UserPermissionRepository;
import com.wcm.campaign.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PermissionRepository permissionRepository,
            UserPermissionRepository userPermissionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(UserRequest request) {

        validateUserRequest(request);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("ADMIN users cannot be created through this endpoint");
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setPasswordHashed(passwordEncoder.encode(request.getPassword()));

        user.setRole(request.getRole());

        assignPermissions(user, request.getPermissions());

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long userId, UserRequest request) {

        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));


        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("The system ADMIN cannot be modified through this endpoint");
        }

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {

            if (userRepository.existsByUsername(
                    request.getUsername()
            )) {
                throw new IllegalArgumentException("Username already exists");
            }

            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {

            if (userRepository.existsByEmail(
                    request.getEmail()
            )) {
                throw new IllegalArgumentException("Email already exists");
            }

            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {

            user.setPasswordHashed(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {

            if (request.getRole() == Role.ADMIN) {
                throw new IllegalArgumentException("A user cannot be changed to ADMIN");
            }

            user.setRole(request.getRole());
        }

        if (request.getPermissions() != null) {

            userPermissionRepository.deleteByUser(user);

            user.getUserPermissions().clear();

            assignPermissions(user, request.getPermissions());
        }

        return userRepository.save(user);
    }

    public User getUserById(Long userId) {

        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));


        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("The system ADMIN cannot be deleted");
        }

        userRepository.delete(user);
    }

    private void assignPermissions(User user, List<Permission> permissions) {

        if (permissions == null || permissions.isEmpty()) {
            return;
        }

        Set<Permission> uniquePermissions = new HashSet<>(permissions);

        for (Permission permission : uniquePermissions) {

            PermissionEntity permissionEntity = permissionRepository
                            .findByPermissionName(permission)
                            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permission));

            UserPermission userPermission = new UserPermission(user, permissionEntity);

            user.getUserPermissions().add(userPermission);
        }
    }

    private void validateUserRequest(
            UserRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException("User request cannot be null");
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("ADMIN cannot be created through normal user management");
        }
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username.trim()).orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }
}