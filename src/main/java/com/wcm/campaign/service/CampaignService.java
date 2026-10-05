package com.wcm.campaign.service;

import com.wcm.campaign.dto.CampaignRequest;
import com.wcm.campaign.model.Campaign;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.repository.CampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class CampaignService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;
    private final TemplateService templateService;
    private final MessageService messageService;

    public CampaignService(
            CampaignRepository campaignRepository,
            TemplateService templateService,
            MessageService messageService
    ) {
        this.campaignRepository = campaignRepository;
        this.templateService = templateService;
        this.messageService = messageService;
    }

    @Transactional
    public Campaign createCampaign(CampaignRequest request) {

        logger.info("Creating campaign");

        Template template = templateService.getById(request.getTemplateId());

        if (!"APPROVED".equals(template.getStatus())) {
            logger.warn("Campaign creation failed: template {} is not approved", request.getTemplateId());
            throw new IllegalStateException("Campaigns can only use approved templates");
        }

        Campaign campaign = new Campaign();

        campaign.setCampaignName(request.getCampaignName().trim());

        if (request.getSenderName() != null) {
            campaign.setSenderName(request.getSenderName().trim());
        }

        campaign.setListId(normalizeListIds(request.getListId()));

        campaign.setTemplate(template);

        campaign.setStatus("DRAFT");

        Campaign savedCampaign = campaignRepository.save(campaign);

        logger.info("Campaign created successfully with id {}", savedCampaign.getCampaignId());

        return savedCampaign;
    }

    @Transactional(readOnly = true)
    public List<Campaign> getAll() {
        logger.info("Fetching all campaigns");
        return campaignRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Campaign getById(Long id) {
        return campaignRepository.findById(id).orElseThrow(() -> {
            logger.warn("Campaign not found with id {}", id);
            return new RuntimeException("Campaign not found with id: " + id);
        });
    }

    @Transactional(readOnly = true)
    public List<Campaign> getByStatus(String status) {

        logger.info("Fetching campaigns by status {}", status);

        return campaignRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<Campaign> search(String name) {

        if (name == null || name.trim().isEmpty()) {
            return getAll();
        }

        logger.info("Searching campaigns by name");

        return campaignRepository.findByCampaignNameContainingIgnoreCase(name.trim());
    }

    @Transactional
    public Campaign update(Long id, CampaignRequest request) {

        logger.info("Updating campaign with id {}", id);

        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            logger.warn("Campaign {} cannot be updated because its status is {}", id, campaign.getStatus());
            throw new IllegalStateException("Only draft campaigns can be updated");
        }

        Template template = templateService.getById(request.getTemplateId());

        if (!"APPROVED".equals(template.getStatus())) {
            logger.warn("Campaign update failed: template {} is not approved", request.getTemplateId());
            throw new IllegalStateException("Campaigns can only use approved templates");
        }

        campaign.setCampaignName(request.getCampaignName().trim());

        if (request.getSenderName() != null) {
            campaign.setSenderName(request.getSenderName().trim());
        } else {
            campaign.setSenderName(null);
        }

        campaign.setListId(normalizeListIds(request.getListId()));

        campaign.setTemplate(template);

        Campaign updatedCampaign = campaignRepository.save(campaign);

        logger.info("Campaign {} updated successfully", id);

        return updatedCampaign;
    }

    @Transactional
    public Campaign submit(Long id) {
        logger.info("Submitting campaign {}", id);

        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            logger.warn("Campaign {} cannot be submitted because its status is {}", id, campaign.getStatus());
            throw new IllegalStateException("Only draft campaigns can be submitted");
        }

        campaign.setStatus("PENDING");

        campaign.setRejectionComment(null);

        Campaign submittedCampaign = campaignRepository.save(campaign);

        logger.info("Campaign {} submitted successfully", id);

        return submittedCampaign;
    }

    @Transactional
    public Campaign approve(Long id) {

        logger.info("Approving campaign {}", id);

        Campaign campaign = getById(id);

        if (!"PENDING".equals(campaign.getStatus())) {
            logger.warn("Campaign {} cannot be approved because its status is {}", id, campaign.getStatus());
            throw new IllegalStateException("Only pending campaigns can be approved");
        }

        campaign.setStatus("APPROVED");

        campaign.setApprovedAt(LocalDateTime.now());

        campaign.setRejectionComment(null);

        Campaign approvedCampaign = campaignRepository.save(campaign);

        logger.info("Campaign {} approved successfully", id);

        messageService.generateMessagesForCampaign(approvedCampaign.getCampaignId());

        logger.info("Messages generated for approved campaign {}", id);

        return approvedCampaign;
    }

    @Transactional
    public Campaign reject(Long id, String rejectionComment) {

        logger.info("Rejecting campaign {}", id);

        Campaign campaign = getById(id);

        if (!"PENDING".equals(campaign.getStatus())) {
            logger.warn("Campaign {} cannot be rejected because its status is {}", id, campaign.getStatus());
            throw new IllegalStateException("Only pending campaigns can be rejected");
        }

        if (rejectionComment == null || rejectionComment.trim().isEmpty()) {
            logger.warn("Campaign {} rejection failed: rejection comment is required", id);
            throw new IllegalArgumentException("Rejection comment is required");
        }

        campaign.setStatus("REJECTED");

        campaign.setApprovedAt(LocalDateTime.now());

        campaign.setRejectionComment(rejectionComment.trim());

        Campaign rejectedCampaign = campaignRepository.save(campaign);

        logger.info("Campaign {} rejected successfully", id);

        return rejectedCampaign;
    }

    @Transactional
    public void delete(Long id) {

        logger.info("Deleting campaign {}", id);

        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            logger.warn("Campaign {} cannot be deleted because its status is {}", id, campaign.getStatus());
            throw new IllegalStateException("Only DRAFT campaigns can be deleted");
        }

        campaignRepository.delete(campaign);

        logger.info("Campaign {} deleted successfully", id);
    }

    private String normalizeListIds(String listIds) {

        if (listIds == null || listIds.trim().isEmpty()) {
            throw new IllegalArgumentException("At least one contact list is required");
        }

        return Arrays.stream(listIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .reduce((first, second) -> first + "," + second)
                .orElseThrow(() -> new IllegalArgumentException("At least one contact list is required"));
    }
}