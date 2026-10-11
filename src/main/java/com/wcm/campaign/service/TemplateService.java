
package com.wcm.campaign.service;

import com.wcm.campaign.dto.TemplateRequest;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateService {

    private static final Logger logger =
            LoggerFactory.getLogger(TemplateService.class);

    private final TemplateRepository templateRepository;
    private final WhatsAppProvider whatsappProvider;

    public TemplateService(
            TemplateRepository templateRepository,
            WhatsAppProvider whatsappProvider
    ) {
        this.templateRepository = templateRepository;
        this.whatsappProvider = whatsappProvider;
    }

    @Transactional
    public Template createTemplate(TemplateRequest request) {
        logger.info("Creating template");

        Template template = new Template();

        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateContent(request.getTemplateContent().trim());
        template.setLanguage("en_US");
        template.setCategory("UTILITY");
        template.setStatus("DRAFT");

        Template savedTemplate = templateRepository.save(template);

        logger.info(
                "Template created successfully. Id={}, Name={}, Status={}",
                savedTemplate.getTemplateId(),
                savedTemplate.getTemplateName(),
                savedTemplate.getStatus());

        return savedTemplate;
    }

    @Transactional
    public List<Template> getAll() {
        logger.info("Fetching all templates");

        synchronizePendingStatuses();

        List<Template> templates = templateRepository.findAll();

        logger.info("Retrieved {} templates", templates.size());

        return templates;
    }

    @Transactional(readOnly = true)
    public Template getById(Long id) {
        logger.info("Fetching template. Id={}", id);

        return templateRepository.findById(id).orElseThrow(() -> {
            logger.warn("Template not found. Id={}", id);
            return new RuntimeException("Template not found with id: " + id);
        });
    }

    @Transactional
    public List<Template> getByStatus(String status) {
        logger.info("Fetching templates by status. Status={}", status);

        synchronizePendingStatuses();

        List<Template> templates =
                templateRepository.findByStatus(status.trim().toUpperCase());

        logger.info(
                "Retrieved {} templates with status {}",
                templates.size(), status);

        return templates;
    }

    @Transactional(readOnly = true)
    public List<Template> search(String name) {
        if (name == null || name.trim().isEmpty()) {
            logger.info("Template search received an empty name; fetching all");
            return getAll();
        }

        logger.info("Searching templates by name");

        List<Template> templates =
                templateRepository.findByTemplateNameContainingIgnoreCase(
                        name.trim());

        logger.info("Template search returned {} results", templates.size());

        return templates;
    }

    @Transactional
    public Template update(Long id, TemplateRequest request) {
        logger.info("Updating template. Id={}", id);

        Template template = getById(id);

        if (!"DRAFT".equals(template.getStatus())) {
            logger.warn(
                    "Template update rejected. Id={}, Status={}",
                    id, template.getStatus());

            throw new IllegalStateException(
                    "Only draft templates can be updated");
        }

        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateContent(request.getTemplateContent().trim());

        Template updatedTemplate = templateRepository.save(template);

        logger.info("Template updated successfully. Id={}", id);

        return updatedTemplate;
    }

    @Transactional
    public Template submit(Long id) {
        logger.info("Submitting template to provider. Id={}", id);

        Template template = getById(id);

        if (!"DRAFT".equals(template.getStatus())) {
            logger.warn(
                    "Template submission rejected. Id={}, Status={}",
                    id, template.getStatus());

            throw new IllegalStateException(
                    "Only draft templates can be submitted");
        }

        WhatsAppTemplateSubmissionResult submission;

        try {
            submission = whatsappProvider.submitTemplate(
                    template.getTemplateName(),
                    template.getTemplateContent(),
                    template.getLanguage(),
                    template.getCategory()
            );
        } catch (RuntimeException exception) {
            logger.error(
                    "Provider template submission failed. Id={}, Name={}",
                    id, template.getTemplateName(), exception);
            throw exception;
        }

        if (!submission.success()) {
            logger.error(
                    "Provider rejected template submission. Id={}, Reason={}",
                    id, submission.failureReason());

            throw new IllegalStateException(
                    "Simulator rejected template submission: "
                            + submission.failureReason());
        }

        template.setProviderTemplateId(submission.providerTemplateId());
        template.setStatus("PENDING");

        Template submittedTemplate = templateRepository.save(template);

        logger.info(
                "Template submitted successfully. Id={}, ProviderId={}, Status={}",
                submittedTemplate.getTemplateId(),
                submittedTemplate.getProviderTemplateId(),
                submittedTemplate.getStatus());

        return submittedTemplate;
    }

    @Transactional
    public void applyProviderStatus(
            String templateName,
            String providerStatus
    ) {
        logger.info(
                "Received provider template status. Name={}, ProviderStatus={}",
                templateName, providerStatus);

        if (templateName == null || templateName.isBlank()
                || providerStatus == null || providerStatus.isBlank()) {
            logger.error(
                    "Cannot apply provider status because template name or status is missing");
            return;
        }

        var templateOptional =
                templateRepository.findByTemplateName(templateName);

        if (templateOptional.isEmpty()) {
            logger.error(
                    "No local template matches provider event. Name={}, ProviderStatus={}",
                    templateName, providerStatus);
            return;
        }

        Template template = templateOptional.get();
        String previousStatus = template.getStatus();

        String newStatus = switch (providerStatus.trim().toUpperCase()) {
            case "APPROVED" -> "APPROVED";
            case "REJECTED", "DISABLED", "PAUSED" -> "REJECTED";
            default -> "PENDING";
        };

        logger.info(
                "Provider status mapping. Name={}, PreviousStatus={}, ProviderStatus={}, NewStatus={}",
                templateName, previousStatus, providerStatus, newStatus);

        if (newStatus.equals(previousStatus)) {
            logger.info(
                    "Template already has the provider-reported status. Name={}, Status={}",
                    templateName, previousStatus);
            return;
        }

        template.setStatus(newStatus);
        templateRepository.save(template);

        logger.info(
                "Template status saved from provider event. Name={}, OldStatus={}, NewStatus={}",
                templateName, previousStatus, newStatus);
    }

    private void synchronizePendingStatuses() {
        List<Template> pendingTemplates =
                templateRepository.findByStatus("PENDING");

        logger.info(
                "Starting provider status synchronization. Pending templates={}",
                pendingTemplates.size());

        if (pendingTemplates.isEmpty()) {
            logger.info("No pending templates require synchronization");
            return;
        }

        final List<WhatsAppProviderTemplate> providerTemplates;

        try {
            providerTemplates = whatsappProvider.listTemplates();

            logger.info(
                    "Retrieved {} templates from provider",
                    providerTemplates.size());
        } catch (RuntimeException exception) {
            logger.error(
                    "Unable to retrieve templates from provider for synchronization",
                    exception);
            return;
        }

        for (Template template : pendingTemplates) {
            logger.info(
                    "Matching pending template with provider. Id={}, Name={}, ProviderId={}",
                    template.getTemplateId(),
                    template.getTemplateName(),
                    template.getProviderTemplateId());

            WhatsAppProviderTemplate providerTemplate =
                    providerTemplates.stream()
                            .filter(candidate ->
                                    template.getProviderTemplateId() != null
                                            && template.getProviderTemplateId()
                                            .equals(candidate.id()))
                            .findFirst()
                            .orElseGet(() -> providerTemplates.stream()
                                    .filter(candidate ->
                                            template.getTemplateName()
                                                    .equals(candidate.name())
                                                    && template.getLanguage()
                                                    .equals(candidate.language()))
                                    .findFirst()
                                    .orElse(null));

            if (providerTemplate == null) {
                logger.warn(
                        "No matching provider template found. Id={}, Name={}",
                        template.getTemplateId(),
                        template.getTemplateName());
                continue;
            }

            if (providerTemplate.status() == null) {
                logger.warn(
                        "Provider template has no status. Id={}, Name={}",
                        template.getTemplateId(),
                        template.getTemplateName());
                continue;
            }

            String synchronizedStatus =
                    switch (providerTemplate.status().toUpperCase()) {
                        case "APPROVED" -> "APPROVED";
                        case "REJECTED", "DISABLED", "PAUSED" -> "REJECTED";
                        default -> "PENDING";
                    };

            logger.info(
                    "Provider synchronization result. Name={}, LocalStatus={}, ProviderStatus={}, MappedStatus={}",
                    template.getTemplateName(),
                    template.getStatus(),
                    providerTemplate.status(),
                    synchronizedStatus);

            if (!synchronizedStatus.equals(template.getStatus())) {
                String previousStatus = template.getStatus();

                template.setStatus(synchronizedStatus);
                templateRepository.save(template);

                logger.info(
                        "Template status synchronized from provider. Name={}, OldStatus={}, NewStatus={}",
                        template.getTemplateName(),
                        previousStatus,
                        synchronizedStatus);
            } else {
                logger.info(
                        "Template status is already synchronized. Name={}, Status={}",
                        template.getTemplateName(),
                        template.getStatus());
            }
        }

        logger.info("Provider status synchronization completed");
    }

    @Transactional
    public void delete(Long id) {
        logger.info("Deleting template. Id={}", id);

        Template template = getById(id);

        if ("APPROVED".equals(template.getStatus())) {
            logger.warn(
                    "Template deletion rejected because it is approved. Id={}",
                    id);

            throw new IllegalStateException(
                    "Approved templates cannot be deleted");
        }

        templateRepository.delete(template);

        logger.info("Template deleted successfully. Id={}", id);
    }
}
