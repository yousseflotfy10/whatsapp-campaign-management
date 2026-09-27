package com.wcm.campaign.dto;

import com.wcm.campaign.model.Campaign;

import java.time.LocalDateTime;

public class CampaignResponse {

    private Long campaignId;
    private String campaignName;
    private String senderName;
    private String listId;
    private Long templateId;
    private String status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentTime;

    public CampaignResponse() {
    }

    public CampaignResponse(Campaign campaign) {

        this.campaignId = campaign.getCampaignId();
        this.campaignName = campaign.getCampaignName();
        this.senderName = campaign.getSenderName();
        this.listId = campaign.getListId();

        if (campaign.getTemplate() != null) {
            this.templateId = campaign.getTemplate().getTemplateId();
        }

        this.status = campaign.getStatus();

        if (campaign.getApprovedBy() != null) {
            this.approvedBy = campaign.getApprovedBy().getUserId();
        }

        this.approvedAt = campaign.getApprovedAt();
        this.rejectionComment = campaign.getRejectionComment();
        this.createdAt = campaign.getCreatedAt();
        this.updatedAt = campaign.getUpdatedAt();
        this.sentTime = campaign.getSentTime();
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionComment() {
        return rejectionComment;
    }

    public void setRejectionComment(String rejectionComment) {
        this.rejectionComment = rejectionComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getSentTime() {
        return sentTime;
    }

    public void setSentTime(LocalDateTime sentTime) {
        this.sentTime = sentTime;
    }
}