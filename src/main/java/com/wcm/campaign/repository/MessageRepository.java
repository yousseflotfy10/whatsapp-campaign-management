package com.wcm.campaign.repository;

import com.wcm.campaign.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByCampaign_CampaignId(Long campaignId);

    List<Message> findByContact_ContactId(Long contactId);

    List<Message> findByStatus(String status);
}