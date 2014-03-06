package org.hwbot.prime.api;

import org.hwbot.prime.model.PersistentLogin;

public interface PersistentLoginAware {

	void notifyPersistentLoginOk(PersistentLogin persistentLogin);

	void notifyPersistentLoginFailed(String message);

}
