package com.wcm.campaign.controller;

import com.wcm.campaign.dto.UserRequest;
import com.wcm.campaign.dto.UserResponse;
import com.wcm.campaign.model.User;
import com.wcm.campaign.service.UserService;
import com.wcm.campaign.service.RealtimeEventService;
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

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

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

        logger.info("Fetching all users");

        List<UserResponse> users = userService
                .getAllUsers()
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        logger.info("Fetched {} users", users.size());

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {

        logger.info("Fetching user with id={}", id);

        User user = userService.getUserById(id);

        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {

        logger.info("Creating user");

        User createdUser = userService.createUser(request);
        realtimeEventService.publishAfterCommit("users");

        logger.info("User created successfully with id={}", createdUser.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(createdUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {

        logger.info("Updating user with id={}", id);

        User updatedUser = userService.updateUser(id, request);
        realtimeEventService.publishAfterCommit("users");

        logger.info("User updated successfully with id={}", id);

        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        logger.info("Deleting user with id={}", id);

        userService.deleteUser(id);
        realtimeEventService.publishAfterCommit("users");

        logger.info("User deleted successfully with id={}", id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<List<UserResponse>> getPendingUsers() {

        logger.info("Fetching pending users");

        List<UserResponse> users = userService.getPendingUsers()
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        logger.info("Fetched {} pending users", users.size());

        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponse> rejectUser(@PathVariable Long id) {

        logger.info("Rejecting user with id={}", id);

        User rejectedUser = userService.rejectUser(id);

        realtimeEventService.publishAfterCommit("users");

        logger.info("User rejected successfully with id={}", id);

        return ResponseEntity.ok(UserResponse.fromUser(rejectedUser));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponse> approveUser(@PathVariable Long id) {

        logger.info("Approving user with id={}", id);

        User approvedUser = userService.approveUser(id);
        realtimeEventService.publishAfterCommit("users");

        logger.info("User approved successfully with id={}", id);

        return ResponseEntity.ok(UserResponse.fromUser(approvedUser));
    }
}