
package com.wcm.campaign.service;

import com.wcm.campaign.dto.CampaignRequest;
import com.wcm.campaign.model.Campaign;
import com.wcm.campaign.model.ContactList;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.repository.CampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CampaignService {

    private static final Logger logger =
            LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;
    private final TemplateService templateService;
    private final MessageService messageService;
    private final ContactService contactService;

    public CampaignService(
            CampaignRepository campaignRepository,
            TemplateService templateService,
            MessageService messageService,
            ContactService contactService
    ) {
        this.campaignRepository = campaignRepository;
        this.templateService = templateService;
        this.messageService = messageService;
        this.contactService = contactService;
    }

    @Transactional
    public Campaign createCampaign(CampaignRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Campaign request is required");
        }

        if (request.getCampaignName() == null
                || request.getCampaignName().isBlank()) {
            throw new IllegalArgumentException("Campaign name is required");
        }

        Template template = templateService.getById(request.getTemplateId());

        if (!"APPROVED".equals(template.getStatus())) {
            throw new IllegalStateException(
                    "Campaigns can only use approved templates");
        }

        List<String> listIds = getRequestedListIds(request);

        ContactList audience = contactService.createCombinedAudience(
                listIds,
                request.getManualContacts()
        );

        Campaign campaign = new Campaign();
        campaign.setCampaignName(request.getCampaignName().trim());

        if (request.getSenderName() != null) {
            campaign.setSenderName(request.getSenderName().trim());
        }

        campaign.setListId(audience.getListId());
        campaign.setTemplate(template);
        campaign.setStatus("DRAFT");

        Campaign savedCampaign = campaignRepository.save(campaign);

        logger.info(
                "Campaign created: {}, audience: {}",
                savedCampaign.getCampaignId(),
                audience.getListId()
        );

        return savedCampaign;
    }

    @Transactional(readOnly = true)
    public List<Campaign> getAll() {
        return campaignRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Campaign getById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Campaign not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Campaign> getByStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Campaign status is required");
        }

        return campaignRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<Campaign> search(String name) {
        if (name == null || name.isBlank()) {
            return getAll();
        }

        return campaignRepository.findByCampaignNameContainingIgnoreCase(
                name.trim()
        );
    }

    @Transactional
    public Campaign update(Long id, CampaignRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Campaign request is required");
        }

        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            throw new IllegalStateException(
                    "Only draft campaigns can be updated");
        }

        if (request.getCampaignName() == null
                || request.getCampaignName().isBlank()) {
            throw new IllegalArgumentException("Campaign name is required");
        }

        Template template = templateService.getById(request.getTemplateId());

        if (!"APPROVED".equals(template.getStatus())) {
            throw new IllegalStateException(
                    "Campaigns can only use approved templates");
        }

        List<String> listIds = getRequestedListIds(request);

        ContactList audience = contactService.createCombinedAudience(
                listIds,
                request.getManualContacts()
        );

        campaign.setCampaignName(request.getCampaignName().trim());
        campaign.setSenderName(
                request.getSenderName() == null
                        ? null
                        : request.getSenderName().trim()
        );
        campaign.setListId(audience.getListId());
        campaign.setTemplate(template);

        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign submit(Long id) {
        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            throw new IllegalStateException(
                    "Only draft campaigns can be submitted");
        }

        campaign.setStatus("PENDING");
        campaign.setRejectionComment(null);

        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign approve(Long id) {
        Campaign campaign = getById(id);

        if (!"PENDING".equals(campaign.getStatus())) {
            throw new IllegalStateException(
                    "Only pending campaigns can be approved");
        }

        campaign.setStatus("APPROVED");
        campaign.setApprovedAt(LocalDateTime.now());
        campaign.setRejectionComment(null);

        Campaign approvedCampaign = campaignRepository.save(campaign);

        messageService.generateMessagesForCampaign(
                approvedCampaign.getCampaignId()
        );

        return approvedCampaign;
    }

    @Transactional
    public Campaign reject(Long id, String rejectionComment) {
        Campaign campaign = getById(id);

        if (!"PENDING".equals(campaign.getStatus())) {
            throw new IllegalStateException(
                    "Only pending campaigns can be rejected");
        }

        if (rejectionComment == null || rejectionComment.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection comment is required");
        }

        campaign.setStatus("REJECTED");
        campaign.setApprovedAt(LocalDateTime.now());
        campaign.setRejectionComment(rejectionComment.trim());

        return campaignRepository.save(campaign);
    }

    @Transactional
    public void delete(Long id) {
        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            throw new IllegalStateException(
                    "Only DRAFT campaigns can be deleted");
        }

        campaignRepository.delete(campaign);
    }

    private List<String> getRequestedListIds(CampaignRequest request) {
        Set<String> ids = new LinkedHashSet<>();

        if (request.getListIds() != null) {
            for (String id : request.getListIds()) {
                if (id != null && !id.isBlank()) {
                    ids.add(id.trim());
                }
            }
        }

        if (request.getListId() != null && !request.getListId().isBlank()) {
            Arrays.stream(request.getListId().split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .forEach(ids::add);
        }

        return new ArrayList<>(ids);
    }
}
