package org.hwbot.prime.api;

import org.hwbot.api.bench.dto.PersistentLoginDTO;

public interface PersistentLoginAware {

	void notifyPersistentLoginOk(PersistentLoginDTO persistentLogin);

	void notifyPersistentLoginFailed(String message);

}
