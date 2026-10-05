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

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

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

        logger.info("Generating messages for campaign {}", campaignId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> {
                    logger.warn("Campaign not found with id {}", campaignId);
                    return new RuntimeException("Campaign not found with id: " + campaignId);
                });

        if (!"APPROVED".equals(campaign.getStatus())) {
            logger.warn("Messages cannot be generated for campaign {} because its status is {}", campaignId, campaign.getStatus());
            throw new IllegalStateException("Messages can only be generated for an APPROVED campaign");
        }

        if (campaign.getTemplate() == null) {
            logger.warn("Campaign {} does not have a template", campaignId);
            throw new IllegalStateException("Campaign does not have a template");
        }

        String listIdString = campaign.getListId();

        if (listIdString == null || listIdString.trim().isEmpty()) {
            logger.warn("Campaign {} does not have a contact list", campaignId);
            throw new IllegalStateException("Campaign does not have a contact list");
        }

        Set<String> campaignListIds = Arrays.stream(listIdString.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());

        List<Contact> contacts = contactService.getAllContacts()
                .stream()
                .filter(Contact::getIsActive)
                .filter(contact -> contact.getContactLists().stream()
                        .anyMatch(contactList -> campaignListIds.contains(contactList.getListId())))
                .toList();

        if (contacts.isEmpty()) {
            logger.warn("No active contacts found for campaign {}", campaignId);
            throw new IllegalStateException("No active contacts found for this campaign");
        }

        Template template = campaign.getTemplate();
        List<Long> messageIds = new ArrayList<>();

        for (Contact contact : contacts) {
            Message message = new Message();
            message.setCampaign(campaign);
            message.setContact(contact);
            message.setTemplate(template);
            message.setMessageContent(generateMessageContent(template.getTemplateContent(), contact));
            message.setMessageType("TEMPLATE");
            message.setStatus("PENDING");

            Message savedMessage = messageRepository.save(message);
            messageIds.add(savedMessage.getMessageId());
        }

        logger.info("Generated {} messages for campaign {}", messageIds.size(), campaignId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                logger.info("Transaction committed. Submitting {} messages to workers for campaign {}", messageIds.size(), campaignId);
                for (Long messageId : messageIds) {
                    messageWorkerService.submit(messageId);
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Message> getAllMessages() {
        logger.info("Fetching all messages");
        return messageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Message getMessageById(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Message not found with id {}", id);
                    return new RuntimeException("Message not found with id: " + id);
                });
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByCampaign(Long campaignId) {
        logger.info("Fetching messages for campaign {}", campaignId);
        return messageRepository.findByCampaign_CampaignId(campaignId);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByContact(Long contactId) {
        logger.info("Fetching messages for contact {}", contactId);
        return messageRepository.findByContact_ContactId(contactId);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByStatus(String status) {

        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Message status is required");
        }

        logger.info("Fetching messages by status {}", status);

        return messageRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional
    public Message updateStatus(Long id, String status) {

        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Message status is required");
        }

        String normalizedStatus = status.trim().toUpperCase();

        logger.info("Updating message {} status to {}", id, normalizedStatus);

        return switch (normalizedStatus) {
            case "SENT" -> markAsSent(id);
            case "DELIVERED" -> markAsDelivered(id);
            case "FAILED" -> markAsFailed(id);
            case "PENDING" -> {
                Message message = getMessageById(id);
                if (!"PENDING".equals(message.getStatus())) {
                    throw new IllegalStateException("Only a new message can be set to PENDING");
                }
                yield message;
            }
            default -> throw new IllegalArgumentException("Invalid message status: " + status);
        };
    }

    @Transactional
    public Message markAsSent(Long id) {

        Message message = getMessageById(id);

        if (!"PENDING".equals(message.getStatus())) {
            logger.warn("Message {} cannot be marked as SENT because its status is {}", id, message.getStatus());
            throw new IllegalStateException("Only PENDING messages can be marked as SENT");
        }

        message.setStatus("SENT");
        message.setSentAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        logger.info("Message {} marked as SENT", id);

        return savedMessage;
    }

    @Transactional
    public Message markAsDelivered(Long id) {

        Message message = getMessageById(id);

        if (!"SENT".equals(message.getStatus())) {
            logger.warn("Message {} cannot be marked as DELIVERED because its status is {}", id, message.getStatus());
            throw new IllegalStateException("Only SENT messages can be marked as DELIVERED");
        }

        message.setStatus("DELIVERED");
        message.setDeliveredAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        logger.info("Message {} marked as DELIVERED", id);

        return savedMessage;
    }

    @Transactional
    public Message markAsFailed(Long id) {

        Message message = getMessageById(id);

        if (!"PENDING".equals(message.getStatus()) && !"SENT".equals(message.getStatus())) {
            logger.warn("Message {} cannot be marked as FAILED because its status is {}", id, message.getStatus());
            throw new IllegalStateException("Only PENDING or SENT messages can be marked as FAILED");
        }

        message.setStatus("FAILED");
        message.setFailedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        logger.info("Message {} marked as FAILED", id);

        return savedMessage;
    }

    private String generateMessageContent(String templateContent, Contact contact) {

        if (templateContent == null) {
            return "";
        }

        String contactName = contact.getContactName();

        if (contactName == null) {
            contactName = "";
        }

        return templateContent.replace("{{name}}", contactName);
    }
}