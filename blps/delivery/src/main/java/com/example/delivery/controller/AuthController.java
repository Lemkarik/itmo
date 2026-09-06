package com.example.delivery.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.delivery.config.JwtUserDetails;
import com.example.delivery.config.XmlUserDetailsService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.webapp.impl.security.auth.Authentications;
import org.camunda.bpm.webapp.impl.security.auth.AuthenticationUtil;
import org.camunda.bpm.webapp.impl.security.auth.UserAuthentication;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ProcessEngine processEngine;
    private final XmlUserDetailsService xmlUserDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          ProcessEngine processEngine,
                          XmlUserDetailsService xmlUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.processEngine = processEngine;
        this.xmlUserDetailsService = xmlUserDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {

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

        List<String> camundaGroups = "ADMIN".equals(user.getRole())
                ? List.of("ADMIN", "camunda-admin")
                : List.of(user.getRole());
        UserAuthentication camundaAuthentication = new UserAuthentication(
                user.getUsername(),
                processEngine.getName());
        camundaAuthentication.setGroupIds(camundaGroups);
        camundaAuthentication.setTenantIds(List.of());
        camundaAuthentication.setAuthorizedApps(authorizedCamundaApps(user.getRole()));
        AuthenticationUtil.revalidateSession(servletRequest, camundaAuthentication);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie(token).toString())
                .body(Map.of(
                        "token", token,
                        "username", user.getUsername(),
                        "role", user.getRole()));
    }

    @PostMapping("/camunda-token")
    public ResponseEntity<?> createTokenFromCamundaSession(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).build();
        }

        Authentications authentications = AuthenticationUtil.getAuthsFromSession(session);
        org.camunda.bpm.webapp.impl.security.auth.Authentication camundaAuthentication =
                authentications.getAuthenticationForProcessEngine(processEngine.getName());
        if (camundaAuthentication == null) {
            return ResponseEntity.status(401).build();
        }

        String username = camundaAuthentication.getIdentityId();
        XmlUserDetailsService.XmlUser xmlUser = xmlUserDetailsService.findByUsername(username);
        Long userId = xmlUserDetailsService.resolveDatabaseUserId(username);
        if (xmlUser == null || userId == null) {
            return ResponseEntity.status(401).build();
        }

        String token = jwtUtil.generateToken(username, xmlUser.role(), userId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, jwtCookie(token).toString())
                .build();
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
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            Authentications authentications = AuthenticationUtil.getAuthsFromSession(session);
            authentications.removeByEngineName(processEngine.getName());
            AuthenticationUtil.updateSession(session, authentications);
        }
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

    private Set<String> authorizedCamundaApps(String role) {
        return switch (role) {
            case "ADMIN" -> Set.of("welcome", "admin", "cockpit", "tasklist");
            case "SHOP", "CUSTOMER", "COURIER" -> Set.of("welcome", "tasklist");
            default -> Set.of("welcome");
        };
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
