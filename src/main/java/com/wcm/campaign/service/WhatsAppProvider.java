package com.wcm.campaign.service;

public interface WhatsAppProvider {

    WhatsAppSendResult sendTemplateMessage(
            String phoneNumber,
            String templateName,
            String language
    );
}