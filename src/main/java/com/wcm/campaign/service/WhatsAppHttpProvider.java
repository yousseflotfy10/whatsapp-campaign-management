package com.wcm.campaign.service;

import com.wcm.campaign.config.WhatsAppProperties;
import com.wcm.campaign.dto.WhatsAppSendResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "whatsapp.provider", havingValue = "http")
public class WhatsAppHttpProvider implements WhatsAppProvider {

    private final WhatsAppProperties properties;
    private final RestClient restClient;

    public WhatsAppHttpProvider(WhatsAppProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public WhatsAppSendResult sendTemplateMessage(String phoneNumber, String templateName, String language) {

        if (!properties.isEnabled()) {
            return WhatsAppSendResult.failure(null, "WhatsApp provider is disabled");
        }

        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            return WhatsAppSendResult.failure(null, "WhatsApp provider URL is not configured");
        }

        Map<String, Object> request = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", phoneNumber,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", language)
                )
        );

        try {
            String url = properties.getBaseUrl() + "/" + properties.getPhoneNumberId() + "/messages";

            WhatsAppSendResponse response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(WhatsAppSendResponse.class);

            if (response == null || response.getMessages() == null || response.getMessages().isEmpty()) {
                return WhatsAppSendResult.failure(null, "Provider returned an empty response");
            }

            String providerMessageId = response.getMessages().get(0).getId();

            if (providerMessageId == null || providerMessageId.isBlank()) {
                return WhatsAppSendResult.failure(null, "Provider did not return message ID");
            }

            return WhatsAppSendResult.success(providerMessageId);

        } catch (Exception exception) {
            return WhatsAppSendResult.failure(null, exception.getMessage());
        }
    }
}
