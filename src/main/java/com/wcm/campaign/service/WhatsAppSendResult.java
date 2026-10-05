package com.wcm.campaign.service;

public class WhatsAppSendResult {

    private final boolean success;

    private final String providerMessageId;

    private final Integer failureCode;

    private final String failureReason;

    private WhatsAppSendResult(
            boolean success,
            String providerMessageId,
            Integer failureCode,
            String failureReason
    ) {

        this.success = success;
        this.providerMessageId = providerMessageId;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
    }

    public static WhatsAppSendResult success(
            String providerMessageId
    ) {

        return new WhatsAppSendResult(
                true,
                providerMessageId,
                null,
                null
        );
    }

    public static WhatsAppSendResult failure(
            Integer failureCode,
            String failureReason
    ) {

        return new WhatsAppSendResult(
                false,
                null,
                failureCode,
                failureReason
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public Integer getFailureCode() {
        return failureCode;
    }

    public String getFailureReason() {
        return failureReason;
    }
}