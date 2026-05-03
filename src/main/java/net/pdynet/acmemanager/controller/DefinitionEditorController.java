package net.pdynet.acmemanager.controller;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.AcmeRegistrationDao;
import net.pdynet.acmemanager.dao.CertificateDefinitionDao;
import net.pdynet.acmemanager.dao.DnsProviderDao;
import net.pdynet.acmemanager.model.AcmeRegistration;
import net.pdynet.acmemanager.model.CertificateDefinition;
import net.pdynet.acmemanager.model.DnsProvider;
import net.pdynet.acmemanager.model.EncryptedString;

public class DefinitionEditorController {
	private static final Logger logger = LoggerFactory.getLogger(DefinitionEditorController.class);
	
	@FXML private Label lblTitle;
	@FXML private TextField txtName;
	@FXML private TextField txtDomain;
	@FXML private TextArea txtSan;
	@FXML private ComboBox<AcmeRegistration> cbAcmeAccount;
	
	@FXML private ComboBox<String> cbChallengeType;
	@FXML private Label lblDnsProvider;
	@FXML private VBox boxPersistInfo;
	@FXML private TextField txtPersistRecordName;
	@FXML private TextField txtPersistRecordValue;
	@FXML private Label lblWebrootPath;
	@FXML private TextField txtWebrootPath;
	
	@FXML private ComboBox<DnsProvider> cbDnsProvider;
	@FXML private ComboBox<String> cbAlgorithm;
	@FXML private ComboBox<String> cbKeySpec;
	@FXML private CheckBox chbAutoRenew;
	@FXML private Spinner<Integer> spnRenewDays;

	@FXML private CheckBox chbAutoExport;
	@FXML private TextField txtExportPathCert;
	@FXML private TextField txtExportPathChain;
	@FXML private TextField txtExportPathKey;

	@FXML private CheckBox chbRunScript;
	@FXML private TextField txtScriptPath;

	@FXML private CheckBox chbAutoExportJks;
	@FXML private TextField txtExportPathJks;
	@FXML private PasswordField passJks;
	@FXML private TextField txtJksVisible;
	@FXML private ToggleButton btnShowJks;

	@FXML private CheckBox chbSendToWebhook;
	@FXML private TextField txtWebhookUrl;
	@FXML private TextArea txtWebhookHeaders;
	@FXML private PasswordField passWebhookPassword;
	@FXML private TextField txtWebhookPasswordVisible;
	@FXML private TextField txtWebhookPayloadId;
	@FXML private ToggleButton btnShowWebhookPass;
	@FXML private CheckBox chbWebhookTrustAll;

	private CertificateDefinition definition;
	private boolean saveClicked = false;

	@FXML
	public void initialize() {
		cbAcmeAccount.setConverter(new StringConverter<AcmeRegistration>() {
			@Override
			public String toString(AcmeRegistration object) {
				return object == null ? null : object.getName();
			}

			@Override
			public AcmeRegistration fromString(String string) {
				return null;
			}
		});

		cbDnsProvider.setConverter(new StringConverter<DnsProvider>() {
			@Override
			public String toString(DnsProvider object) {
				return object == null ? null : object.getName();
			}

			@Override
			public DnsProvider fromString(String string) {
				return null;
			}
		});

		List<AcmeRegistration> accounts = App.getJdbi().withExtension(AcmeRegistrationDao.class, dao -> dao.findAll());
		cbAcmeAccount.setItems(FXCollections.observableArrayList(accounts));

		List<DnsProvider> providers = App.getJdbi().withExtension(DnsProviderDao.class, dao -> dao.findAll());
		cbDnsProvider.setItems(FXCollections.observableArrayList(providers));

		cbAlgorithm.setItems(FXCollections.observableArrayList("RSA", "EC"));
		cbAlgorithm.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateKeySpecs(newVal));

		SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, 10);
		spnRenewDays.setValueFactory(valueFactory);

		txtExportPathCert.disableProperty().bind(chbAutoExport.selectedProperty().not());
		txtExportPathChain.disableProperty().bind(chbAutoExport.selectedProperty().not());
		txtExportPathKey.disableProperty().bind(chbAutoExport.selectedProperty().not());
		txtScriptPath.disableProperty().bind(chbRunScript.selectedProperty().not());
		txtExportPathJks.disableProperty().bind(chbAutoExportJks.selectedProperty().not());
		passJks.disableProperty().bind(chbAutoExportJks.selectedProperty().not());
		txtJksVisible.disableProperty().bind(chbAutoExportJks.selectedProperty().not());
		btnShowJks.disableProperty().bind(chbAutoExportJks.selectedProperty().not());

		txtWebhookUrl.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		txtWebhookHeaders.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		passWebhookPassword.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		txtWebhookPasswordVisible.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		btnShowWebhookPass.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		txtWebhookPayloadId.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		chbWebhookTrustAll.disableProperty().bind(chbSendToWebhook.selectedProperty().not());

		cbChallengeType.setItems(FXCollections.observableArrayList("DNS-01", "DNS-PERSIST-01", "HTTP-01"));
		cbChallengeType.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateChallengeUi());
		cbAcmeAccount.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateChallengeUi());
		txtDomain.textProperty().addListener((obs, oldV, newV) -> updateChallengeUi());
	}

	private void updateKeySpecs(String algorithm) {
		if ("RSA".equals(algorithm)) {
			cbKeySpec.setItems(FXCollections.observableArrayList("2048", "4096"));
		} else if ("EC".equals(algorithm)) {
			cbKeySpec.setItems(FXCollections.observableArrayList("secp256r1", "secp384r1"));
		} else {
			cbKeySpec.setItems(FXCollections.observableArrayList());
		}
		
		if (!cbKeySpec.getItems().isEmpty()) {
			cbKeySpec.getSelectionModel().selectFirst();
		}
	}

	public void setDefinition(CertificateDefinition definition) {
		this.definition = definition;
		
		if (definition != null) {
			lblTitle.setText("Edit Certificate Definition");
			txtName.setText(definition.getName());
			txtDomain.setText(definition.getDomainName());
			txtSan.setText(definition.getSubjectAltNames());

			cbAcmeAccount.getItems().stream()
					.filter(a -> a.getId() == definition.getAcmeRegistrationId())
					.findFirst()
					.ifPresent(cbAcmeAccount.getSelectionModel()::select);

			cbDnsProvider.getItems().stream()
					.filter(p -> definition.getDnsProviderId() != null && p.getId() == definition.getDnsProviderId())
					.findFirst()
					.ifPresent(cbDnsProvider.getSelectionModel()::select);
			
			String challengeType = definition.getChallengeType();
			if (StringUtils.isNotBlank(challengeType)) {
				cbChallengeType.setValue(challengeType);
			} else if (definition.getDnsProviderId() == null && definition.getId() != 0) {
				cbChallengeType.setValue("DNS-PERSIST-01");
			} else {
				cbChallengeType.setValue("DNS-01");
			}

			cbAlgorithm.getSelectionModel().select(definition.getKeyAlgorithm());
			cbKeySpec.getSelectionModel().select(definition.getKeySizeOrCurve());

			chbAutoRenew.setSelected(definition.isAutoRenew());
			spnRenewDays.getValueFactory().setValue(definition.getDaysBeforeExpiryToRenew());

			chbAutoExport.setSelected(definition.isAutoExport());
			txtExportPathCert.setText(definition.getExportPathCert());
			txtExportPathChain.setText(definition.getExportPathChain());
			txtExportPathKey.setText(definition.getExportPathKey());
			txtWebrootPath.setText(definition.getWebrootPath());

			chbRunScript.setSelected(definition.isRunScript());
			txtScriptPath.setText(definition.getScriptPath());

			chbAutoExportJks.setSelected(definition.isAutoExportJks());
			txtExportPathJks.setText(definition.getExportPathJks());
			passJks.setText(definition.getJksPassword() != null ? definition.getJksPassword().value() : "");

			chbSendToWebhook.setSelected(definition.isSendToWebhook());
			txtWebhookUrl.setText(definition.getWebhookUrl());
			txtWebhookHeaders.setText(definition.getWebhookHeaders());
			txtWebhookPayloadId.setText(definition.getWebhookPayloadId());
			chbWebhookTrustAll.setSelected(definition.isWebhookTrustAll());
			passWebhookPassword.setText(definition.getWebhookPassword() != null ? definition.getWebhookPassword().value() : "");
			
			updateChallengeUi();
		} else {
			this.definition = new CertificateDefinition();
			lblTitle.setText("New Certificate Definition");
			cbAlgorithm.getSelectionModel().select("RSA");
			cbKeySpec.getSelectionModel().select("4096");
			cbChallengeType.setValue("DNS-01");
		}
	}

	@FXML
	private void handleSave() {
		if (!validateInput()) {
			return;
		}

		definition.setName(txtName.getText().trim());
		definition.setDomainName(txtDomain.getText().trim());
		definition.setSubjectAltNames(txtSan.getText().trim());
		definition.setAcmeRegistrationId(cbAcmeAccount.getValue().getId());
		
		Integer providerId = switch (cbChallengeType.getValue()) {
			case "DNS-01" -> cbDnsProvider.getValue().getId();
			case "DNS-PERSIST-01", "HTTP-01" -> null; 
			case null, default -> null;
		};
		
		definition.setDnsProviderId(providerId);
		
		definition.setChallengeType(cbChallengeType.getValue());
		definition.setKeyAlgorithm(cbAlgorithm.getValue());
		definition.setKeySizeOrCurve(cbKeySpec.getValue());
		definition.setAutoRenew(chbAutoRenew.isSelected());
		definition.setDaysBeforeExpiryToRenew(spnRenewDays.getValue());

		definition.setAutoExport(chbAutoExport.isSelected());
		definition.setExportPathCert(StringUtils.trimToEmpty(txtExportPathCert.getText()));
		definition.setExportPathChain(StringUtils.trimToEmpty(txtExportPathChain.getText()));
		definition.setExportPathKey(StringUtils.trimToEmpty(txtExportPathKey.getText()));
		definition.setWebrootPath(StringUtils.trimToEmpty(txtWebrootPath.getText()));

		definition.setRunScript(chbRunScript.isSelected());
		definition.setScriptPath(StringUtils.trimToEmpty(txtScriptPath.getText()));

		definition.setAutoExportJks(chbAutoExportJks.isSelected());
		definition.setExportPathJks(StringUtils.trimToEmpty(txtExportPathJks.getText()));
		definition.setJksPassword(new EncryptedString(passJks.getText()));

		definition.setSendToWebhook(chbSendToWebhook.isSelected());
		definition.setWebhookUrl(StringUtils.trimToEmpty(txtWebhookUrl.getText()));
		definition.setWebhookHeaders(StringUtils.trimToEmpty(txtWebhookHeaders.getText()));
		definition.setWebhookPayloadId(StringUtils.trimToEmpty(txtWebhookPayloadId.getText()));
		definition.setWebhookTrustAll(chbWebhookTrustAll.isSelected());
		definition.setWebhookPassword(new EncryptedString(passWebhookPassword.getText()));

		definition.setDateEdit(OffsetDateTime.now());

		try {
			CertificateDefinitionDao dao = App.getJdbi().onDemand(CertificateDefinitionDao.class);
			if (definition.getId() == 0) {
				definition.setDateWrite(OffsetDateTime.now());
				dao.insert(definition);
			} else {
				dao.update(definition);
			}

			saveClicked = true;
			((Stage) txtName.getScene().getWindow()).close();
		} catch (Exception e) {
			logger.error("Failed to save definition", e);
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setContentText("Failed to save definition: " + e.getMessage());
			alert.showAndWait();
		}
	}

	@FXML
	private void handleCancel() {
		((Stage) txtName.getScene().getWindow()).close();
	}

	@FXML
	private void togglePasswordVisibility(ActionEvent event) {
		ToggleButton btn = (ToggleButton) event.getSource();
		if (btn == btnShowJks) {
			syncPassFields(passJks, txtJksVisible, btn.isSelected());
		} else if (btn == btnShowWebhookPass) {
			syncPassFields(passWebhookPassword, txtWebhookPasswordVisible, btn.isSelected());
		}
	}

	private void syncPassFields(PasswordField pass, TextField txt, boolean show) {
		if (show) {
			txt.setText(pass.getText());
			txt.setVisible(true);
			pass.setVisible(false);
		} else {
			pass.setText(txt.getText());
			pass.setVisible(true);
			txt.setVisible(false);
		}
	}

	private boolean validateInput() {
		if (StringUtils.isBlank(txtName.getText()) || StringUtils.isBlank(txtDomain.getText()) || cbAcmeAccount.getValue() == null) {
			showWarning("Please fill in the basic required fields (Friendly Name, Domain Name, and ACME Account).");
			return false;
		}
		
		String domain = StringUtils.trimToEmpty(txtDomain.getText());
		String sanText = StringUtils.trimToEmpty(txtSan.getText());
		boolean isWildcard = domain.startsWith("*.");
		
		if (!isWildcard && !sanText.isEmpty()) {
			isWildcard = Arrays.stream(sanText.split(","))
					.map(String::trim)
					.anyMatch(s -> s.startsWith("*."));
		}
		
		switch (cbChallengeType.getValue()) {
			case "DNS-01" -> {
				if (cbDnsProvider.getValue() == null) {
					showWarning("For the DNS-01 challenge type, you must select a DNS Provider for automation.");
					return false;
				}
			}
			case "HTTP-01" -> {
				if (StringUtils.isBlank(txtWebrootPath.getText())) {
					showWarning("For the HTTP-01 challenge type, you must specify the Webroot Path.");
					return false;
				}
				
				if (isWildcard) {
					showWarning("Wildcard certificates (e.g., *.example.com) strictly require DNS validation. HTTP-01 cannot be used.");
					return false;
				}
			}
			case "DNS-PERSIST-01" -> {
				// NOOP.
			}
			case null, default -> {
				showWarning("Please select a valid Challenge Type.");
				return false;
			}
		}

		return true;
	}
	
	private void showWarning(String message) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("Validation Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	public boolean isSaveClicked() {
		return saveClicked;
	}
	
	private void updateChallengeUi() {
		switch (cbChallengeType.getValue()) {
			case "DNS-01" -> {
				showNode(lblDnsProvider, true);
				showNode(cbDnsProvider, true);
				showNode(boxPersistInfo, false);
				showNode(lblWebrootPath, false);
				showNode(txtWebrootPath, false);
			}
			case "HTTP-01" -> {
				showNode(lblDnsProvider, false);
				showNode(cbDnsProvider, false);
				showNode(boxPersistInfo, false);
				showNode(lblWebrootPath, true);
				showNode(txtWebrootPath, true);
			}
			case "DNS-PERSIST-01" -> {
				showNode(lblDnsProvider, false);
				showNode(cbDnsProvider, false);
				showNode(boxPersistInfo, true);
				showNode(lblWebrootPath, false);
				showNode(txtWebrootPath, false);
				
				String domain = StringUtils.isNotBlank(txtDomain.getText()) ? txtDomain.getText().trim() : "<your-FQDN>";
				boolean isWildcard = false;
				
				if (domain.startsWith("*.")) {
					isWildcard = true;
					domain = Strings.CS.removeStart(domain, "*.");
				}
				
				txtPersistRecordName.setText("_validation-persist." + domain);
				
				AcmeRegistration account = cbAcmeAccount.getValue();
				StringBuilder sb = new StringBuilder();
				
				if (account != null) {
					sb.append("letsencrypt.org; accounturi=").append(account.getAccountUrl());
				} else {
					sb.append("letsencrypt.org; accounturi=<select-acme-account>");
				}
				
				if (isWildcard)
					sb.append("; policy=wildcard");
				
				txtPersistRecordValue.setText(sb.toString());
			}
			case null, default -> {
			}
		}
	}
	
	private void showNode(javafx.scene.Node node, boolean show) {
		node.setVisible(show);
		node.setManaged(show);
	}
}
