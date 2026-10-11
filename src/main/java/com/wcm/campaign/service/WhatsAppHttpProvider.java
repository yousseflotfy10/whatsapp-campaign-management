
package com.wcm.campaign.service;

import com.wcm.campaign.config.WhatsAppProperties;
import com.wcm.campaign.dto.WhatsAppSendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "whatsapp.provider", havingValue = "http")
public class WhatsAppHttpProvider implements WhatsAppProvider {

    private static final Logger logger =
            LoggerFactory.getLogger(WhatsAppHttpProvider.class);

    private final WhatsAppProperties properties;
    private final RestClient restClient;

    public WhatsAppHttpProvider(WhatsAppProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public WhatsAppSendResult sendTemplateMessage(
            String phoneNumber,
            String templateName,
            String language
    ) {
        if (!properties.isEnabled()) {
            logger.warn("WhatsApp send skipped: provider is disabled");
            return WhatsAppSendResult.failure(
                    null, "WhatsApp provider is disabled"
            );
        }

        if (properties.getBaseUrl() == null
                || properties.getBaseUrl().isBlank()) {
            logger.error("WhatsApp send rejected: provider URL is missing");
            return WhatsAppSendResult.failure(
                    null, "WhatsApp provider URL is not configured"
            );
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

        String url = properties.getBaseUrl() + "/"
                + properties.getPhoneNumberId() + "/messages";

        try {
            logger.info(
                    "Sending template message: url={}, phoneNumberId={}, template={}, language={}",
                    url,
                    properties.getPhoneNumberId(),
                    templateName,
                    language
            );

            WhatsAppSendResponse response = restClient.post()
                    .uri(url)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + properties.getAccessToken()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(WhatsAppSendResponse.class);

            if (response == null
                    || response.getMessages() == null
                    || response.getMessages().isEmpty()) {
                logger.error(
                        "Simulator returned an empty message response: template={}",
                        templateName
                );
                return WhatsAppSendResult.failure(
                        null, "Provider returned an empty response"
                );
            }

            String providerMessageId = response.getMessages().get(0).getId();

            if (providerMessageId == null || providerMessageId.isBlank()) {
                logger.error(
                        "Simulator response has no message ID: template={}",
                        templateName
                );
                return WhatsAppSendResult.failure(
                        null, "Provider did not return message ID"
                );
            }

            logger.info(
                    "Simulator accepted message: providerMessageId={}, template={}",
                    providerMessageId,
                    templateName
            );

            return WhatsAppSendResult.success(providerMessageId);

        } catch (RestClientResponseException exception) {
            String reason = providerError(exception);

            logger.error(
                    "Simulator rejected message: template={}, status={}, reason={}",
                    templateName,
                    exception.getStatusCode().value(),
                    reason
            );

            return WhatsAppSendResult.failure(
                    exception.getStatusCode().value(), reason
            );

        } catch (Exception exception) {
            logger.error(
                    "Message request failed: template={}",
                    templateName,
                    exception
            );

            return WhatsAppSendResult.failure(
                    null, exception.getMessage()
            );
        }
    }

    @Override
    public WhatsAppTemplateSubmissionResult submitTemplate(
            String templateName,
            String content,
            String language,
            String category
    ) {
        if (!properties.isEnabled()) {
            logger.warn("Template submission skipped: provider is disabled");
            return WhatsAppTemplateSubmissionResult.failure(
                    "WhatsApp provider is disabled"
            );
        }

        if (properties.getBaseUrl() == null
                || properties.getBaseUrl().isBlank()
                || properties.getWabaId() == null
                || properties.getWabaId().isBlank()) {
            logger.error(
                    "Template submission rejected: provider URL or WABA ID is missing"
            );
            return WhatsAppTemplateSubmissionResult.failure(
                    "WhatsApp template API configuration is incomplete"
            );
        }

        Map<String, Object> request = Map.of(
                "name", templateName,
                "language", language,
                "category", category,
                "components", List.of(
                        Map.of(
                                "type", "BODY",
                                "text", content
                        )
                )
        );

        String url = properties.getBaseUrl() + "/"
                + properties.getWabaId() + "/message_templates";

        try {
            logger.info(
                    "Submitting template: url={}, wabaId={}, template={}, language={}, category={}",
                    url,
                    properties.getWabaId(),
                    templateName,
                    language,
                    category
            );

            TemplateSubmissionResponse response = restClient.post()
                    .uri(url)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + properties.getAccessToken()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TemplateSubmissionResponse.class);

            if (response == null) {
                logger.error(
                        "Template submission returned an empty response: template={}",
                        templateName
                );
                return WhatsAppTemplateSubmissionResult.failure(
                        "Simulator returned an empty response"
                );
            }

            logger.info(
                    "Template submission response: template={}, providerTemplateId={}, providerStatus={}",
                    templateName,
                    response.id,
                    response.status
            );

            if (response.id == null || response.id.isBlank()) {
                logger.error(
                        "Template submission response has no ID: template={}",
                        templateName
                );
                return WhatsAppTemplateSubmissionResult.failure(
                        "Simulator returned no template ID"
                );
            }

            String status = response.status == null
                    ? "PENDING"
                    : response.status;

            logger.info(
                    "Template submission accepted: template={}, providerTemplateId={}, status={}",
                    templateName,
                    response.id,
                    status
            );

            return new WhatsAppTemplateSubmissionResult(
                    true,
                    response.id,
                    status,
                    null
            );

        } catch (RestClientResponseException exception) {
            String reason = providerError(exception);

            logger.error(
                    "Simulator rejected template submission: template={}, status={}, reason={}",
                    templateName,
                    exception.getStatusCode().value(),
                    reason
            );

            return WhatsAppTemplateSubmissionResult.failure(reason);

        } catch (Exception exception) {
            logger.error(
                    "Template submission failed before a valid response: template={}",
                    templateName,
                    exception
            );

            return WhatsAppTemplateSubmissionResult.failure(
                    exception.getMessage()
            );
        }
    }

    @Override
    public List<WhatsAppProviderTemplate> listTemplates() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException(
                    "WhatsApp provider is disabled"
            );
        }

