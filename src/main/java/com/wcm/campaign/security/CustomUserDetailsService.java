package com.wcm.campaign.security;

import com.wcm.campaign.model.User;
import com.wcm.campaign.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userRepository.findByEmail(username.toLowerCase())
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHashed())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                .build();
    }
}