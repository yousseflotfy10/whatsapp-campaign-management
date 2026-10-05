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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wcm.campaign.model.User.AccountStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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

        logger.info("Creating user");

        validateUserRequest(request);

        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("User creation failed: username already exists");
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("User creation failed: email already exists");
            throw new IllegalArgumentException("Email already exists");
        }

        if (request.getRole() == Role.ADMIN) {
            logger.warn("User creation failed: administrator account creation is not allowed through this endpoint");
            throw new IllegalArgumentException("Administrator accounts cannot be created through this endpoint");
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setPasswordHashed(passwordEncoder.encode(request.getPassword()));

        user.setStatus(AccountStatus.ACTIVE);

        user.setRole(request.getRole());

        assignPermissions(user, request.getPermissions());

        User savedUser = userRepository.save(user);

        logger.info("User created successfully with id {}", savedUser.getUserId());

        return savedUser;
    }

    @Transactional
    public User updateUser(Long userId, UserRequest request) {

        logger.info("Updating user {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> {
            logger.warn("User not found with id {}", userId);
            return new IllegalArgumentException("User not found");
        });

        if (user.getRole() == Role.ADMIN) {
            logger.warn("Administrator user {} cannot be modified", userId);
            throw new IllegalArgumentException("The system administrator cannot be modified through this endpoint");
        }

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                logger.warn("User update failed: username already exists");
                throw new IllegalArgumentException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                logger.warn("User update failed: email already exists");
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHashed(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            if (request.getRole() == Role.ADMIN) {
                logger.warn("User {} cannot be changed to ADMIN", userId);
                throw new IllegalArgumentException("A user cannot be changed to an administrator");
            }
            user.setRole(request.getRole());
        }

        if (request.getPermissions() != null) {
            userPermissionRepository.deleteByUser(user);
            user.getUserPermissions().clear();
            assignPermissions(user, request.getPermissions());
        }

        User updatedUser = userRepository.save(user);

        logger.info("User {} updated successfully", userId);

        return updatedUser;
    }

    public User getUserById(Long userId) {

        return userRepository.findById(userId).orElseThrow(() -> {
            logger.warn("User not found with id {}", userId);
            return new IllegalArgumentException("User not found");
        });
    }

    public List<User> getAllUsers() {
        logger.info("Fetching all users");
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId) {

        logger.info("Deleting user {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> {
            logger.warn("User not found with id {}", userId);
            return new IllegalArgumentException("User not found");
        });

        if (user.getRole() == Role.ADMIN) {
            logger.warn("System administrator {} cannot be deleted", userId);
            throw new IllegalArgumentException("The system ADMIN cannot be deleted");
        }

        userRepository.delete(user);

        logger.info("User {} deleted successfully", userId);
    }

    private void assignPermissions(User user, List<Permission> permissions) {

        if (permissions == null || permissions.isEmpty()) {
            return;
        }

        Set<Permission> uniquePermissions = new HashSet<>(permissions);

        for (Permission permission : uniquePermissions) {
            PermissionEntity permissionEntity = permissionRepository.findByPermissionName(permission)
                    .orElseThrow(() -> {
                        logger.warn("Permission not found: {}", permission);
                        return new IllegalArgumentException("Permission not found: " + permission);
                    });

            UserPermission userPermission = new UserPermission(user, permissionEntity);

            user.getUserPermissions().add(userPermission);
        }
    }

    private void validateUserRequest(UserRequest request) {

        if (request == null) {
            logger.warn("User creation failed: request is null");
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
        return userRepository.findByUsername(username.trim()).orElseThrow(() -> {
            logger.warn("User not found with username {}", username);
            return new RuntimeException("User not found with username: " + username);
        });
    }

    @Transactional
    public User createPendingUser(String username, String email, String password, Role role) {

        logger.info("Creating pending user");

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if (role == Role.ADMIN) {
            logger.warn("Pending user creation failed: ADMIN signup is not allowed");
            throw new IllegalArgumentException("Administrator accounts cannot be created through signup");
        }

        if (userRepository.existsByUsername(username)) {
            logger.warn("Pending user creation failed: username already exists");
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            logger.warn("Pending user creation failed: email already exists");
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();

        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPasswordHashed(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(User.AccountStatus.PENDING);

        User savedUser = userRepository.save(user);

        logger.info("Pending user created successfully with id {}", savedUser.getUserId());

        return savedUser;
    }

    public List<User> getPendingUsers() {
        logger.info("Fetching pending users");
        return userRepository.findByStatus(User.AccountStatus.PENDING);
    }

    @Transactional
    public User approveUser(Long userId) {

        logger.info("Approving user {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> {
            logger.warn("User not found with id {}", userId);
            return new IllegalArgumentException("User not found");
        });

        if (user.getStatus() != User.AccountStatus.PENDING) {
            logger.warn("User {} cannot be approved because its status is {}", userId, user.getStatus());
            throw new IllegalArgumentException("Only pending users can be approved");
        }

        user.setStatus(User.AccountStatus.ACTIVE);
        user.setApprovedAt(java.time.LocalDateTime.now());

        User approvedUser = userRepository.save(user);

        logger.info("User {} approved successfully", userId);

        return approvedUser;
    }

    @Transactional
    public User rejectUser(Long userId) {

        logger.info("Rejecting user {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> {
            logger.warn("User not found with id {}", userId);
            return new IllegalArgumentException("User not found");
        });

        if (user.getStatus() != User.AccountStatus.PENDING) {
            logger.warn("User {} cannot be rejected because its status is {}", userId, user.getStatus());
            throw new IllegalArgumentException("Only pending users can be rejected");
        }

        user.setStatus(User.AccountStatus.REJECTED);

        User rejectedUser = userRepository.save(user);

        logger.info("User {} rejected successfully", userId);

        return rejectedUser;
    }
}