package com.wcm.campaign.controller;

import com.wcm.campaign.dto.AccountRequestResponse;
import com.wcm.campaign.dto.SignupRequest;
import com.wcm.campaign.dto.UserResponse;
import com.wcm.campaign.model.AccountRequest;
import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.Role;
import com.wcm.campaign.model.User;
import com.wcm.campaign.service.AccountRequestService;
import com.wcm.campaign.service.RealtimeEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/account-requests")
public class AccountRequestController {
    private final AccountRequestService accountRequestService;
    private final RealtimeEventService realtimeEventService;

    public AccountRequestController(
            AccountRequestService accountRequestService,
            RealtimeEventService realtimeEventService
    ) {
        this.accountRequestService = accountRequestService;
        this.realtimeEventService = realtimeEventService;
    }

    @PostMapping
    public ResponseEntity<String> submitRequest(@Valid @RequestBody SignupRequest request) {
        accountRequestService.submitRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Account request submitted successfully. " + "Please wait for administrator approval.");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<List<AccountRequestResponse>> getPendingRequests() {
        List<AccountRequestResponse> requests = accountRequestService
                        .getPendingRequests()
                        .stream()
                        .map(AccountRequestResponse::fromRequest)
                        .collect(Collectors.toList());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<AccountRequestResponse> getRequestById(@PathVariable Long id) {
        AccountRequest request = accountRequestService.getRequestById(id);
        return ResponseEntity.ok(AccountRequestResponse.fromRequest(request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserResponse> approveRequest(@PathVariable Long id, @RequestParam Role role, @RequestBody(required = false) List<Permission> permissions) {
        User user = accountRequestService.approveRequest(id, role, permissions);
        realtimeEventService.publishAfterCommit("requests");
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(user));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<AccountRequestResponse> rejectRequest(@PathVariable Long id) {
        AccountRequest request = accountRequestService.rejectRequest(id);
        realtimeEventService.publishAfterCommit("requests");
        return ResponseEntity.ok(AccountRequestResponse.fromRequest(request));
    }
}