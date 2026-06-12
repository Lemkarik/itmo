package com.example.delivery.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.delivery.config.JwtUserDetails;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.exception.NotFoundException;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.repository.UserRepository;


@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public JwtUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof JwtUserDetails)) {
            throw new BadRequestException("Authentication required");
        }
        return (JwtUserDetails) auth.getPrincipal();
    }

    public Long getCurrentUserId() {
        return getCurrentUserDetails().getUserId();
    }

    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Current user not found in database: " + userId));
    }

    public User getCurrentUserWithRole(UserRole role) {
        User user = getCurrentUser();
        if (user.getRole() != role) {
            throw new BadRequestException("User " + user.getId() + " is not a " + role);
        }
        return user;
    }
}
