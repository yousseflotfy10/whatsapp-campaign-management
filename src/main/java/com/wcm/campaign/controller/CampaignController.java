package com.wcm.campaign.controller;

import com.wcm.campaign.dto.CampaignRequest;
import com.wcm.campaign.dto.CampaignResponse;
import com.wcm.campaign.model.Campaign;
import com.wcm.campaign.service.CampaignService;
import com.wcm.campaign.service.RealtimeEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {
    private final CampaignService campaignService;
    private final RealtimeEventService realtimeEventService;

    public CampaignController(
            CampaignService campaignService,
            RealtimeEventService realtimeEventService
    ) {
        this.campaignService = campaignService;
        this.realtimeEventService = realtimeEventService;
    }

    @PostMapping
        @PreAuthorize("hasAuthority('CAMPAIGN_CREATE')")
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest request) {
        Campaign campaign = campaignService.createCampaign(request);
        realtimeEventService.publishAfterCommit("campaigns");
        return ResponseEntity.status(HttpStatus.CREATED).body(new CampaignResponse(campaign));
    }

    @GetMapping
        @PreAuthorize("hasAuthority('CAMPAIGN_VIEW')")
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {

        List<CampaignResponse> campaigns = campaignService.getAll()
                        .stream()
                        .map(CampaignResponse::new)
                        .toList();
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW')")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable Long id ){
        Campaign campaign = campaignService.getById(id);

        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW')")
    public ResponseEntity<List<CampaignResponse>> getByStatus(@PathVariable String status) {

        List<CampaignResponse> campaigns = campaignService
                        .getByStatus(status)
                        .stream()
                        .map(CampaignResponse::new)
                        .toList();
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW')")
    public ResponseEntity<List<CampaignResponse>> searchCampaigns(@RequestParam String name) {

        List<CampaignResponse> campaigns = campaignService
                        .search(name)
                        .stream()
                        .map(CampaignResponse::new)
                        .toList();
        return ResponseEntity.ok(campaigns);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPAIGN_UPDATE')")
    public ResponseEntity<CampaignResponse> updateCampaign(@PathVariable Long id, @Valid @RequestBody CampaignRequest request) {
        Campaign campaign = campaignService.update(id, request);
        realtimeEventService.publishAfterCommit("campaigns");
        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('CAMPAIGN_SUBMIT')")
    public ResponseEntity<CampaignResponse> submitCampaign(@PathVariable Long id) {

        Campaign campaign = campaignService.submit(id);
        realtimeEventService.publishAfterCommit("campaigns");
        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('CAMPAIGN_APPROVE')")
    public ResponseEntity<CampaignResponse> approveCampaign(@PathVariable Long id) {

        Campaign campaign = campaignService.approve(id);
        realtimeEventService.publishAfterCommit("campaigns");

        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('CAMPAIGN_REJECT')")
    public ResponseEntity<CampaignResponse> rejectCampaign(@PathVariable Long id, @RequestParam String comment) {

        Campaign campaign = campaignService.reject(id, comment);
        realtimeEventService.publishAfterCommit("campaigns");

        return ResponseEntity.ok(new CampaignResponse(campaign));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPAIGN_DELETE')")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {

        campaignService.delete(id);
        realtimeEventService.publishAfterCommit("campaigns");

        return ResponseEntity.noContent().build();
    }
}