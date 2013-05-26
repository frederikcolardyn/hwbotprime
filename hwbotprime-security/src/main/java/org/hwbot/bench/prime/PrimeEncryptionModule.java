package org.hwbot.bench.prime;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.math.NumberUtils;
import org.hwbot.bench.model.Request;
import org.hwbot.bench.security.EncryptionModule;

public class PrimeEncryptionModule implements EncryptionModule {

	public char[] getIv() {
		return new char[] { '0', '0', 'B', 'C', 'A', 'E', '3', '6', '2', '7', '4', '2', 'F', '3', '1', '6', '8', '2', '9', '4', '3', '7', '7', 'E', '8', '7',
				'1', '1', '0', '1', '3', 'F' };
	}

	public char[] getKey() {
		return new char[] { 'D', 'C', '4', 'D', 'E', '0', 'C', '6', '7', 'E', 'F', '1', '0', 'A', '1', '4', '3', '0', 'B', '0', '5', 'B', '2', '8', '9', '9',
				'6', 'E', 'A', 'B', '6', '4' };
	}

	public void addChecksum(Request request) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(request.getApplication().getName());
		buffer.append("-");
		buffer.append(request.getApplication().getVersion());
		buffer.append("-");
		buffer.append((int) (NumberUtils.toFloat(request.getScore().getPoints()) * 1000));
		buffer.append("-");
		buffer.append(request.getHardware().getProcessor().getName());
		try {
			request.setApplicationChecksum(toSHA1(buffer.toString().getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
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

}
