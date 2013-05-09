package org.hwbot.bench.security;

import org.hwbot.bench.model.Request;

public interface EncryptionModule {

	public char[] getIv();

	public char[] getKey();

	public void addChecksum(Request request);

}
