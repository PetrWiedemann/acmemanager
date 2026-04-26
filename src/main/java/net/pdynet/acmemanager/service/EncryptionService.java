package net.pdynet.acmemanager.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionService {
	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int TAG_LENGTH_BIT = 128;
	private static final int IV_LENGTH_BYTE = 12;

	private final SecretKey masterKey;
	private final SecureRandom secureRandom = new SecureRandom();

	public EncryptionService(SecretKey masterKey) {
		this.masterKey = masterKey;
	}

	public String encrypt(String plainText) throws Exception {
		byte[] iv = new byte[IV_LENGTH_BYTE];
		secureRandom.nextBytes(iv);

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
		cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

		byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

		ByteBuffer bb = ByteBuffer.allocate(iv.length + cipherText.length);
		bb.put(iv);
		bb.put(cipherText);

		return Base64.getEncoder().encodeToString(bb.array());
	}

	public String decrypt(String encryptedBase64) throws Exception {
		byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
		ByteBuffer bb = ByteBuffer.wrap(decoded);

		byte[] iv = new byte[IV_LENGTH_BYTE];
		bb.get(iv);

		byte[] cipherText = new byte[bb.remaining()];
		bb.get(cipherText);

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
		cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);

		byte[] plainText = cipher.doFinal(cipherText);
		return new String(plainText, StandardCharsets.UTF_8);
	}
}