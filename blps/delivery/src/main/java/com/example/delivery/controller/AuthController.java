package com.example.delivery.controller;

import java.util.Map;

import com.example.delivery.config.JwtUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.config.JwtUtil;
import com.example.delivery.config.XmlUserDetailsService;
import com.example.delivery.config.XmlUserDetailsService.XmlUser;
import com.example.delivery.dto.LoginRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final XmlUserDetailsService xmlUserDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          XmlUserDetailsService xmlUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.xmlUserDetailsService = xmlUserDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()));

        JwtUserDetails user =
                (JwtUserDetails) authentication.getPrincipal();

        String token =
                jwtUtil.generateToken(
                        user.getUsername(),
                        user.getRole(),
                        user.getUserId());

        return ResponseEntity.ok(Map.of("token", token));
    }
}
