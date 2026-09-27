package com.wcm.campaign.service;

import com.wcm.campaign.dto.CampaignRequest;
import com.wcm.campaign.model.Campaign;
import com.wcm.campaign.model.Template;
import com.wcm.campaign.model.User;
import com.wcm.campaign.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final TemplateService templateService;
    private final UserService userService;

    public CampaignService(CampaignRepository campaignRepository, TemplateService templateService, UserService userService) {
        this.campaignRepository = campaignRepository;
        this.templateService = templateService;
        this.userService = userService;
    }

    @Transactional
    public Campaign createCampaign(CampaignRequest request) {

        Template template = templateService.getById(request.getTemplateId());

        if (!"APPROVED".equals(template.getStatus())) {
            throw new IllegalStateException("Campaign can only use an APPROVED template");
        }

        Campaign campaign = new Campaign();

        campaign.setCampaignName(request.getCampaignName().trim());

        if (request.getSenderName() != null) {
            campaign.setSenderName(request.getSenderName().trim());
        }

        campaign.setListId(normalizeListIds(request.getListId()));

        campaign.setTemplate(template);
        campaign.setStatus("DRAFT");

        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public List<Campaign> getAll() {
        return campaignRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Campaign getById(Long id) {

        return campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Campaign> getByStatus(String status) {
        return campaignRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<Campaign> search(String name) {

        if (name == null || name.trim().isEmpty()) {
            return getAll();
        }

        return campaignRepository.findByCampaignNameContainingIgnoreCase(name.trim());
    }

    @Transactional
    public Campaign update(Long id, CampaignRequest request) {

        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            throw new IllegalStateException("Only DRAFT campaigns can be updated");
        }

        Template template = templateService.getById(request.getTemplateId());

        if (!"APPROVED".equals(template.getStatus())) {
            throw new IllegalStateException("Campaign can only use an APPROVED template");
        }

        campaign.setCampaignName(request.getCampaignName().trim());

        if (request.getSenderName() != null) {
            campaign.setSenderName(request.getSenderName().trim());
        } else {
            campaign.setSenderName(null);
        }

        campaign.setListId(normalizeListIds(request.getListId())
        );

        campaign.setTemplate(template);

        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign submit(Long id) {

        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            throw new IllegalStateException("Only DRAFT campaigns can be submitted");
        }

        campaign.setStatus("PENDING");

        campaign.setRejectionComment(null);

        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign approve(Long id, Long approvingUserId) {

        Campaign campaign = getById(id);

        User user = userService.getUserById(approvingUserId);

        validateApprover(user);

        if (!"PENDING".equals(campaign.getStatus())) {
            throw new IllegalStateException("Only PENDING campaigns can be approved");
        }

        campaign.setStatus("APPROVED");
        campaign.setApprovedBy(user);
        campaign.setApprovedAt(LocalDateTime.now());
        campaign.setRejectionComment(null);

        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign reject(Long id, Long rejectingUserId, String rejectionComment) {

        Campaign campaign = getById(id);

        User user = userService.getUserById(rejectingUserId);

        validateApprover(user);

        if (!"PENDING".equals(campaign.getStatus())) {
            throw new IllegalStateException("Only PENDING campaigns can be rejected");
        }

        if (rejectionComment == null || rejectionComment.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection comment is required");
        }

        campaign.setStatus("REJECTED");
        campaign.setApprovedBy(user);
        campaign.setApprovedAt(LocalDateTime.now());
        campaign.setRejectionComment(rejectionComment.trim());

        return campaignRepository.save(campaign);
    }

    @Transactional
    public void delete(Long id) {

        Campaign campaign = getById(id);

        if (!"DRAFT".equals(campaign.getStatus())) {
            throw new IllegalStateException("Only DRAFT campaigns can be deleted");
        }

        campaignRepository.delete(campaign);
    }

    private void validateApprover(User user) {

        String role = user.getRole();

        if (!"ADMIN".equals(role) && !"MANAGER".equals(role)) {
            throw new IllegalStateException("Only ADMIN or MANAGER can approve/reject campaigns");
        }
    }

    private String normalizeListIds(String listIds) {

        if (listIds == null || listIds.trim().isEmpty()) {
            throw new IllegalArgumentException("At least one contact list is required");
        }

        return java.util.Arrays.stream(listIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .reduce((first, second) -> first + "," + second)
                .orElseThrow(() -> new IllegalArgumentException("At least one contact list is required"));
    }
}