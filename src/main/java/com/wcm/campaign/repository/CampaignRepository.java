package com.wcm.campaign.repository;

import com.wcm.campaign.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatus(String status);

    List<Campaign> findByCampaignNameContainingIgnoreCase(String campaignName);
}