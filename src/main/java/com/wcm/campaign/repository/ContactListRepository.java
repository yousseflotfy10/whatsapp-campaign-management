package com.wcm.campaign.repository;

import com.wcm.campaign.model.ContactList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContactListRepository extends JpaRepository<ContactList, String> {

    Optional<ContactList> findByListHash(String listHash);

    boolean existsByListHash(String listHash);
}