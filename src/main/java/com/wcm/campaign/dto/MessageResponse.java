package com.wcm.campaign.dto;

import com.wcm.campaign.model.Message;

import java.time.LocalDateTime;

public class MessageResponse {

    private Long messageId;
    private Long campaignId;
    private Long contactId;
    private Long templateId;
    private String messageContent;
    private String status;
    private LocalDateTime sentAt;

    public MessageResponse() {
    }

    public MessageResponse(Message message) {

        this.messageId = message.getMessageId();
        if (message.getCampaign() != null) {
            this.campaignId = message.getCampaign().getCampaignId();
        }
        if (message.getContact() != null) {
            this.contactId = message.getContact().getContactId();
        }
        if (message.getTemplate() != null) {
            this.templateId = message.getTemplate().getTemplateId();
        }
        this.messageContent = message.getMessageContent();
        this.status = message.getStatus();
        this.sentAt = message.getSentAt();
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}