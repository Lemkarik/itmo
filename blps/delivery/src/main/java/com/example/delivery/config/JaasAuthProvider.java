package com.example.delivery.config;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.example.delivery.model.Privilege;
import com.example.delivery.model.UserRole;

import javax.security.auth.Subject;


@Component
public class JaasAuthProvider implements AuthenticationProvider {

    private final JaasAuthenticationProvider delegate;

    public JaasAuthProvider(JaasAuthenticationProvider jaasAuthenticationProvider) {
        this.delegate = jaasAuthenticationProvider;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        Authentication jaasAuth = delegate.authenticate(authentication);

        if (!(jaasAuth instanceof org.springframework.security.authentication.jaas.JaasAuthenticationToken token)) {
            throw new BadCredentialsException(
                    "Expected JaasAuthenticationToken but got "
                            + jaasAuth.getClass().getName());
        }

        JaasUserPrincipal principal = token.getLoginContext().getSubject()
                .getPrincipals(JaasUserPrincipal.class)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "JaasUserPrincipal not found in Subject"));

        UserRole role = principal.getRole();

        Collection<? extends GrantedAuthority> authorities =
                jaasAuth.getAuthorities();

        JwtUserDetails userDetails =
                new JwtUserDetails(
                        principal.getName(),
                        role.name(),
                        principal.getUserId(),
                        authorities);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}

