package com.wcm.campaign.controller;

import com.wcm.campaign.dto.UserRequest;
import com.wcm.campaign.dto.UserResponse;
import com.wcm.campaign.model.User;
import com.wcm.campaign.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {

        User user = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        List<UserResponse> users = userService.getAllUsers().stream().map(UserResponse::new).toList();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {

        User user = userService.getUserById(id);

        return ResponseEntity.ok(new UserResponse(user));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {

        User user = userService.getUserByUsername(username);

        return ResponseEntity.ok(new UserResponse(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {

        User user = userService.updateUser(id, request);

        return ResponseEntity.ok(new UserResponse(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}