package com.wcm.campaign.security;

import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.Role;
import com.wcm.campaign.model.User;
import com.wcm.campaign.model.UserPermission;
import com.wcm.campaign.repository.UserPermissionRepository;
import com.wcm.campaign.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;

    public CustomUserDetailsService(UserRepository userRepository, UserPermissionRepository userPermissionRepository) {
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        logger.info("Loading user details for username={}", username);

        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            logger.warn("User not found for username={}", username);
            return new UsernameNotFoundException("User not found: " + username);
        });

        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        if (user.getRole() == Role.ADMIN) {
            for (Permission permission : Permission.values()) {
                authorities.add(new SimpleGrantedAuthority(permission.name()));
            }

            logger.info("Loaded administrator permissions for username={}", username);
        } else {
            List<UserPermission> userPermissions = userPermissionRepository.findByUser(user);

            for (UserPermission userPermission : userPermissions) {
                authorities.add(new SimpleGrantedAuthority(
                        userPermission.getPermission().getPermissionName().name()));
            }

            logger.info("Loaded {} permissions for username={}", userPermissions.size(), username);
        }

        logger.info("User details loaded successfully for username={}", username);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHashed())
                .authorities(authorities)
                .disabled(user.getStatus() != User.AccountStatus.ACTIVE)
                .build();
    }
}