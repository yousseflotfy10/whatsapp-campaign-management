package com.wcm.campaign.repository;

import com.wcm.campaign.model.Contact;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    @EntityGraph(attributePaths = {"contactLists"})
    List<Contact> findAll();

    @EntityGraph(attributePaths = {"contactLists"})
    List<Contact> findByIsActive(Boolean isActive);

    @EntityGraph(attributePaths = {"contactLists"})
    Optional<Contact> findById(Long id);

    @EntityGraph(attributePaths = {"contactLists"})
    List<Contact> findByContactNameContainingIgnoreCase(String contactName);

    Optional<Contact> findFirstByContactNumbersAndIsActiveTrue(String contactNumbers);
}