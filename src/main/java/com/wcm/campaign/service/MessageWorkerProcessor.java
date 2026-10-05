package com.wcm.campaign.service;

import com.wcm.campaign.model.Contact;
import com.wcm.campaign.model.Message;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MessageWorkerProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MessageWorkerProcessor.class);

    private final MessageRepository messageRepository;
    private final WhatsAppProvider whatsappProvider;

    @Value("${whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    public MessageWorkerProcessor(
            MessageRepository messageRepository,
            WhatsAppProvider whatsappProvider
    ) {
        this.messageRepository = messageRepository;
        this.whatsappProvider = whatsappProvider;
    }

    @Transactional
    public void processMessage(Long messageId) {

        logger.info("Processing message {}", messageId);

        Message message = messageRepository.findById(messageId).orElse(null);

        if (message == null) {
            logger.warn("Message {} was not found", messageId);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(message.getStatus())) {
            logger.info("Message {} skipped. Current status: {}", messageId, message.getStatus());
            return;
        }

        if (!whatsappEnabled) {
            logger.info("WhatsApp provider is disabled. Message {} remains PENDING", messageId);
            return;
        }

        Contact contact = message.getContact();

        if (contact == null) {
            logger.error("Message {} does not have a contact", messageId);
            markAsFailed(message, null, "Message does not have a contact");
            return;
        }

        Template template = message.getTemplate();

        if (template == null) {
            logger.error("Message {} does not have a template", messageId);
            markAsFailed(message, null, "Message does not have a template");
            return;
        }

        String phoneNumber = contact.getContactNumbers();

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.error("Message {} contact does not have a phone number", messageId);
            markAsFailed(message, null, "Contact does not have a phone number");
            return;
        }

        String templateName = template.getTemplateName();

        if (templateName == null || templateName.trim().isEmpty()) {
            logger.error("Message {} template does not have a template name", messageId);
            markAsFailed(message, null, "Template does not have a template name");
            return;
        }

        String language = template.getLanguage();

        if (language == null || language.trim().isEmpty()) {
            language = "en_US";
        }

        try {
            logger.info("Worker {} processing message {}", Thread.currentThread().getName(), messageId);

            WhatsAppSendResult result = whatsappProvider.sendTemplateMessage(phoneNumber, templateName, language);

            if (result == null) {
                logger.error("WhatsApp provider returned no response for message {}", messageId);
                markAsFailed(message, null, "WhatsApp provider returned no response");
                return;
            }

            if (!result.isSuccess()) {
                logger.warn("WhatsApp provider failed for message {}", messageId);
                markAsFailed(message, result.getFailureCode(), result.getFailureReason());
                return;
            }

            String providerMessageId = result.getProviderMessageId();

            if (providerMessageId == null || providerMessageId.trim().isEmpty()) {
                logger.error("WhatsApp provider returned success without a message ID for message {}", messageId);
                markAsFailed(message, null, "Provider returned success without a message ID");
                return;
            }

            message.setProviderMessageId(providerMessageId);
            message.setStatus("SENT");
            message.setSentAt(LocalDateTime.now());
            messageRepository.save(message);

            logger.info("Message {} SENT successfully", messageId);

        } catch (Exception exception) {
            logger.error("Message {} failed during processing", messageId, exception);
            markAsFailed(message, null, exception.getMessage());
        }
    }

    private void markAsFailed(Message message, Integer failureCode, String failureReason) {

        message.setStatus("FAILED");
        message.setFailedAt(LocalDateTime.now());
        message.setFailureCode(failureCode);
        message.setFailureReason(failureReason);
        messageRepository.save(message);

        logger.error("Message {} marked as FAILED. Reason: {}", message.getMessageId(), failureReason);
    }
}