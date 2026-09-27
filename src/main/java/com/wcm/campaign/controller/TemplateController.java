package com.wcm.campaign.controller;

import com.wcm.campaign.dto.TemplateRequest;
import com.wcm.campaign.dto.TemplateResponse;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody TemplateRequest request) {

        Template template = templateService.createTemplate(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(new TemplateResponse(template));
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {

        List<TemplateResponse> templates = templateService.getAll().stream().map(TemplateResponse::new).toList();

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplateById(@PathVariable Long id) {

        Template template = templateService.getById(id);

        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TemplateResponse>> getByStatus(@PathVariable String status) {

        List<TemplateResponse> templates = templateService.getByStatus(status).stream().map(TemplateResponse::new).toList();

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TemplateResponse>> search(@RequestParam String name) {

        List<TemplateResponse> templates = templateService.search(name).stream().map(TemplateResponse::new) .toList();

        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateRequest request
    ) {

        Template template = templateService.update(id, request);

        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<TemplateResponse> approveTemplate(@PathVariable Long id) {

        Template template = templateService.approve(id);

        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<TemplateResponse> rejectTemplate(@PathVariable Long id) {

        Template template = templateService.reject(id);

        return ResponseEntity.ok(new TemplateResponse(template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {

        templateService.delete(id);

        return ResponseEntity.noContent().build();
    }
}