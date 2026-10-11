package com.wcm.campaign.controller;

import com.wcm.campaign.dto.TemplateRequest;
import com.wcm.campaign.dto.TemplateResponse;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.service.TemplateService;
import com.wcm.campaign.service.RealtimeEventService;
import com.wcm.campaign.service.WhatsAppProvider;
import com.wcm.campaign.service.WhatsAppProviderTemplate;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    private final TemplateService templateService;
    private final RealtimeEventService realtimeEventService;
    private final WhatsAppProvider whatsappProvider;

    public TemplateController(
            TemplateService templateService,
            RealtimeEventService realtimeEventService,
            WhatsAppProvider whatsappProvider
    ) {
        this.templateService = templateService;
        this.realtimeEventService = realtimeEventService;
        this.whatsappProvider = whatsappProvider;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEMPLATE_CREATE')")
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody TemplateRequest request) {

        logger.info("Creating template");

        Template template = templateService.createTemplate(request);

        realtimeEventService.publishAfterCommit("templates");

        logger.info("Template created successfully with id={}", template.getTemplateId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new TemplateResponse(template));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {

        logger.info("Fetching all templates");

        List<TemplateResponse> templates = templateService.getAll().stream().map(TemplateResponse::new).toList();

        logger.info("Fetched {} templates", templates.size());

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/provider")
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    public ResponseEntity<List<WhatsAppProviderTemplate>> getProviderTemplates() {
        return ResponseEntity.ok(whatsappProvider.listTemplates());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    public ResponseEntity<TemplateResponse> getTemplateById(@PathVariable Long id) {

        logger.info("Fetching template with id={}", id);

        Template template = templateService.getById(id);

        realtimeEventService.publishAfterCommit("templates");

        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    public ResponseEntity<List<TemplateResponse>> getByStatus(@PathVariable String status) {

        logger.info("Fetching templates with status={}", status);

        List<TemplateResponse> templates = templateService.getByStatus(status).stream().map(TemplateResponse::new).toList();

        logger.info("Fetched {} templates with status={}", templates.size(), status);

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    public ResponseEntity<List<TemplateResponse>> search(@RequestParam String name) {

        logger.info("Searching templates");

        List<TemplateResponse> templates = templateService.search(name).stream().map(TemplateResponse::new) .toList();

        logger.info("Template search returned {} results", templates.size());

        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_UPDATE')")
    public ResponseEntity<TemplateResponse> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateRequest request
    ) {

        logger.info("Updating template with id={}", id);

        Template template = templateService.update(id, request);

        realtimeEventService.publishAfterCommit("templates");

        logger.info("Template updated successfully with id={}", id);

        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('TEMPLATE_UPDATE')")
    public ResponseEntity<TemplateResponse> submitTemplate(@PathVariable Long id) {

        logger.info("Submitting template with id={}", id);

        Template template = templateService.submit(id);

        realtimeEventService.publishAfterCommit("templates");

        logger.info("Template submitted successfully with id={}", id);

        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_UPDATE')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {

        logger.info("Deleting template with id={}", id);

        templateService.delete(id);
        realtimeEventService.publishAfterCommit("templates");

        logger.info("Template deleted successfully with id={}", id);

        return ResponseEntity.noContent().build();
    }
}