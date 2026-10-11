
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

    private static final Logger logger =
            LoggerFactory.getLogger(CampaignService.class);

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
        logger.info("Starting campaign creation");

        if (request == null) {
            logger.error("Campaign creation failed because the request is null");
            throw new IllegalArgumentException("Campaign request is required");
        }

        logger.info(
                "Campaign creation request received. Name={}, TemplateId={}",
                request.getCampaignName(), request.getTemplateId());

        Template template = templateService.getById(request.getTemplateId());

        logger.info(
                "Template loaded for campaign creation. TemplateId={}, Status={}",
                template.getTemplateId(), template.getStatus());

        if (!"APPROVED".equals(template.getStatus())) {
            logger.warn(
                    "Campaign creation rejected because template {} has status {}",
                    request.getTemplateId(), template.getStatus());

            throw new IllegalStateException(
                    "Campaigns can only use approved templates");
        }

        Campaign campaign = new Campaign();

        campaign.setCampaignName(request.getCampaignName().trim());

        if (request.getSenderName() != null) {
            campaign.setSenderName(request.getSenderName().trim());
        }

        campaign.setListId(normalizeListIds(request.getListId()));
        campaign.setTemplate(template);
        campaign.setStatus("DRAFT");

        logger.info(
                "Saving campaign. Name={}, Status={}, ListIds={}",
                campaign.getCampaignName(),
                campaign.getStatus(),
                campaign.getListId());

        Campaign savedCampaign = campaignRepository.save(campaign);

        logger.info(
                "Campaign created successfully. Id={}, Status={}",
                savedCampaign.getCampaignId(),
                savedCampaign.getStatus());

        return savedCampaign;
    }

    @Transactional(readOnly = true)
    public List<Campaign> getAll() {
        logger.info("Fetching all campaigns");

        List<Campaign> campaigns = campaignRepository.findAll();

        logger.info("Retrieved {} campaigns", campaigns.size());

        return campaigns;
    }

    @Transactional(readOnly = true)
    public Campaign getById(Long id) {
        logger.info("Fetching campaign. Id={}", id);

        return campaignRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Campaign not found. Id={}", id);

                    return new RuntimeException(
                            "Campaign not found with id: " + id);
                });
    }

    @Transactional(readOnly = true)
    public List<Campaign> getByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            logger.warn("Campaign status filter is missing");
            throw new IllegalArgumentException(
                    "Campaign status is required");
        }

        String normalizedStatus = status.trim().toUpperCase();

        logger.info(
                "Fetching campaigns by status {}",
                normalizedStatus);

        List<Campaign> campaigns =
                campaignRepository.findByStatus(normalizedStatus);

        logger.info(
                "Retrieved {} campaigns with status {}",
                campaigns.size(), normalizedStatus);

        return campaigns;
    }

    @Transactional(readOnly = true)
    public List<Campaign> search(String name) {
        if (name == null || name.trim().isEmpty()) {
            logger.info("Campaign search is empty; fetching all campaigns");
            return getAll();
        }

        logger.info("Searching campaigns by name");

        List<Campaign> campaigns =
                campaignRepository.findByCampaignNameContainingIgnoreCase(
                        name.trim());

        logger.info(
                "Campaign search returned {} results",
                campaigns.size());

        return campaigns;
    }

    @Transactional
    public Campaign update(Long id, CampaignRequest request) {
        logger.info("Starting campaign update. Id={}", id);

        if (request == null) {
            logger.error(
                    "Campaign update failed because the request is null. Id={}",
                    id);
            throw new IllegalArgumentException("Campaign request is required");
        }

        Campaign campaign = getById(id);

        logger.info(
                "Campaign loaded for update. Id={}, CurrentStatus={}",
                id, campaign.getStatus());

        if (!"DRAFT".equals(campaign.getStatus())) {
            logger.warn(
                    "Campaign update rejected. Id={}, Status={}",
                    id, campaign.getStatus());

            throw new IllegalStateException(
                    "Only draft campaigns can be updated");
        }

        Template template = templateService.getById(request.getTemplateId());

        logger.info(
                "Template loaded for campaign update. TemplateId={}, Status={}",
                template.getTemplateId(), template.getStatus());

        if (!"APPROVED".equals(template.getStatus())) {
            logger.warn(
                    "Campaign update rejected because template {} has status {}",
                    request.getTemplateId(), template.getStatus());

            throw new IllegalStateException(
                    "Campaigns can only use approved templates");
        }

        campaign.setCampaignName(request.getCampaignName().trim());

        if (request.getSenderName() != null) {
            campaign.setSenderName(request.getSenderName().trim());
        } else {
            campaign.setSenderName(null);
        }

        campaign.setListId(normalizeListIds(request.getListId()));
        campaign.setTemplate(template);

        logger.info("Saving campaign update. Id={}", id);

        Campaign updatedCampaign = campaignRepository.save(campaign);

        logger.info(
                "Campaign updated successfully. Id={}, Status={}",
                id, updatedCampaign.getStatus());

        return updatedCampaign;
    }

    @Transactional
    public Campaign submit(Long id) {
        logger.info("Submitting campaign for review. Id={}", id);

        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            logger.warn(
                    "Campaign submission rejected. Id={}, Status={}",
                    id, campaign.getStatus());

            throw new IllegalStateException(
                    "Only draft campaigns can be submitted");
        }

        campaign.setStatus("PENDING");
        campaign.setRejectionComment(null);

        Campaign submittedCampaign = campaignRepository.save(campaign);

        logger.info(
                "Campaign submitted successfully. Id={}, Status={}",
                id, submittedCampaign.getStatus());

        return submittedCampaign;
    }

    @Transactional
    public Campaign approve(Long id) {
        logger.info("Campaign approval requested. Id={}", id);

        Campaign campaign = getById(id);

        if (!"PENDING".equals(campaign.getStatus())) {
            logger.warn(
                    "Campaign approval rejected. Id={}, Status={}",
                    id, campaign.getStatus());

            throw new IllegalStateException(
                    "Only pending campaigns can be approved");
        }

        campaign.setStatus("APPROVED");
        campaign.setApprovedAt(LocalDateTime.now());
        campaign.setRejectionComment(null);

        Campaign approvedCampaign = campaignRepository.save(campaign);

        logger.info(
                "Campaign approved and saved. Id={}, Status={}",
                id, approvedCampaign.getStatus());

        logger.info(
                "Starting message generation for approved campaign {}",
                id);

        messageService.generateMessagesForCampaign(
                approvedCampaign.getCampaignId());

        logger.info(
                "Message generation requested for approved campaign {}",
                id);

        return approvedCampaign;
    }

    @Transactional
    public Campaign reject(Long id, String rejectionComment) {
        logger.info("Campaign rejection requested. Id={}", id);

        Campaign campaign = getById(id);

        if (!"PENDING".equals(campaign.getStatus())) {
            logger.warn(
                    "Campaign rejection rejected because its status is {}. Id={}",
                    campaign.getStatus(), id);

            throw new IllegalStateException(
                    "Only pending campaigns can be rejected");
        }

        if (rejectionComment == null
                || rejectionComment.trim().isEmpty()) {
            logger.warn(
                    "Campaign rejection failed because a comment is required. Id={}",
                    id);

            throw new IllegalArgumentException(
                    "Rejection comment is required");
        }

        campaign.setStatus("REJECTED");
        campaign.setApprovedAt(LocalDateTime.now());
        campaign.setRejectionComment(rejectionComment.trim());

        Campaign rejectedCampaign = campaignRepository.save(campaign);

        logger.info(
                "Campaign rejected successfully. Id={}, Status={}",
                id, rejectedCampaign.getStatus());

        return rejectedCampaign;
    }

    @Transactional
    public void delete(Long id) {
        logger.info("Campaign deletion requested. Id={}", id);

        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            logger.warn(
                    "Campaign deletion rejected. Id={}, Status={}",
                    id, campaign.getStatus());

            throw new IllegalStateException(
                    "Only DRAFT campaigns can be deleted");
        }

        campaignRepository.delete(campaign);

        logger.info("Campaign deleted successfully. Id={}", id);
    }

    private String normalizeListIds(String listIds) {
        logger.info("Normalizing campaign contact-list IDs");

        if (listIds == null || listIds.trim().isEmpty()) {
            logger.warn("Campaign validation failed: no contact lists provided");

            throw new IllegalArgumentException(
                    "At least one contact list is required");
        }

        String normalizedListIds = Arrays.stream(listIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .reduce((first, second) -> first + "," + second)
                .orElseThrow(() -> {
                    logger.warn("No valid contact-list IDs were provided");

                    return new IllegalArgumentException(
                            "At least one contact list is required");
                });

        logger.info(
                "Contact-list IDs normalized successfully. ListIds={}",
                normalizedListIds);

        return normalizedListIds;
    }
}
