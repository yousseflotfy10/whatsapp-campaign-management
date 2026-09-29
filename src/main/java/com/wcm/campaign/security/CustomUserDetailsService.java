package com.wcm.campaign.security;

import com.wcm.campaign.model.Permission;
import com.wcm.campaign.model.Role;
import com.wcm.campaign.model.User;
import com.wcm.campaign.model.UserPermission;
import com.wcm.campaign.repository.UserRepository;
import com.wcm.campaign.repository.UserPermissionRepository;
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

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;

    public CustomUserDetailsService(UserRepository userRepository, UserPermissionRepository userPermissionRepository) {
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();

      // Assign Role
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

       // Admin receive Permissions
        if (user.getRole() == Role.ADMIN) {
            for (Permission permission : Permission.values()) {
                authorities.add(new SimpleGrantedAuthority(permission.name()));
            }

        } else {
            List<UserPermission> userPermissions = userPermissionRepository.findByUser(user);

            for (UserPermission userPermission : userPermissions) {

                authorities.add(
                        new SimpleGrantedAuthority(
                                userPermission
                                        .getPermission()
                                        .getPermissionName()
                                        .name()
                        )
                );
            }
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHashed())
                .authorities(authorities)
                .build();
    }
}