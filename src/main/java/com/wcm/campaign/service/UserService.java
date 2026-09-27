package com.wcm.campaign.service;

import com.wcm.campaign.dto.SignupRequest;
import com.wcm.campaign.dto.UserRequest;
import com.wcm.campaign.model.User;
import com.wcm.campaign.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User signup(SignupRequest request) {

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {

            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);

        user.setPasswordHashed(passwordEncoder.encode(request.getPassword()));

        user.setRole("EMPLOYEE");

        user.setPermissions(null);

        return userRepository.save(user);
    }

    @Transactional
    public User createUser(UserRequest request) {

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String role = request.getRole().trim().toUpperCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);

        user.setPasswordHashed(passwordEncoder.encode(request.getPassword()));

        user.setRole(role);
        user.setPermissions(request.getPermissions());

        return userRepository.save(user);
    }


    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {

        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }


    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {

        return userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {

        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Transactional
    public User updateUser(Long id, UserRequest request) {

        User user = getUserById(id);

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String role = request.getRole().trim().toUpperCase();

        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setUsername(username);
        user.setEmail(email);

        user.setPasswordHashed(passwordEncoder.encode(request.getPassword()));

        user.setRole(role);
        user.setPermissions(request.getPermissions());

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {

        User user = getUserById(id);

        userRepository.delete(user);
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}