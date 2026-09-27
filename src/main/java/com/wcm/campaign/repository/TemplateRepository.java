package com.wcm.campaign.repository;

import com.wcm.campaign.model.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findByStatus(String status);

    List<Template> findByTemplateNameContainingIgnoreCase(String templateName);
}