package com.wcm.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CampaignRequest {

    @NotBlank(message = "Campaign name is required")
    @Size(max = 100, message = "Campaign name must not exceed 100 characters")
    private String campaignName;

    @Size(max = 100, message = "Sender name must not exceed 100 characters")
    private String senderName;

    @NotBlank(message = "At least one contact list is required")
    private String listId;

    @NotNull(message = "Template ID is required")
    private Long templateId;

    public CampaignRequest() {
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }
}