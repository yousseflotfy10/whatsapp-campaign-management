package com.wcm.campaign.service;

import java.util.List;

public interface WhatsAppProvider {

    WhatsAppSendResult sendTemplateMessage(
            String phoneNumber,
            String templateName,
            String language
    );

    WhatsAppTemplateSubmissionResult submitTemplate(
            String templateName,
            String content,
            String language,
            String category
    );

    List<WhatsAppProviderTemplate> listTemplates();

    WhatsAppMediaUploadResult uploadMedia(
            byte[] content,
            String filename,
            String contentType
    );
}