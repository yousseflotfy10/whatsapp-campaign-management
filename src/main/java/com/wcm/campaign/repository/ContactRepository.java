package com.wcm.campaign.repository;

import com.wcm.campaign.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByContactNameContainingIgnoreCase(String contactName);

    List<Contact> findByIsActive(Boolean isActive);

    Optional<Contact> findFirstByListHash(String listHash);
}