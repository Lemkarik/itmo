package com.example.delivery.config;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.example.delivery.model.Privilege;
import com.example.delivery.model.UserRole;


public class JaasLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private String username;
    private UserRole role;
    private boolean succeeded = false;
    private Long userId;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback nameCallback = new NameCallback("Username: ");
        PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
        } catch (IOException | UnsupportedCallbackException e) {
            throw new LoginException("Error handling callbacks: " + e.getMessage());
        }

        this.username = nameCallback.getName();

        char[] passwordChars = passwordCallback.getPassword();

        if (passwordChars == null) {
            throw new LoginException("Password not provided");
        }

        String password = new String(passwordChars);

        XmlUserDetailsService xmlService = XmlUserDetailsServiceHolder.getService();
        if (xmlService == null) {
            throw new LoginException("XmlUserDetailsService not initialized");
        }

        XmlUserDetailsService.XmlUser user = xmlService.findByUsername(username);
        if (user == null || !xmlService.passwordMatches(password, user.password())) {
            succeeded = false;
            throw new LoginException("Invalid username or password");
        }

        this.userId = xmlService.resolveDatabaseUserId(username);
        if (this.userId == null) {
            throw new LoginException("Application user is not initialized for account: " + username);
        }
        this.role = UserRole.valueOf(user.role());
        succeeded = true;
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (!succeeded) {
            return false;
        }
        
        subject.getPrincipals().add(new JaasUserPrincipal(username, role, userId));
        
        subject.getPublicCredentials().add(username);
        subject.getPublicCredentials().add(role.name());
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        succeeded = false;
        username = null;
        role = null;
        userId = null;
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        succeeded = false;
        username = null;
        role = null;
        if (subject != null) {
            subject.getPrincipals().clear();
            subject.getPublicCredentials().clear();
        }
        return true;
    }
}