        if (properties.getBaseUrl() == null
                || properties.getBaseUrl().isBlank()
                || properties.getWabaId() == null
                || properties.getWabaId().isBlank()) {
            throw new IllegalStateException(
                    "WhatsApp template API configuration is incomplete"
            );
        }

        String url = properties.getBaseUrl() + "/"
                + properties.getWabaId() + "/message_templates";

        try {
            logger.info(
                    "Synchronizing templates from provider: url={}, wabaId={}",
                    url,
                    properties.getWabaId()
            );

            TemplateListResponse response = restClient.get()
                    .uri(url)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + properties.getAccessToken()
                    )
                    .retrieve()
                    .body(TemplateListResponse.class);

            if (response == null || response.data == null) {
                logger.warn(
                        "Provider returned no template data for wabaId={}",
                        properties.getWabaId()
                );
                return Collections.emptyList();
            }

            List<WhatsAppProviderTemplate> templates =
                    Arrays.stream(response.data)
                            .map(template -> new WhatsAppProviderTemplate(
                                    template.id,
                                    template.name,
                                    template.language,
                                    template.category,
                                    template.status
                            ))
                            .toList();

            logger.info(
                    "Provider template synchronization completed: count={}",
                    templates.size()
            );

            templates.forEach(template ->
                    logger.info(
                            "Provider template: id={}, name={}, language={}, status={}",
                            template.id(),
                            template.name(),
                            template.language(),
                            template.status()
                    )
            );

            return templates;

        } catch (RestClientResponseException exception) {
            String reason = providerError(exception);

            logger.error(
                    "Provider template synchronization failed: status={}, reason={}",
                    exception.getStatusCode().value(),
                    reason
            );

            throw new IllegalStateException(reason, exception);

        } catch (Exception exception) {
            logger.error(
                    "Unexpected error during provider template synchronization",
                    exception
            );

            throw new IllegalStateException(
                    "Provider template synchronization failed",
                    exception
            );
        }
    }

    @Override
    public WhatsAppMediaUploadResult uploadMedia(
            byte[] content,
            String filename,
            String contentType
    ) {
        if (!properties.isEnabled()) {
            logger.warn("Media upload skipped: provider is disabled");
            return WhatsAppMediaUploadResult.failure(
                    "WhatsApp provider is disabled"
            );
        }

        if (content == null || content.length == 0) {
            logger.warn("Media upload rejected: file is empty");
            return WhatsAppMediaUploadResult.failure(
                    "Media file is empty"
            );
        }

        try {
            LinkedMultiValueMap<String, Object> form =
                    new LinkedMultiValueMap<>();

            form.add("messaging_product", "whatsapp");

            ByteArrayResource resource = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            HttpHeaders fileHeaders = new HttpHeaders();

            if (contentType != null && !contentType.isBlank()) {
                fileHeaders.setContentType(
                        MediaType.parseMediaType(contentType)
                );
            }

            form.add("file", new HttpEntity<>(resource, fileHeaders));

            String url = properties.getBaseUrl() + "/"
                    + properties.getPhoneNumberId() + "/media";

            logger.info(
                    "Uploading media: url={}, filename={}, sizeBytes={}, contentType={}",
                    url,
                    filename,
                    content.length,
                    contentType
            );

            MediaUploadResponse response = restClient.post()
                    .uri(url)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + properties.getAccessToken()
                    )
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(MediaUploadResponse.class);

            if (response == null
                    || response.id == null
                    || response.id.isBlank()) {
                logger.error(
                        "Media upload response has no media ID: filename={}",
                        filename
                );
                return WhatsAppMediaUploadResult.failure(
                        "Simulator returned no media ID"
                );
            }

            logger.info(
                    "Media upload completed: filename={}, mediaId={}",
                    filename,
                    response.id
            );

            return WhatsAppMediaUploadResult.success(response.id);

        } catch (RestClientResponseException exception) {
            String reason = providerError(exception);

            logger.error(
                    "Media upload rejected: filename={}, status={}, reason={}",
                    filename,
                    exception.getStatusCode().value(),
                    reason
            );

            return WhatsAppMediaUploadResult.failure(reason);

        } catch (Exception exception) {
            logger.error(
                    "Media upload failed: filename={}",
                    filename,
                    exception
            );

            return WhatsAppMediaUploadResult.failure(
                    exception.getMessage()
            );
        }
    }

    private String providerError(
            RestClientResponseException exception
    ) {
        String responseBody = exception.getResponseBodyAsString();

        return responseBody == null || responseBody.isBlank()
                ? "HTTP " + exception.getStatusCode().value()
                + ": " + exception.getStatusText()
                : "HTTP " + exception.getStatusCode().value()
                + ": " + responseBody;
    }

    private static class TemplateSubmissionResponse {
        private String id;
        private String status;

        public void setId(String id) {
            this.id = id;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    private static class TemplateListResponse {
        private TemplateSummary[] data;

        public void setData(TemplateSummary[] data) {
            this.data = data;
        }
    }

    private static class TemplateSummary {
        private String id;
        private String name;
        private String language;
        private String category;
        private String status;

        public void setId(String id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    private static class MediaUploadResponse {
        private String id;

        public void setId(String id) {
            this.id = id;
        }
    }
}
