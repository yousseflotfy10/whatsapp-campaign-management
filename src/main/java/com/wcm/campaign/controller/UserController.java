
package com.wcm.campaign.controller;

import com.wcm.campaign.dto.UserRequest;
import com.wcm.campaign.dto.UserResponse;
import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.Role;
import com.wcm.campaign.model.User;
import com.wcm.campaign.service.RealtimeEventService;
import com.wcm.campaign.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger =
            LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final RealtimeEventService realtimeEventService;

    public UserController(
            UserService userService,
            RealtimeEventService realtimeEventService
    ) {
        this.userService = userService;
        this.realtimeEventService = realtimeEventService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers()
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id
    ) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserResponse> createUser(
            @RequestBody UserRequest request
    ) {
        User createdUser = userService.createUser(request);
        realtimeEventService.publishAfterCommit("users");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.fromUser(createdUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserRequest request
    ) {
        User updatedUser = userService.updateUser(id, request);
        realtimeEventService.publishAfterCommit("users");

        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id
    ) {
        userService.deleteUser(id);
        realtimeEventService.publishAfterCommit("users");

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<List<UserResponse>> getPendingUsers() {
        List<UserResponse> users = userService.getPendingUsers()
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponse> approveUser(
            @PathVariable Long id,
            @RequestBody ApproveUserRequest request
    ) {
        logger.info("Approving pending user {}", id);

        User approvedUser = userService.approveUser(
                id,
                request.getRole(),
                request.getPermissions()
        );

        realtimeEventService.publishAfterCommit("users");

        return ResponseEntity.ok(UserResponse.fromUser(approvedUser));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponse> rejectUser(
            @PathVariable Long id
    ) {
        User rejectedUser = userService.rejectUser(id);
        realtimeEventService.publishAfterCommit("users");

        return ResponseEntity.ok(UserResponse.fromUser(rejectedUser));
    }

    public static class ApproveUserRequest {

        private Role role;
        private List<Permission> permissions;

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
}
