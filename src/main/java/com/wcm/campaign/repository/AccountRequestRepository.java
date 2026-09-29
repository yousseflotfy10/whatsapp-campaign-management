package com.wcm.campaign.repository;

import com.wcm.campaign.model.AccountRequest;
import com.wcm.campaign.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {

    Optional<AccountRequest> findByUsername(String username);

    Optional<AccountRequest> findByEmail(String email);

    List<AccountRequest> findByStatus(RequestStatus status);

    boolean existsByUsernameAndStatus(String username, RequestStatus status);

    boolean existsByEmailAndStatus(String email, RequestStatus status);
}