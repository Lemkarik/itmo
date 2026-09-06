package com.example.delivery.config;

import java.io.IOException;
import java.util.List;

import org.camunda.bpm.engine.IdentityService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CamundaAuthenticationFilter extends OncePerRequestFilter {

    private final IdentityService identityService;

    public CamundaAuthenticationFilter(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserDetails user) {
            List<String> groups = "ADMIN".equals(user.getRole())
                    ? List.of("ADMIN", "camunda-admin")
                    : List.of(user.getRole());
            identityService.setAuthentication(user.getUsername(), groups);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            identityService.clearAuthentication();
        }
    }
}
