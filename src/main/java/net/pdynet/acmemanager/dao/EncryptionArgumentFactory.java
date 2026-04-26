package net.pdynet.acmemanager.dao;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import net.pdynet.acmemanager.model.EncryptedString;
import net.pdynet.acmemanager.service.EncryptionService;

import java.sql.Types;

public class EncryptionArgumentFactory extends AbstractArgumentFactory<EncryptedString> {
	private final EncryptionService encryptionService;

	public EncryptionArgumentFactory(EncryptionService encryptionService) {
		super(Types.VARCHAR);
		this.encryptionService = encryptionService;
	}

	@Override
	protected Argument build(EncryptedString value, ConfigRegistry config) {
		return (position, statement, ctx) -> {
			try {
				if (value == null || value.value() == null) {
					statement.setString(position, null);
				} else {
					String encrypted = encryptionService.encrypt(value.value());
					statement.setString(position, encrypted);
				}
			} catch (Exception e) {
				throw new RuntimeException("Error while encrypting record", e);
			}
		};
	}
}
