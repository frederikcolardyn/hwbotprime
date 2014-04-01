package org.hwbot.prime.service;

import org.hwbot.api.bench.dto.PersistentLoginDTO;

public class AuthenticationService {

    protected PersistentLoginDTO credentials;
    protected static AuthenticationService service;

    private AuthenticationService() {
    }

    public static AuthenticationService getInstance() {
        if (service == null) {
            service = new AuthenticationService();
        }
        return service;
    }

    public PersistentLoginDTO getCredentials() {
        return credentials;
    }

    public void setCredentials(PersistentLoginDTO credentials) {
        this.credentials = credentials;
    }

}
