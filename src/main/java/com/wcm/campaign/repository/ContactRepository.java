package com.wcm.campaign.repository;

import com.wcm.campaign.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByContactNameContainingIgnoreCase(String contactName);

    List<Contact> findByIsActive(Boolean isActive);

    Optional<Contact> findFirstByListHash(String listHash);

    Optional<Contact> findFirstByContactNumbersAndIsActiveTrue(String contactNumbers);

    List<Contact> findByListIdIn(Collection<String> listIds);

    @Query("select distinct c.listId from Contact c where c.isActive = true and c.listId is not null order by c.listId")
    List<String> findActiveListIds();

}