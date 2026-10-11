package com.wcm.campaign.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wcm.campaign.config.WhatsAppProperties;
import com.wcm.campaign.service.MessageService;
import com.wcm.campaign.service.RealtimeEventService;
import com.wcm.campaign.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class WhatsAppWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TemplateService templateService;
    private final MessageService messageService;
    private final RealtimeEventService realtimeEventService;

    public WhatsAppWebhookController(
            WhatsAppProperties properties,
            TemplateService templateService,
            MessageService messageService,
            RealtimeEventService realtimeEventService
    ) {
        this.properties = properties;
        this.templateService = templateService;
        this.messageService = messageService;
        this.realtimeEventService = realtimeEventService;
    }

    @GetMapping({"/webhook", "/api/whatsapp/webhook", "/api/webhook", "/{phoneNumberId}/webhook"})
    public ResponseEntity<String> verify(
            @PathVariable(name = "phoneNumberId", required = false) String phoneNumberId,
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if (phoneNumberId != null && !phoneNumberId.equals(properties.getPhoneNumberId())) {
            logger.warn("Webhook verification rejected for unexpected phone number ID: {}", phoneNumberId);
            return ResponseEntity.notFound().build();
        }

        if (mode == null) {
            logger.info("Webhook endpoint accessed via GET without handshake parameters; returning active status");
            return ResponseEntity.ok("WhatsApp Webhook endpoint is active and listening for events.");
        }

        if ("subscribe".equals(mode)
                && properties.getWebhookSecret() != null
                && properties.getWebhookSecret().equals(verifyToken)) {
            logger.info("Webhook verification succeeded");
            return ResponseEntity.ok(challenge);
        }

        logger.warn("Webhook verification failed: mode={}", mode);
        return ResponseEntity.status(403).body("Webhook verification failed");
    }

    @PostMapping({"/", "/webhook", "/api/whatsapp/webhook", "/api/webhook", "/{phoneNumberId}/webhook"})
    public ResponseEntity<Void> receive(
            @PathVariable(name = "phoneNumberId", required = false) String phoneNumberId,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] rawBody
    ) {
        if (phoneNumberId != null && !phoneNumberId.equals(properties.getPhoneNumberId())) {
            logger.warn("Webhook rejected for unexpected phone number ID: {}", phoneNumberId);
            return ResponseEntity.notFound().build();
        }

        logger.info("Webhook POST received: pathPhoneNumberId={}", phoneNumberId == null ? "not supplied" : phoneNumberId);

        boolean signatureValid = isValidSignature(signature, rawBody);
        if (properties.isWebhookSignatureValidationEnabled() && !signatureValid) {
            logger.warn("Webhook rejected because signature validation failed");
            return ResponseEntity.status(403).build();
        }
        if (!signatureValid) {
            logger.warn("Webhook signature mismatch accepted because strict validation is disabled for the simulator");
        }

        final JsonNode payload;
        try {
            payload = objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            logger.warn("Webhook rejected because payload is invalid JSON", exception);
            return ResponseEntity.badRequest().build();
        }

        logger.info("Webhook payload parsed successfully");
        JsonNode entries = payload.path("entry");

        for (JsonNode entry : entries) {
            for (JsonNode change : entry.path("changes")) {
                JsonNode value = change.path("value");

                for (JsonNode status : value.path("statuses")) {
                    String providerMessageId = status.path("id").asText(null);
                    String messageStatus = status.path("status").asText(null);
                    String failureReason = status.path("errors").toString();

                    if (providerMessageId != null && messageStatus != null) {
                        logger.info("Processing simulator message status: providerMessageId={}, status={}",
                                providerMessageId, messageStatus);
                        messageService.applyProviderStatus(
                                providerMessageId,
                                messageStatus,
                                failureReason
                        );
                    }
                }

                JsonNode templateUpdate = value.path("message_template_status_update");
                if (templateUpdate.isObject()) {
                    String templateName = templateUpdate.path("message_template_name").asText(null);
                    String event = templateUpdate.path("event").asText(null);

                    if (templateName != null && event != null) {
                        logger.info("Processing simulator template status: template={}, status={}",
                                templateName, event);
                        templateService.applyProviderStatus(templateName, event);
                    } else {
                        logger.warn("Template status webhook missing template name or event");
                    }
                }
            }
        }

        realtimeEventService.publishAfterCommit("templates");
        realtimeEventService.publishAfterCommit("messages");

        logger.info("Webhook processing completed successfully");
        return ResponseEntity.ok().build();
    }

    private boolean isValidSignature(String signature, byte[] rawBody) {
        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank() || signature == null || !signature.startsWith("sha256=")) {
            logger.warn("Webhook signature is missing or malformed");
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody);
            String expected = "sha256=" + java.util.HexFormat.of().formatHex(digest);
            boolean valid = MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );

            if (valid) {
                logger.info("Webhook signature accepted");
            } else {
                logger.warn("Webhook signature mismatch");
            }

            return valid;
        } catch (Exception exception) {
            logger.error("Webhook signature calculation failed", exception);
            return false;
        }
    }
}
