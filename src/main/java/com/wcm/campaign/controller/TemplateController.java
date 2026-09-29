package com.wcm.campaign.controller;

import com.wcm.campaign.dto.TemplateRequest;
import com.wcm.campaign.dto.TemplateResponse;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.service.TemplateService;
import com.wcm.campaign.service.RealtimeEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final RealtimeEventService realtimeEventService;

    public TemplateController(
            TemplateService templateService,
            RealtimeEventService realtimeEventService
    ) {
        this.templateService = templateService;
        this.realtimeEventService = realtimeEventService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEMPLATE_CREATE')")
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody TemplateRequest request) {

        Template template = templateService.createTemplate(request);

        realtimeEventService.publishAfterCommit("templates");
        return ResponseEntity.status(HttpStatus.CREATED).body(new TemplateResponse(template));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {

        List<TemplateResponse> templates = templateService.getAll().stream().map(TemplateResponse::new).toList();

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    public ResponseEntity<TemplateResponse> getTemplateById(@PathVariable Long id) {

        Template template = templateService.getById(id);

        realtimeEventService.publishAfterCommit("templates");
        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    public ResponseEntity<List<TemplateResponse>> getByStatus(@PathVariable String status) {

        List<TemplateResponse> templates = templateService.getByStatus(status).stream().map(TemplateResponse::new).toList();

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    public ResponseEntity<List<TemplateResponse>> search(@RequestParam String name) {

        List<TemplateResponse> templates = templateService.search(name).stream().map(TemplateResponse::new) .toList();

        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_UPDATE')")
    public ResponseEntity<TemplateResponse> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateRequest request
    ) {

        Template template = templateService.update(id, request);

        realtimeEventService.publishAfterCommit("templates");
        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('TEMPLATE_APPROVE')")
    public ResponseEntity<TemplateResponse> approveTemplate(@PathVariable Long id) {

        Template template = templateService.approve(id);

        realtimeEventService.publishAfterCommit("templates");
        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('TEMPLATE_REJECT')")
    public ResponseEntity<TemplateResponse> rejectTemplate(@PathVariable Long id) {

        Template template = templateService.reject(id);

        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_UPDATE')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {

        templateService.delete(id);
        realtimeEventService.publishAfterCommit("templates");

        return ResponseEntity.noContent().build();
    }
}