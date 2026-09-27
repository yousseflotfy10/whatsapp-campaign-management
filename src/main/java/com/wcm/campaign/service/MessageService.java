package com.wcm.campaign.service;

import com.wcm.campaign.model.Campaign;
import com.wcm.campaign.model.Contact;
import com.wcm.campaign.model.Message;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final CampaignService campaignService;
    private final ContactService contactService;
    private final TemplateService templateService;

    public MessageService(MessageRepository messageRepository, CampaignService campaignService, ContactService contactService, TemplateService templateService) {
        this.messageRepository = messageRepository;
        this.campaignService = campaignService;
        this.contactService = contactService;
        this.templateService = templateService;
    }

    @Transactional
    public Message createMessage(Long campaignId, Long contactId, Long templateId) {
        Campaign campaign = campaignService.getById(campaignId);

        Contact contact = contactService.getContactById(contactId);

        Template template = templateService.getById(templateId);

        Message message = new Message();

        message.setCampaign(campaign);
        message.setContact(contact);
        message.setTemplate(template);

        message.setMessageContent(generateMessageContent(template.getTemplateContent(), contact));

        message.setStatus("PENDING");

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Message getMessageById(Long id) {
        return messageRepository.findById(id).orElseThrow(() -> new RuntimeException("Message not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByCampaign(Long campaignId) {
        return messageRepository.findByCampaign_CampaignId(campaignId);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByContact(Long contactId) {
        return messageRepository.findByContact_ContactId(contactId);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByStatus(String status) {
        return messageRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional
    public Message updateStatus(Long id, String status) {
        String normalizedStatus = status.trim().toUpperCase();

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
            throw new IllegalStateException("Only PENDING messages can be marked as SENT");
        }

        message.setStatus("SENT");
        message.setSentAt(LocalDateTime.now());

        return messageRepository.save(message);
    }

    @Transactional
    public Message markAsDelivered(Long id) {

        Message message = getMessageById(id);

        if (!"SENT".equals(message.getStatus())) {
            throw new IllegalStateException("Only SENT messages can be marked as DELIVERED");
        }

        message.setStatus("DELIVERED");

        message.setDeliveredAt(LocalDateTime.now());

        return messageRepository.save(message);
    }

    @Transactional
    public Message markAsFailed(Long id) {

        Message message = getMessageById(id);

        if (!"PENDING".equals(message.getStatus()) && !"SENT".equals(message.getStatus())) {
            throw new IllegalStateException("Only PENDING or SENT messages can be marked as FAILED");
        }

        message.setStatus("FAILED");

        return messageRepository.save(message);
    }

    private String generateMessageContent(String templateContent, Contact contact) {

        String contactName = contact.getContactName();

        if (contactName == null) {
            contactName = "";
        }

        return templateContent.replace("{{name}}", contactName);
    }
}