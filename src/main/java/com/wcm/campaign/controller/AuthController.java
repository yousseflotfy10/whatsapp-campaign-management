package com.wcm.campaign.controller;

import com.wcm.campaign.dto.LoginRequest;
import com.wcm.campaign.dto.SignupRequest;
import com.wcm.campaign.dto.UserResponse;
import com.wcm.campaign.model.User;
import com.wcm.campaign.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.signup(request);
        UserResponse response = convertToResponse(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<UserResponse> signin(@Valid @RequestBody LoginRequest request, HttpSession session) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        session.setAttribute("USER_ID", user.getUserId());
        session.setAttribute("USERNAME", user.getUsername());
        session.setAttribute("ROLE", user.getRole());
        return ResponseEntity.ok(convertToResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();

        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setPermissions(user.getPermissions());

        return response;
    }
}