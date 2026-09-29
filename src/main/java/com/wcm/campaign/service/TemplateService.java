package com.wcm.campaign.service;

import com.wcm.campaign.dto.TemplateRequest;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.repository.TemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional
    public Template createTemplate(TemplateRequest request) {

        Template template = new Template();

        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateContent(request.getTemplateContent().trim());

        template.setLanguage("en_US");
        template.setCategory("UTILITY");

        template.setStatus("PENDING");

        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<Template> getAll() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Template getById(Long id) {
        return templateRepository.findById(id).orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Template> getByStatus(String status) {
        return templateRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<Template> search(String name) {

        if (name == null || name.trim().isEmpty()) {
            return getAll();
        }

        return templateRepository.findByTemplateNameContainingIgnoreCase(name.trim());
    }

    @Transactional
    public Template update(Long id, TemplateRequest request) {

        Template template = getById(id);

        if (!"PENDING".equals(template.getStatus())) {
            throw new IllegalStateException("Only PENDING templates can be updated");
        }

        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateContent(request.getTemplateContent().trim());

        return templateRepository.save(template);
    }

    @Transactional
    public Template approve(Long id) {

        Template template = getById(id);

        if (!"PENDING".equals(template.getStatus())) {
            throw new IllegalStateException("Only PENDING templates can be approved");
        }

        template.setStatus("APPROVED");

        return templateRepository.save(template);
    }

    @Transactional
    public Template reject(Long id) {

        Template template = getById(id);

        if (!"PENDING".equals(template.getStatus())) {
            throw new IllegalStateException("Only PENDING templates can be rejected");
        }

        template.setStatus("REJECTED");

        return templateRepository.save(template);
    }

    @Transactional
    public void delete(Long id) {

        Template template = getById(id);

        if ("APPROVED".equals(template.getStatus())) {
            throw new IllegalStateException("Approved templates cannot be deleted");
        }

        templateRepository.delete(template);
    }
}