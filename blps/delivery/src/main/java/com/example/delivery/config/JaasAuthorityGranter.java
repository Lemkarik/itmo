package com.example.delivery.config;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.jaas.AuthorityGranter;

import com.example.delivery.model.Privilege;
import com.example.delivery.model.UserRole;


public class JaasAuthorityGranter implements AuthorityGranter {

    @Override
    public Set<String> grant(Principal principal) {
        if (principal instanceof JaasUserPrincipal jaasPrincipal) {
            UserRole role = jaasPrincipal.getRole();
            Set<Privilege> privileges = PrivilegeMapping.getPrivilegesForRole(role);
            Set<String> authorities = privileges.stream()
                    .map(p -> "PRIVILEGE_" + p.name())
                    .collect(Collectors.toSet());
            authorities.add("ROLE_" + role.name());
            return authorities;
        }
        return Collections.emptySet();
    }
}
