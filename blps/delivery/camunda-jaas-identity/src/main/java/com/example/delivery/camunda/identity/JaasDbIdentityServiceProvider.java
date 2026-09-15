package com.example.delivery.camunda.identity;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.camunda.bpm.engine.impl.identity.db.DbIdentityServiceProvider;

final class JaasDbIdentityServiceProvider extends DbIdentityServiceProvider {

    private static final String LOGIN_CONTEXT = "DeliveryLogin";
    private static final Logger LOGGER = Logger.getLogger(JaasDbIdentityServiceProvider.class.getName());

    private final Configuration configuration;

    JaasDbIdentityServiceProvider(XmlUserStore userStore) {
        configuration = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                if (!LOGIN_CONTEXT.equals(name)) {
                    return null;
                }
                return new AppConfigurationEntry[] {new AppConfigurationEntry(
                        XmlJaasLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        Map.of(XmlJaasLoginModule.USER_STORE_OPTION, userStore))};
            }
        };
    }

    @Override
    public boolean checkPassword(String userId, String password) {
        if (userId == null || password == null) {
            return false;
        }
        CallbackHandler callbacks = values -> setCredentials(values, userId, password);
        Thread thread = Thread.currentThread();
        ClassLoader originalClassLoader = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(XmlJaasLoginModule.class.getClassLoader());
            LoginContext context = new LoginContext(LOGIN_CONTEXT, null, callbacks, configuration);
            context.login();
            context.logout();
            return true;
        } catch (LoginException exception) {
            LOGGER.log(Level.INFO, "Delivery JAAS rejected '" + userId + "': " + exception.getMessage());
            return false;
        } finally {
            thread.setContextClassLoader(originalClassLoader);
        }
    }

    private void setCredentials(Callback[] callbacks, String username, String password)
            throws UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback name) {
                name.setName(username);
            } else if (callback instanceof PasswordCallback passwordCallback) {
                passwordCallback.setPassword(password.toCharArray());
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }
}
