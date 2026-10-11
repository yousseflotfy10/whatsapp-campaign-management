package com.wcm.campaign.service;

public record WhatsAppTemplateSubmissionResult(
        boolean success,
        String providerTemplateId,
        String status,
        String failureReason
) {
    public static WhatsAppTemplateSubmissionResult failure(String reason) {
        return new WhatsAppTemplateSubmissionResult(false, null, null, reason);
    }
}
