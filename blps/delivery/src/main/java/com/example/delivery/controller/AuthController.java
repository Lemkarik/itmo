package com.example.delivery.controller;

import java.util.Map;

import com.example.delivery.config.JwtUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.config.JwtUtil;
import com.example.delivery.dto.LoginRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

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

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie(token).toString())
                .body(Map.of(
                        "token", token,
                        "username", user.getUsername(),
                        "role", user.getRole()));
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        JwtUserDetails user = (JwtUserDetails) authentication.getPrincipal();
        return Map.of(
                "username", user.getUsername(),
                "role", user.getRole(),
                "userId", user.getUserId());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from("DELIVERY_JWT", "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseCookie jwtCookie(String token) {
        return ResponseCookie.from("DELIVERY_JWT", token)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(java.time.Duration.ofMillis(jwtUtil.getExpirationMs()))
                .build();
    }
}
