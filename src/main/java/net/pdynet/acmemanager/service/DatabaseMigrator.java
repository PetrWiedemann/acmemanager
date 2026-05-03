package net.pdynet.acmemanager.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Script;

import net.pdynet.acmemanager.dao.ConfigDao;

public class DatabaseMigrator {
	private final Jdbi jdbi;
	private static final int CURRENT_APP_SCHEMA_VERSION = 3;
	
	public DatabaseMigrator(Jdbi jdbi) {
		this.jdbi = jdbi;
	}
	
	public void migrate() throws IOException {
		jdbi.useHandle(handle -> {
			handle.execute("CREATE TABLE IF NOT EXISTS config (id VARCHAR(255) NOT NULL PRIMARY KEY, int_val INTEGER, text_val TEXT)");
			
			Integer schemaVersion = handle.createQuery("SELECT int_val FROM config WHERE id = 'schema_version'")
					.mapTo(Integer.class)
					.findFirst()
					.orElse(0);
			
			if (schemaVersion < CURRENT_APP_SCHEMA_VERSION) {
				for (int i = schemaVersion + 1; i <= CURRENT_APP_SCHEMA_VERSION; i++) {
					String path = "/sql/schema_v" + i + ".sql";
					runScript(handle, path);
				}
				
				handle.attach(ConfigDao.class).updateConfig("schema_version", CURRENT_APP_SCHEMA_VERSION, StringUtils.EMPTY);
			}
			
		});
	}
	
	private void runScript(Handle handle, String path) throws IOException {
		try (var is = getClass().getResourceAsStream(path)) {
			String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
					.lines()
					.collect(Collectors.joining("\n"));
			Script script = handle.createScript(sql);
			script.execute();
		}
	}
}
