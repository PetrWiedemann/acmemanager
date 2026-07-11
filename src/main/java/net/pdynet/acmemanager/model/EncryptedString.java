package net.pdynet.acmemanager.model;

/**
 * Holds a secret in its decrypted form while in memory. Encryption and decryption happen
 * at the JDBI boundary (see EncryptionArgumentFactory and EncryptionMapper).
 */
public record EncryptedString(String value) {

	/**
	 * Never render the secret. Model classes embed this type in their own toString(), which
	 * is one accidental logger call away from writing account keys and passwords to app.log.
	 */
	@Override
	public String toString() {
		return value == null ? "null" : "***";
	}
}
