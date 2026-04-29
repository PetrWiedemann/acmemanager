package net.pdynet.acmemanager.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.AcmeRegistrationDao;
import net.pdynet.acmemanager.model.AcmeRegistration;
import net.pdynet.acmemanager.model.EncryptedString;
import net.pdynet.acmemanager.util.BlindTrustManager;

import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.connector.HttpConnector;
import org.shredzone.acme4j.connector.NetworkSettings;
import org.shredzone.acme4j.provider.AcmeProvider;
import org.shredzone.acme4j.provider.GenericAcmeProvider;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.time.OffsetDateTime;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class AcmeRegistrationEditorController {
	private static final Logger logger = LoggerFactory.getLogger(AcmeRegistrationEditorController.class);

	@FXML
	private TextField txtName, txtUrl, txtEmail, txtAccountUrl;
	@FXML
	private CheckBox chbTrustAll;
	@FXML
	private Button btnSave;
	@FXML
	private Label lblAccountUrl;
	
	private AcmeRegistration registration;
	private boolean saved = false;

	@FXML
	public void initialize() {
		lblAccountUrl.setVisible(false);
		lblAccountUrl.setManaged(false);
		txtAccountUrl.setVisible(false);
		txtAccountUrl.setManaged(false);
	}
	
	public void setRegistration(AcmeRegistration reg) {
		this.registration = reg;
		if (reg != null) {
			txtName.setText(reg.getName());
			txtUrl.setText(reg.getServerUrl());
			txtEmail.setText(reg.getEmail());
			chbTrustAll.setSelected(reg.isTrustAllCertificates());
			
			lblAccountUrl.setVisible(true);
			lblAccountUrl.setManaged(true);
			txtAccountUrl.setVisible(true);
			txtAccountUrl.setManaged(true);
			txtAccountUrl.setText(reg.getAccountUrl());
			
			txtUrl.setEditable(false);
			txtEmail.setEditable(false);
			
			btnSave.setText("Save");
			btnSave.setPrefWidth(80);
		}
	}

	@FXML
	private void handleSave() {
		String name = txtName.getText() != null ? txtName.getText().trim() : "";
		
		if (name.isBlank()) {
			showWarning("Friendly Name is required.");
			return;
		}		

		try {
			boolean trustAll = chbTrustAll.isSelected();
			
			if (registration == null) {
				// NOVÁ REGISTRACE PŘES ACME4J
				String url = txtUrl.getText() != null ? txtUrl.getText().trim() : "";
				String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
				
				if (url.isBlank()) {
					showWarning("Directory URL is required for a new registration.");
					return;
				}
				
				if (email.isBlank()) {
					showWarning("Contact Email is required for a new registration.");
					return;
				}
				
				AcmeProvider acmeProvider = null;
				
				if (trustAll) {
					TrustManager[] trustAllCerts = new TrustManager[] { new BlindTrustManager() };
					SSLContext sc = SSLContext.getInstance("TLS");
					sc.init(null, trustAllCerts, new SecureRandom());
					
					HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().sslContext(sc);
					final HttpClient client = httpClientBuilder.build();
					
					acmeProvider = new GenericAcmeProvider() {
						protected HttpConnector createHttpConnector(NetworkSettings settings, HttpClient httpClient) {
							return new HttpConnector(settings, client);
						}
					};
				}
				
				Session session = null;
				if (acmeProvider == null)
					session = new Session(URI.create(url));
				else
					session = new Session(URI.create(url), acmeProvider);
				
				KeyPair accountKeyPair = KeyPairUtils.createKeyPair();

				var account = new AccountBuilder().addContact("mailto:" + email)
						.agreeToTermsOfService().useKeyPair(accountKeyPair).create(session);

				StringWriter sw = new StringWriter();
				KeyPairUtils.writeKeyPair(accountKeyPair, sw);

				registration = new AcmeRegistration();
				registration.setName(name);
				registration.setServerUrl(url);
				registration.setAccountUrl(account.getLocation().toString());
				registration.setAccountKey(new EncryptedString(sw.toString()));
				registration.setEmail(email);
				registration.setTrustAllCertificates(trustAll);
				registration.setDateWrite(OffsetDateTime.now());
				registration.setDateEdit(OffsetDateTime.now());

				App.getJdbi().useExtension(AcmeRegistrationDao.class, dao -> dao.insert(registration));
			} else {
				// POUZE PŘEJMENOVÁNÍ
				registration.setName(name);
				registration.setTrustAllCertificates(trustAll);
				registration.setDateEdit(OffsetDateTime.now());
				App.getJdbi().useExtension(AcmeRegistrationDao.class, dao -> dao.update(registration));
			}
			saved = true;
			((Stage) txtName.getScene().getWindow()).close();
		} catch (Exception e) {
			logger.error("ACME operation failed", e);
			new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
		}
	}

	@FXML
	private void handleCancel() {
		((Stage) txtName.getScene().getWindow()).close();
	}

	public boolean isSaved() {
		return saved;
	}
	
	private void showWarning(String message) {
		new Alert(Alert.AlertType.WARNING, message).showAndWait();
	}	
}
