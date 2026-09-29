package com.wcm.campaign.service;

import com.wcm.campaign.dto.SignupRequest;
import com.wcm.campaign.model.AccountRequest;
import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.PermissionEntity;
import com.wcm.campaign.model.RequestStatus;
import com.wcm.campaign.model.Role;
import com.wcm.campaign.model.User;
import com.wcm.campaign.model.UserPermission;
import com.wcm.campaign.repository.AccountRequestRepository;
import com.wcm.campaign.repository.PermissionRepository;
import com.wcm.campaign.repository.UserPermissionRepository;
import com.wcm.campaign.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AccountRequestService {

    private final AccountRequestRepository accountRequestRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountRequestService(
            AccountRequestRepository accountRequestRepository,
            UserRepository userRepository,
            PermissionRepository permissionRepository,
            UserPermissionRepository userPermissionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.accountRequestRepository = accountRequestRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AccountRequest submitRequest(SignupRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Signup request cannot be null");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (accountRequestRepository.existsByUsernameAndStatus(username, RequestStatus.PENDING)) {
            throw new IllegalArgumentException("A pending request already exists for this username");
        }

        if (accountRequestRepository.existsByEmailAndStatus(email, RequestStatus.PENDING)) {
            throw new IllegalArgumentException("A pending request already exists for this email");
        }

        AccountRequest accountRequest = new AccountRequest();

        accountRequest.setUsername(username);
        accountRequest.setEmail(email);

        accountRequest.setPasswordHashed(passwordEncoder.encode(request.getPassword()));

        accountRequest.setStatus(RequestStatus.PENDING);

        accountRequest.setRequestedAt(LocalDateTime.now());

        return accountRequestRepository.save(accountRequest);
    }

    public List<AccountRequest> getPendingRequests() {
        return accountRequestRepository.findByStatus(RequestStatus.PENDING);
    }

    public AccountRequest getRequestById(Long requestId) {
        return accountRequestRepository.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Account request not found"));
    }

    @Transactional
    public User approveRequest(Long requestId, Role role, List<Permission> permissions) {

        AccountRequest request = getRequestById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("This account request has already been processed");
        }

        if (role == null || role == Role.ADMIN) {
            throw new IllegalArgumentException("Approved account must be MANAGER or EMPLOYEE");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();

        user.setUsername(request.getUsername());

        user.setEmail(request.getEmail());

        user.setPasswordHashed(request.getPasswordHashed());

        user.setRole(role);

        assignPermissions(user, permissions);

        User savedUser = userRepository.save(user);

        request.setStatus(RequestStatus.APPROVED);

        request.setProcessedAt(LocalDateTime.now());

        accountRequestRepository.save(request);

        return savedUser;
    }
    @Transactional
    public AccountRequest rejectRequest(Long requestId) {

        AccountRequest request = getRequestById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("This account request has already been processed");
        }

        request.setStatus(RequestStatus.REJECTED);

        request.setProcessedAt(LocalDateTime.now());

        return accountRequestRepository.save(request);
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
}