package com.wcm.campaign.controller;

import com.wcm.campaign.dto.CampaignRequest;
import com.wcm.campaign.dto.CampaignResponse;
import com.wcm.campaign.model.Campaign;
import com.wcm.campaign.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest request) {
        Campaign campaign = campaignService.createCampaign(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CampaignResponse(campaign));
    }

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {

        List<CampaignResponse> campaigns = campaignService.getAll().stream().map(CampaignResponse::new).toList();

        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable Long id) {

        Campaign campaign = campaignService.getById(id);

        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CampaignResponse>> getByStatus(@PathVariable String status) {

        List<CampaignResponse> campaigns = campaignService.getByStatus(status).stream().map(CampaignResponse::new).toList();

        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CampaignResponse>> searchCampaigns(@RequestParam String name) {

        List<CampaignResponse> campaigns = campaignService.search(name).stream().map(CampaignResponse::new).toList();

        return ResponseEntity.ok(campaigns);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(@PathVariable Long id, @Valid @RequestBody CampaignRequest request) {

        Campaign campaign = campaignService.update(id, request);

        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<CampaignResponse> submitCampaign(@PathVariable Long id) {

        Campaign campaign = campaignService.submit(id);

        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<CampaignResponse> approveCampaign(@PathVariable Long id, @RequestParam Long userId) {

        Campaign campaign = campaignService.approve(id, userId);

        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<CampaignResponse> rejectCampaign(@PathVariable Long id, @RequestParam Long userId, @RequestParam String comment) {

        Campaign campaign = campaignService.reject(id, userId, comment);

        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {

        campaignService.delete(id);

        return ResponseEntity.noContent().build();
    }
}