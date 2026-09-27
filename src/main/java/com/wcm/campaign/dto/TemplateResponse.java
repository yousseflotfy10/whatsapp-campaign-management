package com.wcm.campaign.dto;

import com.wcm.campaign.model.Template;

public class TemplateResponse {

    private Long templateId;
    private String templateName;
    private String templateContent;
    private String status;

    public TemplateResponse() {}

    public TemplateResponse(Template template) {
        this.templateId = template.getTemplateId();
        this.templateName = template.getTemplateName();
        this.templateContent = template.getTemplateContent();
        this.status = template.getStatus();
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}