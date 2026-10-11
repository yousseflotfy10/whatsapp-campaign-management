package com.wcm.campaign.config;

import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.PermissionEntity;
import com.wcm.campaign.model.Role;
import com.wcm.campaign.model.User;
import com.wcm.campaign.repository.PermissionRepository;
import com.wcm.campaign.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initializeData(
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            logger.info("Starting application data initialization");

            for (Permission permission : Permission.values()) {
                if (permissionRepository.findByPermissionName(permission).isEmpty()) {
                    PermissionEntity permissionEntity = new PermissionEntity(permission, createDescription(permission));
                    permissionRepository.save(permissionEntity);
                    logger.info("Permission initialized: {}", permission.name());
                }
            }

            if (!userRepository.existsByRole(Role.ADMIN)) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@system.com");
                admin.setPasswordHashed(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);
                admin.setStatus(User.AccountStatus.ACTIVE);
                admin.setApprovedAt(java.time.LocalDateTime.now());
                userRepository.save(admin);

                logger.info("System administrator created successfully");
            } else {
                userRepository.findByUsername("admin").ifPresent(admin -> {
                    if (admin.getStatus() != User.AccountStatus.ACTIVE) {
                        admin.setStatus(User.AccountStatus.ACTIVE);
                        admin.setApprovedAt(java.time.LocalDateTime.now());
                        userRepository.save(admin);
                        logger.info("System administrator account enabled");
                    }
                });
                logger.info("System administrator already exists");
            }

            logger.info("Application data initialization completed");
        };
    }

    private String createDescription(Permission permission) {
        return switch (permission) {
            case USER_VIEW -> "View users";
            case USER_CREATE -> "Create users";
            case USER_UPDATE -> "Update users";
            case USER_DELETE -> "Delete users";

            case CONTACT_VIEW -> "View contacts";
            case CONTACT_CREATE -> "Create contacts";
            case CONTACT_UPDATE -> "Update contacts";
            case CONTACT_DELETE -> "Delete contacts";

            case TEMPLATE_VIEW -> "View templates";
            case TEMPLATE_CREATE -> "Create templates";
            case TEMPLATE_UPDATE -> "Update templates";
            case TEMPLATE_APPROVE -> "Approve templates";
            case TEMPLATE_REJECT -> "Reject templates";

            case CAMPAIGN_VIEW -> "View campaigns";
            case CAMPAIGN_CREATE -> "Create campaigns";
            case CAMPAIGN_UPDATE -> "Update campaigns";
            case CAMPAIGN_DELETE -> "Delete campaigns";
            case CAMPAIGN_SUBMIT -> "Submit campaigns";
            case CAMPAIGN_APPROVE -> "Approve campaigns";
            case CAMPAIGN_REJECT -> "Reject campaigns";

            case MESSAGE_VIEW -> "View messages";
            case MESSAGE_SEND -> "Send messages";

            case REPORT_VIEW -> "View reports";
            case REPORT_EXPORT -> "Export reports";
        };
    }
}