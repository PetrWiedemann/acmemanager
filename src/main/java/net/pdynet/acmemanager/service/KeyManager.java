package net.pdynet.acmemanager.service;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {
	
	public SecretKey getOrGenerateKey(Path file) throws Exception {
		if (!Files.isRegularFile(file)) {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(256);
			SecretKey key = keyGen.generateKey();
			
			
			Path parentDir = file.getParent();
			if (parentDir != null && !Files.exists(parentDir)) {
				Files.createDirectories(parentDir);
			}
			
			Files.write(file, key.getEncoded());
			return key;
		} else {
			byte[] keyData = Files.readAllBytes(file);
			return new SecretKeySpec(keyData, "AES");
		}
	}
}