package net.pdynet.acmemanager;

import com.beust.jcommander.Parameter;

public class AppArgs {

	@Parameter(names = { "-d", "--db-path" },
			description = "Path to the H2 database file (without the .mv.db extension). If not specified, defaults to 'db/acmemanager' in the current working directory.")
	private String dbPath = "db/acmemanager";

	@Parameter(names = { "-k", "--key-path" },
			description = "Path to the master encryption key file. If not specified, defaults to 'db/master.key' in the current working directory.")
	private String masterKeyPath = "db/master.key";

	@Parameter(names = { "-r", "--renew-all" },
			description = "Start the application in headless mode (no GUI) and trigger automatic renewal of all eligible certificates. The application will automatically exit after the process is finished.")
	private boolean renewAll = false;

	@Parameter(names = { "-h", "--help" },
			description = "Display this help message, showing all available commands and options, then exit.", help = true)
	private boolean help = false;

	public String getDbPath() {
		return dbPath;
	}

	public String getMasterKeyPath() {
		return masterKeyPath;
	}

	public boolean isRenewAll() {
		return renewAll;
	}

	public boolean isHelp() {
		return help;
	}
}