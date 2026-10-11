
package com.wcm.campaign.service;

import com.wcm.campaign.model.Campaign;
import com.wcm.campaign.model.Contact;
import com.wcm.campaign.model.Message;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.repository.CampaignRepository;
import com.wcm.campaign.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger logger =
            LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final CampaignRepository campaignRepository;
    private final ContactService contactService;
    private final TemplateService templateService;
    private final MessageWorkerService messageWorkerService;

    public MessageService(
            MessageRepository messageRepository,
            CampaignRepository campaignRepository,
            ContactService contactService,
            TemplateService templateService,
            MessageWorkerService messageWorkerService
    ) {
        this.messageRepository = messageRepository;
        this.campaignRepository = campaignRepository;
        this.contactService = contactService;
        this.templateService = templateService;
        this.messageWorkerService = messageWorkerService;
    }

    @Transactional
    public void generateMessagesForCampaign(Long campaignId) {
        logger.info("Starting message generation for campaign {}", campaignId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> {
                    logger.error("Campaign not found. Id={}", campaignId);
                    return new RuntimeException(
                            "Campaign not found with id: " + campaignId);
                });

        logger.info(
                "Campaign loaded. Id={}, Status={}",
                campaignId, campaign.getStatus());

        if (!"APPROVED".equals(campaign.getStatus())) {
            logger.warn(
                    "Message generation rejected. Campaign={}, Status={}",
                    campaignId, campaign.getStatus());
            throw new IllegalStateException(
                    "Messages can only be generated for an APPROVED campaign");
        }

        if (campaign.getTemplate() == null) {
            logger.error("Campaign {} has no template", campaignId);
            throw new IllegalStateException(
                    "Campaign does not have a template");
        }

        Template template = campaign.getTemplate();

        logger.info(
                "Campaign template loaded. Campaign={}, TemplateId={}, TemplateName={}, TemplateStatus={}",
                campaignId,
                template.getTemplateId(),
                template.getTemplateName(),
                template.getStatus());

        String listIdString = campaign.getListId();

        if (listIdString == null || listIdString.trim().isEmpty()) {
            logger.error("Campaign {} has no contact list", campaignId);
            throw new IllegalStateException(
                    "Campaign does not have a contact list");
        }

        Set<String> campaignListIds = Arrays.stream(listIdString.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());

        logger.info(
                "Campaign recipient lists parsed. Campaign={}, ListCount={}, ListIds={}",
                campaignId, campaignListIds.size(), campaignListIds);

        List<Contact> allContacts = contactService.getAllContacts();

        logger.info(
                "Retrieved {} contacts before campaign filtering",
                allContacts.size());

        List<Contact> contacts = allContacts.stream()
                .filter(Contact::getIsActive)
                .filter(contact -> contact.getContactLists().stream()
                        .anyMatch(contactList ->
                                campaignListIds.contains(contactList.getListId())))
                .toList();

        logger.info(
                "Recipient filtering completed. Campaign={}, ActiveRecipients={}",
                campaignId, contacts.size());

        if (contacts.isEmpty()) {
            logger.warn(
                    "No active contacts found for campaign {}",
                    campaignId);
            throw new IllegalStateException(
                    "No active contacts found for this campaign");
        }

        List<Long> messageIds = new ArrayList<>();

        for (Contact contact : contacts) {
            logger.debug(
                    "Creating message. Campaign={}, ContactId={}",
                    campaignId, contact.getContactId());

            Message message = new Message();
            message.setCampaign(campaign);
            message.setContact(contact);
            message.setTemplate(template);
            message.setMessageContent(
                    generateMessageContent(
                            template.getTemplateContent(), contact));
            message.setMessageType("TEMPLATE");
            message.setStatus("PENDING");

            Message savedMessage = messageRepository.save(message);
            messageIds.add(savedMessage.getMessageId());

            logger.debug(
                    "Message created. MessageId={}, Campaign={}, ContactId={}, Status={}",
                    savedMessage.getMessageId(),
                    campaignId,
                    contact.getContactId(),
                    savedMessage.getStatus());
        }

        logger.info(
                "Message generation completed. Campaign={}, GeneratedCount={}",
                campaignId, messageIds.size());

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            logger.error(
                    "Transaction synchronization is unavailable for campaign {}. Messages were saved, but worker submission was not scheduled",
                    campaignId);

            throw new IllegalStateException(
                    "Transaction synchronization is required to submit messages after commit");
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        logger.info(
                                "Campaign transaction committed. Submitting {} messages to workers. Campaign={}",
                                messageIds.size(), campaignId);

                        for (Long messageId : messageIds) {
                            try {
                                logger.debug(
                                        "Submitting message {} to worker",
                                        messageId);

                                messageWorkerService.submit(messageId);

                                logger.debug(
                                        "Message {} submitted to worker",
                                        messageId);
                            } catch (Exception exception) {
                                logger.error(
                                        "Failed to submit message {} to worker. Campaign={}",
                                        messageId, campaignId, exception);
                            }
                        }

                        logger.info(
                                "Worker submission loop completed. Campaign={}",
                                campaignId);
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            logger.warn(
                                    "Campaign transaction did not commit. Campaign={}, TransactionStatus={}",
                                    campaignId, status);
                        }
                    }
                });
    }

    @Transactional(readOnly = true)
    public List<Message> getAllMessages() {
        logger.info("Fetching all messages");

        List<Message> messages = messageRepository.findAll();

        logger.info("Retrieved {} messages", messages.size());

        return messages;
    }

    @Transactional(readOnly = true)
    public Message getMessageById(Long id) {
        logger.info("Fetching message. Id={}", id);

        return messageRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Message not found. Id={}", id);
                    return new RuntimeException(
                            "Message not found with id: " + id);
                });
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByCampaign(Long campaignId) {
        logger.info("Fetching messages for campaign {}", campaignId);

        List<Message> messages =
                messageRepository.findByCampaign_CampaignId(campaignId);

        logger.info(
                "Retrieved {} messages for campaign {}",
                messages.size(), campaignId);

        return messages;
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByContact(Long contactId) {
        logger.info("Fetching messages for contact {}", contactId);

        List<Message> messages =
                messageRepository.findByContact_ContactId(contactId);

        logger.info(
                "Retrieved {} messages for contact {}",
                messages.size(), contactId);

        return messages;
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            logger.warn("Message status filter is missing");
            throw new IllegalArgumentException(
                    "Message status is required");
        }

        String normalizedStatus = status.trim().toUpperCase();

        logger.info(
                "Fetching messages by status {}",
                normalizedStatus);

        List<Message> messages =
                messageRepository.findByStatus(normalizedStatus);

        logger.info(
                "Retrieved {} messages with status {}",
                messages.size(), normalizedStatus);

        return messages;
    }

    @Transactional
    public Message updateStatus(Long id, String status) {
        if (status == null || status.trim().isEmpty()) {
            logger.warn(
                    "Message status update rejected because status is missing. Id={}",
                    id);
            throw new IllegalArgumentException(
                    "Message status is required");
        }

        String normalizedStatus = status.trim().toUpperCase();

        logger.info(
                "Message status update requested. Id={}, RequestedStatus={}",
                id, normalizedStatus);

        return switch (normalizedStatus) {
            case "SENT" -> markAsSent(id);
            case "DELIVERED" -> markAsDelivered(id);
            case "FAILED" -> markAsFailed(id);
            case "PENDING" -> {
                Message message = getMessageById(id);

                if (!"PENDING".equals(message.getStatus())) {
                    logger.warn(
                            "Message cannot be set to PENDING. Id={}, CurrentStatus={}",
                            id, message.getStatus());

                    throw new IllegalStateException(
                            "Only a new message can be set to PENDING");
                }

                logger.info(
                        "Message {} is already PENDING",
                        id);

                yield message;
            }
            default -> {
                logger.warn(
                        "Invalid message status requested. Id={}, Status={}",
                        id, status);

                throw new IllegalArgumentException(
                        "Invalid message status: " + status);
            }
        };
    }

    @Transactional
    public void applyProviderStatus(
            String providerMessageId,
            String status,
            String failureReason
    ) {
        logger.info(
                "Provider message status callback received. ProviderMessageId={}, Status={}",
                providerMessageId, status);

        if (providerMessageId == null || providerMessageId.isBlank()
                || status == null || status.isBlank()) {
            logger.error(
                    "Provider message status callback ignored because message ID or status is missing");
            return;
        }

        var messageOptional =
                messageRepository.findByProviderMessageId(providerMessageId);

        if (messageOptional.isEmpty()) {
            logger.warn(
                    "No local message matches provider message ID {}",
                    providerMessageId);
            return;
        }

        Message message = messageOptional.get();
        String previousStatus = message.getStatus();
        String normalizedStatus = status.trim().toUpperCase();

        logger.info(
                "Matching message found. MessageId={}, PreviousStatus={}, ProviderStatus={}",
                message.getMessageId(), previousStatus, normalizedStatus);

        message.setStatus(normalizedStatus);

        LocalDateTime now = LocalDateTime.now();

        switch (normalizedStatus) {
            case "SENT" -> message.setSentAt(now);
            case "DELIVERED" -> message.setDeliveredAt(now);
            case "READ" -> message.setReadAt(now);
            case "FAILED" -> {
                message.setFailedAt(now);
                message.setFailureReason(failureReason);
            }
            default -> logger.warn(
                    "Provider returned an unrecognized message status. MessageId={}, Status={}",
                    message.getMessageId(), normalizedStatus);
        }

        messageRepository.save(message);

        logger.info(
                "Provider message status saved. MessageId={}, ProviderMessageId={}, OldStatus={}, NewStatus={}",
                message.getMessageId(),
                providerMessageId,
                previousStatus,
                normalizedStatus);
    }

    @Transactional
    public Message markAsSent(Long id) {
        logger.info("Marking message as SENT. Id={}", id);

        Message message = getMessageById(id);

        if (!"PENDING".equals(message.getStatus())) {
            logger.warn(
                    "Cannot mark message as SENT. Id={}, CurrentStatus={}",
                    id, message.getStatus());

            throw new IllegalStateException(
                    "Only PENDING messages can be marked as SENT");
        }

        message.setStatus("SENT");
        message.setSentAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        logger.info("Message marked as SENT. Id={}", id);

        return savedMessage;
    }

    @Transactional
    public Message markAsDelivered(Long id) {
        logger.info("Marking message as DELIVERED. Id={}", id);

        Message message = getMessageById(id);

        if (!"SENT".equals(message.getStatus())) {
            logger.warn(
                    "Cannot mark message as DELIVERED. Id={}, CurrentStatus={}",
                    id, message.getStatus());

            throw new IllegalStateException(
                    "Only SENT messages can be marked as DELIVERED");
        }

        message.setStatus("DELIVERED");
        message.setDeliveredAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        logger.info("Message marked as DELIVERED. Id={}", id);

        return savedMessage;
    }

    @Transactional
    public Message markAsFailed(Long id) {
        logger.info("Marking message as FAILED. Id={}", id);

        Message message = getMessageById(id);

        if (!"PENDING".equals(message.getStatus())
                && !"SENT".equals(message.getStatus())) {
            logger.warn(
                    "Cannot mark message as FAILED. Id={}, CurrentStatus={}",
                    id, message.getStatus());

            throw new IllegalStateException(
                    "Only PENDING or SENT messages can be marked as FAILED");
        }

        message.setStatus("FAILED");
        message.setFailedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        logger.info("Message marked as FAILED. Id={}", id);

        return savedMessage;
    }

    private String generateMessageContent(
            String templateContent,
            Contact contact
    ) {
        if (templateContent == null) {
            logger.warn(
                    "Template content is null while generating message for contact {}",
                    contact.getContactId());
            return "";
        }

        String contactName = contact.getContactName();

        if (contactName == null) {
            contactName = "";
        }

        String content = templateContent.replace("{{name}}", contactName);

        logger.debug(
                "Message content generated. ContactId={}",
                contact.getContactId());

        return content;
    }
}
