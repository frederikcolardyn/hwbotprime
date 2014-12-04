package org.hwbot.prime.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.hwbot.api.bench.dto.PersistentLoginDTO;
import org.hwbot.bench.security.EncryptionModule;
import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.api.PersistentLoginAware;
import org.hwbot.prime.tasks.LoginTokenTask;

import android.content.Context;
import android.util.Log;

public class SecurityService {

    protected static SecurityService service;
    protected static EncryptionModule encryptionModule;
    private PersistentLoginDTO credentials;
    private String checksum, checksumbase;
    private char[] checksumChars;

    private SecurityService() {
        if (encryptionModule != null) {
            Log.i(this.getClass().getSimpleName(), "Using encryption module: " + encryptionModule);
        } else {
            // no encryption
            Log.e(this.getClass().getSimpleName(), "No encryption module found.");
        }
    }

    public static void setEncryptionModule(EncryptionModule encryptionModule) {
        SecurityService.encryptionModule = encryptionModule;
    }

    public static SecurityService getInstance() {
        if (service == null) {
            service = new SecurityService();
        }
        return service;
    }

    public void updateChecksum(Number score) {
        try {
            checksum = toSHA1((checksumbase + score).getBytes("UTF8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        checksumChars = checksum.toCharArray();
    }

    public static String toSHA1(byte[] string) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException();
        }
        return Hex.encodeHexString(md.digest(string));
    }

    public PersistentLoginDTO getCredentials() {
        return credentials;
    }

    public void setCredentials(PersistentLoginDTO credentials) {
        if (credentials != null && credentials.getUserId() == null) {
            // Log.e(this.getClass().getSimpleName(), "Empty credentials, user id is required.");
        }
        this.credentials = credentials;
    }

    public EncryptionModule getEncryptionModule() {
        return encryptionModule;
    }

    public byte[] encrypt(String xml, Context ctx) {
        try {
            byte[] bytes;
            if (encryptionModule != null) {
                // Log.i(this.getClass().getSimpleName(), "Encrypting xml:\n" + xml + "\n==== end xml =====");
                bytes = encryptionModule.encrypt(xml.getBytes("utf8"), ctx);
            } else {
                // Log.e(this.getClass().getSimpleName(), "Encryption disabled!");
                bytes = xml.getBytes("utf8");
            }
            return bytes;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadToken(NetworkStatusAware networkStatusAware, PersistentLoginAware persistentLoginAware, String token) {
        if (isLoggedIn()) {
            org.hwbot.bench.prime.Log.info(this.getClass().getSimpleName(), "Not loading credentials, already logged in.");
            return;
        }
        if (StringUtils.isNotEmpty(token)) {
            org.hwbot.bench.prime.Log.info(this.getClass().getSimpleName(), "Loading credentials for token " + token);
            new LoginTokenTask(networkStatusAware, persistentLoginAware, token).execute((Void) null);
        }
    }

    public boolean isLoggedIn() {
        if (getCredentials() == null || getCredentials().getUserId() == null) {
            return false;
        } else {
            return true;
        }
    }

}
