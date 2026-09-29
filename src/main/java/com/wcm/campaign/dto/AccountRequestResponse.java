package com.wcm.campaign.dto;

import com.wcm.campaign.model.AccountRequest;

import java.time.LocalDateTime;

public class AccountRequestResponse {

    private Long requestId;
    private String username;
    private String email;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    public AccountRequestResponse() {
    }

    public AccountRequestResponse(
            Long requestId,
            String username,
            String email,
            String status,
            LocalDateTime requestedAt,
            LocalDateTime processedAt
    ) {
        this.requestId = requestId;
        this.username = username;
        this.email = email;
        this.status = status;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
    }

    public static AccountRequestResponse fromRequest(
            AccountRequest request
    ) {

        return new AccountRequestResponse(
                request.getRequestId(),
                request.getUsername(),
                request.getEmail(),
                request.getStatus().name(),
                request.getRequestedAt(),
                request.getProcessedAt()
        );
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}