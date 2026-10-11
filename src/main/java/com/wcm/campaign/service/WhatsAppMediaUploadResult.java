package com.wcm.campaign.service;

public record WhatsAppMediaUploadResult(
        boolean success,
        String mediaId,
        String failureReason
) {

    public static WhatsAppMediaUploadResult success(String mediaId) {
        return new WhatsAppMediaUploadResult(true, mediaId, null);
    }

    public static WhatsAppMediaUploadResult failure(String reason) {
        return new WhatsAppMediaUploadResult(false, null, reason);
    }
}
