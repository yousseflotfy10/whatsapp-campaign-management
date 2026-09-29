package com.wcm.campaign.controller;

import com.wcm.campaign.dto.LoginRequest;
import com.wcm.campaign.dto.SignupRequest;
import com.wcm.campaign.dto.UserResponse;
import com.wcm.campaign.model.User;
import com.wcm.campaign.repository.UserRepository;
import com.wcm.campaign.service.AccountRequestService;
import com.wcm.campaign.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AccountRequestService accountRequestService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthController(
            UserService userService,
            AccountRequestService accountRequestService,
            AuthenticationManager authenticationManager,
            UserRepository userRepository
    ) {
        this.userService = userService;
        this.accountRequestService = accountRequestService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        accountRequestService.submitRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Account request submitted successfully. " + "Please wait for administrator approval.");
    }

    @PostMapping("/signin")
    public ResponseEntity<UserResponse> signin(@Valid @RequestBody LoginRequest request, HttpSession session, HttpServletRequest httpRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        new HttpSessionSecurityContextRepository().saveContext(
                        SecurityContextHolder.getContext(),
                        httpRequest,
                        response
                );

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);

        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();

        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(7));
        userRepository.save(user);

        session.setAttribute("USER_ID", user.getUserId());
        session.setAttribute("USERNAME", user.getUsername());
        session.setAttribute("ROLE", user.getRole());

        UserResponse payload = UserResponse.fromUser(user);
        payload.setAccessToken(accessToken);
        payload.setRefreshToken(refreshToken);

        return ResponseEntity.ok(payload);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpServletRequest request) {
        User user = resolveUser(request);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponse response = UserResponse.fromUser(user);
        response.setAccessToken(user.getAccessToken());
        response.setRefreshToken(user.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserResponse> refresh(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String refreshToken = authorization.substring(7).trim();
        User user = userRepository.findByRefreshToken(refreshToken).orElse(null);

        if (user == null || user.getRefreshTokenExpiresAt() == null || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = UUID.randomUUID().toString();
        user.setAccessToken(accessToken);
        userRepository.save(user);

        UserResponse response = UserResponse.fromUser(user);
        response.setAccessToken(accessToken);
        response.setRefreshToken(user.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session, HttpServletRequest request) {

        session.invalidate();
        SecurityContextHolder.clearContext();

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7).trim();
            userRepository.findByAccessToken(accessToken)
                    .ifPresent(user -> {
                        user.setAccessToken(null);
                        user.setRefreshToken(null);
                        user.setRefreshTokenExpiresAt(null);
                        userRepository.save(user);
                    });
        }

        return ResponseEntity.ok("Logged out successfully");
    }

    private User resolveUser(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.substring(7).trim();
            return userRepository.findByAccessToken(accessToken).orElse(null);
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object userId = session.getAttribute("USER_ID");
            if (userId != null) {
                return userRepository.findById(Long.valueOf(userId.toString())).orElse(null);
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }

        return null;
    }
}