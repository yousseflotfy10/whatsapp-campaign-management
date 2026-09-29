package com.wcm.campaign.controller;

import com.wcm.campaign.dto.UserRequest;
import com.wcm.campaign.dto.UserResponse;
import com.wcm.campaign.model.User;
import com.wcm.campaign.service.UserService;
import com.wcm.campaign.service.RealtimeEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

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

        List<UserResponse> users = userService
                .getAllUsers()
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {

        User user = userService.getUserById(id);

        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {

        User createdUser = userService.createUser(request);
        realtimeEventService.publishAfterCommit("users");

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(createdUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {

        User updatedUser = userService.updateUser(id, request);
        realtimeEventService.publishAfterCommit("users");

        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);
        realtimeEventService.publishAfterCommit("users");

        return ResponseEntity.noContent().build();
    }
}