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

    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);

    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional
    public Template createTemplate(TemplateRequest request) {

        logger.info("Creating template");

        Template template = new Template();

        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateContent(request.getTemplateContent().trim());

        template.setLanguage("en_US");
        template.setCategory("UTILITY");

        template.setStatus("PENDING");

        Template savedTemplate = templateRepository.save(template);

        logger.info("Template created successfully with id {}", savedTemplate.getTemplateId());

        return savedTemplate;
    }

    @Transactional(readOnly = true)
    public List<Template> getAll() {
        logger.info("Fetching all templates");
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Template getById(Long id) {
        return templateRepository.findById(id).orElseThrow(() -> {
            logger.warn("Template not found with id {}", id);
            return new RuntimeException("Template not found with id: " + id);
        });
    }

    @Transactional(readOnly = true)
    public List<Template> getByStatus(String status) {
        logger.info("Fetching templates by status {}", status);
        return templateRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<Template> search(String name) {

        if (name == null || name.trim().isEmpty()) {
            return getAll();
        }

        logger.info("Searching templates by name");

        return templateRepository.findByTemplateNameContainingIgnoreCase(name.trim());
    }

    @Transactional
    public Template update(Long id, TemplateRequest request) {

        logger.info("Updating template {}", id);

        Template template = getById(id);

        if (!"PENDING".equals(template.getStatus())) {
            logger.warn("Template {} cannot be updated because its status is {}", id, template.getStatus());
            throw new IllegalStateException("Only pending templates can be updated");
        }

        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateContent(request.getTemplateContent().trim());

        Template updatedTemplate = templateRepository.save(template);

        logger.info("Template {} updated successfully", id);

        return updatedTemplate;
    }

    @Transactional
    public Template approve(Long id) {

        logger.info("Approving template {}", id);

        Template template = getById(id);

        if (!"PENDING".equals(template.getStatus())) {
            logger.warn("Template {} cannot be approved because its status is {}", id, template.getStatus());
            throw new IllegalStateException("Only pending templates can be approved");
        }

        template.setStatus("APPROVED");

        Template approvedTemplate = templateRepository.save(template);

        logger.info("Template {} approved successfully", id);

        return approvedTemplate;
    }

    @Transactional
    public Template reject(Long id) {

        logger.info("Rejecting template {}", id);

        Template template = getById(id);

        if (!"PENDING".equals(template.getStatus())) {
            logger.warn("Template {} cannot be rejected because its status is {}", id, template.getStatus());
            throw new IllegalStateException("Only pending templates can be rejected");
        }

        template.setStatus("REJECTED");

        Template rejectedTemplate = templateRepository.save(template);

        logger.info("Template {} rejected successfully", id);

        return rejectedTemplate;
    }

    @Transactional
    public void delete(Long id) {

        logger.info("Deleting template {}", id);

        Template template = getById(id);

        if ("APPROVED".equals(template.getStatus())) {
            logger.warn("Template {} cannot be deleted because it is approved", id);
            throw new IllegalStateException("Approved templates cannot be deleted");
        }

        templateRepository.delete(template);

        logger.info("Template {} deleted successfully", id);
    }
}