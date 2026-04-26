package net.pdynet.acmemanager.dao;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import net.pdynet.acmemanager.model.EncryptedString;
import net.pdynet.acmemanager.service.EncryptionService;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EncryptionMapper implements ColumnMapper<EncryptedString> {
	private final EncryptionService encryptionService;

	public EncryptionMapper(EncryptionService encryptionService) {
		this.encryptionService = encryptionService;
	}

	@Override
	public EncryptedString map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
		String base64 = r.getString(columnNumber);
		if (base64 == null)
			return null;

		try {
			return new EncryptedString(encryptionService.decrypt(base64));
		} catch (Exception e) {
			throw new SQLException("Error while decrypting column", e);
		}
	}
}
