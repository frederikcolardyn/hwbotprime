package org.hwbot.prime.service;

import org.hwbot.prime.model.PersistentLogin;

public class AuthenticationService {

    protected PersistentLogin credentials;
    protected static AuthenticationService service;

    private AuthenticationService() {
    }

    public static AuthenticationService getInstance() {
        if (service == null) {
            service = new AuthenticationService();
        }
        return service;
    }

    public PersistentLogin getCredentials() {
        return credentials;
    }

    public void setCredentials(PersistentLogin credentials) {
        this.credentials = credentials;
    }

}
