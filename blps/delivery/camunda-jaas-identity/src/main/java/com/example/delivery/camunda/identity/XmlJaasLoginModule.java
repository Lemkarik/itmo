package com.example.delivery.camunda.identity;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.mindrot.jbcrypt.BCrypt;

public class XmlJaasLoginModule implements LoginModule {

    static final String USER_STORE_OPTION = "userStore";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private XmlUserStore userStore;
    private XmlUserStore.UserAccount account;
    private char[] password;

    @Override
    public void initialize(
            Subject subject,
            CallbackHandler callbackHandler,
            Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.userStore = (XmlUserStore) options.get(USER_STORE_OPTION);
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback name = new NameCallback("Username");
        PasswordCallback passwordCallback = new PasswordCallback("Password", false);
        try {
            callbackHandler.handle(new Callback[] {name, passwordCallback});
        } catch (IOException | UnsupportedCallbackException exception) {
            throw loginException("Cannot read credentials", exception);
        }
        password = passwordCallback.getPassword();
        account = userStore.find(name.getName());
        if (account == null) {
            clear();
            throw new LoginException("Unknown delivery user");
        }
        if (password == null || !matches(password, account.password())) {
            clear();
            throw new LoginException("Invalid password");
        }
        return true;
    }

    @Override
    public boolean commit() {
        if (account == null) {
            return false;
        }
        subject.getPrincipals().add(new DeliveryPrincipal(account.username()));
        subject.getPrincipals().add(new DeliveryRolePrincipal(account.role()));
        clearPassword();
        return true;
    }

    @Override
    public boolean abort() {
        clear();
        return true;
    }

    @Override
    public boolean logout() {
        subject.getPrincipals().removeIf(principal ->
                principal instanceof DeliveryPrincipal || principal instanceof DeliveryRolePrincipal);
        clear();
        return true;
    }

    private void clear() {
        account = null;
        clearPassword();
    }

    private void clearPassword() {
        if (password != null) {
            Arrays.fill(password, '\0');
            password = null;
        }
    }

    private boolean matches(char[] rawPassword, String passwordHash) {
        try {
            return passwordHash != null
                    && passwordHash.startsWith("$2")
                    && BCrypt.checkpw(new String(rawPassword), passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private LoginException loginException(String message, Exception cause) {
        LoginException exception = new LoginException(message);
        exception.initCause(cause);
        return exception;
    }

    private record DeliveryPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }

    private record DeliveryRolePrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
