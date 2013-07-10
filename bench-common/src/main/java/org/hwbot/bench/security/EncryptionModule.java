package org.hwbot.bench.security;

import org.hwbot.bench.model.Request;

public interface EncryptionModule {

    public void addChecksum(Request request);

    public byte[] encrypt(byte[] bytes);

}
