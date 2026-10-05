package com.wcm.campaign.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "whatsapp.provider",
        havingValue = "fake",
        matchIfMissing = true
)
public class WhatsAppFakeProvider implements WhatsAppProvider {

    @Override
    public WhatsAppSendResult sendTemplateMessage(
            String phoneNumber,
            String templateName,
            String language
    ) {
        return WhatsAppSendResult.failure(
                null,
                "WhatsApp provider is disabled for this environment"
        );
    }
}
