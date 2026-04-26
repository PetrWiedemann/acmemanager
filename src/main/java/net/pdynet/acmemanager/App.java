package net.pdynet.acmemanager;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.sql.SQLException;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.pdynet.acmemanager.dao.ConfigDao;
import net.pdynet.acmemanager.dao.EncryptionArgumentFactory;
import net.pdynet.acmemanager.dao.EncryptionMapper;
import net.pdynet.acmemanager.model.ConfigRecord;
import net.pdynet.acmemanager.service.DatabaseMigrator;
import net.pdynet.acmemanager.service.EncryptionService;
import net.pdynet.acmemanager.service.KeyManager;
import net.pdynet.acmemanager.service.RenewalService;
import net.pdynet.acmemanager.service.Threading;
import net.pdynet.acmemanager.service.bot.Activity;
import net.pdynet.acmemanager.service.bot.Message;
import net.pdynet.acmemanager.service.bot.ReportSubmission;

public class App extends Application {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private static Jdbi jdbi;
	private static HikariDataSource dataSource;
	private static EncryptionService encryptionService;

	public static Jdbi getJdbi() {
		return jdbi;
	}

	public static EncryptionService getEncryptionService() {
		return encryptionService;
	}

	@Override
	public void init() throws Exception {
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
		Parent root = loader.load();

		primaryStage.setTitle("ACME Certificate Manager");
		primaryStage.setScene(new Scene(root, 1100, 700));
		primaryStage.show();
	}

	private static void openDatabase(String dbPath) throws SQLException {
		Path path = Paths.get(dbPath).toAbsolutePath().normalize();
		String normalizedPath = path.toString().replace("\\", "/");
		String jdbcUrl = "jdbc:h2:file:" + normalizedPath;
		logger.debug("Connecting to database using URL: {}", jdbcUrl);
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(jdbcUrl);
		config.setDriverClassName("org.h2.Driver");
		config.setMaximumPoolSize(10);
		config.setMinimumIdle(0);
		config.setPoolName("AcmePool");
		
		dataSource = new HikariDataSource(config);
		
		jdbi = Jdbi.create(dataSource);
		jdbi.installPlugin(new SqlObjectPlugin());
	}
	
	private static void closeDatabase() {
		if (dataSource != null) {
			dataSource.close();
		}
	}

	public static void main(String[] args) {
		try {
			AppArgs appArgs = new AppArgs();
			JCommander jc = JCommander.newBuilder()
					.addObject(appArgs)
					.programName("java -jar acmemanager.jar")
					.build();

			try {
				jc.parse(args);
			} catch (ParameterException e) {
				logger.error("Error parsing command line parameters.", e);
				System.err.println("Error parsing command line parameters: " + e.getMessage());
				jc.usage();
				System.exit(1);
			}
			
			if (appArgs.isHelp()) {
				jc.usage();
				System.exit(0);
			}
			
			initCoreServices(appArgs.getDbPath(), appArgs.getMasterKeyPath());
			
			DatabaseMigrator databaseMigrator = new DatabaseMigrator(jdbi);
			databaseMigrator.migrate();
			
			if (appArgs.isRenewAll()) {
				logger.debug("Application started in headless mode (--renew-all).");
				try {
					RenewalService renewalService = new RenewalService();
					renewalService.runAutomatedRenewal();
					System.exit(0);
				} catch (Exception e) {
					logger.error("Critical error during headless renewal", e);
					System.exit(1);
				}
			} else {
				logger.debug("Starting application in GUI mode.");
				launch(args);
			}
		} catch (Exception e) {
			logger.error("Main app error.", e);
		} finally {
			closeDatabase();
			Threading.close();
		}
	}
	
	private static void initCoreServices(String dbPath, String keyPath) {
		try {
			logger.info("Initializing database connection at: {}", dbPath);
			logger.info("Using master key from: {}", keyPath);
			
			Security.addProvider(new BouncyCastleProvider());
			
			// Inicializace DB a šifrování
			Path keyFile = Paths.get(keyPath);
			KeyManager keyManager = new KeyManager();
			SecretKey masterKey = keyManager.getOrGenerateKey(keyFile);
			encryptionService = new EncryptionService(masterKey);

			openDatabase(dbPath);
			jdbi.registerColumnMapper(new EncryptionMapper(encryptionService));
			jdbi.registerArgument(new EncryptionArgumentFactory(encryptionService));
			
		} catch (Exception e) {
			logger.error("Failed to initialize core services", e);
			System.exit(1);
		}
	}
	
	public static void sendBotReport(String message) {
		try {
			if (StringUtils.isNotBlank(message)) {
				ConfigRecord configReportUri = getJdbi().withExtension(ConfigDao.class, dao -> dao.findById("report_uri"));
				ConfigRecord configReportRecipient = getJdbi().withExtension(ConfigDao.class, dao -> dao.findById("report_recipient"));
				ConfigRecord configReportKey = getJdbi().withExtension(ConfigDao.class, dao -> dao.findById("report_key"));
				
				if (configReportUri != null
						&& configReportRecipient != null
						&& configReportKey != null
						&& StringUtils.isNotBlank(configReportUri.getTextValue())
						&& StringUtils.isNotBlank(configReportRecipient.getTextValue())
						&& StringUtils.isNotBlank(configReportKey.getTextValue())) {
					Activity activity = new Activity();
					activity.setType("message");
					activity.setText(message);
					
					Message botMessage = new Message();
					botMessage.setS4wConnectId(configReportRecipient.getTextValue());
					botMessage.setActivity(activity);
					
					ReportSubmission reportSubmission = new ReportSubmission(HttpClient.newHttpClient(), new URI(configReportUri.getTextValue()), configReportKey.getTextValue());
					reportSubmission.sendMessage(botMessage);
				}
			}
		} catch (Exception e) {
			logger.error("Failed to send report", e);
		}
	}
}
