package net.pdynet.acmemanager.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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

	@FXML
	private Label lblTitle;
	@FXML
	private TextField txtName, txtDomain;
	@FXML
	private TextArea txtSan, txtWebhookHeaders;
	@FXML
	private ComboBox<AcmeRegistration> cbAcme;
	@FXML
	private ComboBox<DnsProvider> cbDns;
	@FXML
	private ComboBox<String> cbAlgorithm, cbKeySpec;
	@FXML
	private CheckBox chbAutoRenew;
	@FXML
	private Spinner<Integer> spnRenewDays;
	@FXML
	private CheckBox chbAutoExport, chbRunScript, chbSendToWebhook, chbWebhookTrustAll;
	@FXML
	private TextField txtExportPathCert, txtExportPathChain, txtExportPathKey, txtScriptPath, txtWebhookUrl, txtWebhookPayloadId;

	@FXML
	private CheckBox chbExportJks;
	@FXML
	private TextField txtExportPathJks, txtJksVisible, txtWebhookPasswordVisible;
	@FXML
	private PasswordField passJks, passWebhookPassword;
	@FXML
	private ToggleButton btnShowJks, btnShowWebhookPass;	
	
	private CertificateDefinition definition;
	private boolean saveClicked = false;

	@FXML
	public void initialize() {
		// Synchronizace textu mezi PasswordField a TextField
		txtJksVisible.textProperty().bindBidirectional(passJks.textProperty());
		txtWebhookPasswordVisible.textProperty().bindBidirectional(passWebhookPassword.textProperty());
		
		// Inicializace Spinneru (1-90 dní, default 10)
		spnRenewDays.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, 10));

		// Algoritmy
		cbAlgorithm.setItems(FXCollections.observableArrayList("RSA", "ECDSA"));
		cbAlgorithm.getSelectionModel().selectedItemProperty()
				.addListener((obs, oldVal, newVal) -> updateKeySpecs(newVal));
		cbAlgorithm.getSelectionModel().select("RSA");

		txtExportPathCert.disableProperty().bind(chbAutoExport.selectedProperty().not());
		txtExportPathChain.disableProperty().bind(chbAutoExport.selectedProperty().not());
		txtExportPathKey.disableProperty().bind(chbAutoExport.selectedProperty().not());
		
		txtExportPathJks.disableProperty().bind(chbExportJks.selectedProperty().not());
		txtJksVisible.disableProperty().bind(chbExportJks.selectedProperty().not());
		passJks.disableProperty().bind(chbExportJks.selectedProperty().not());
		btnShowJks.disableProperty().bind(chbExportJks.selectedProperty().not());
		
		txtWebhookUrl.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		txtWebhookHeaders.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		passWebhookPassword.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		txtWebhookPasswordVisible.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		btnShowWebhookPass.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		
		txtWebhookPayloadId.disableProperty().bind(chbSendToWebhook.selectedProperty().not());
		
		txtScriptPath.disableProperty().bind(chbRunScript.selectedProperty().not());

		// Načtení relací z DB
		loadComboBoxData();
	}

	private void loadComboBoxData() {
		List<AcmeRegistration> registrations = App.getJdbi().withExtension(AcmeRegistrationDao.class, dao -> dao.findAll());
		cbAcme.setItems(FXCollections.observableArrayList(registrations));
		cbAcme.setConverter(new StringConverter<>() {
			@Override
			public String toString(AcmeRegistration object) {
				return object == null ? "" : object.getName();
			}

			@Override
			public AcmeRegistration fromString(String string) {
				return null;
			}
		});

		List<DnsProvider> providers = App.getJdbi().withExtension(DnsProviderDao.class, dao -> dao.findAll());
		cbDns.setItems(FXCollections.observableArrayList(providers));
		cbDns.setConverter(new StringConverter<>() {
			@Override
			public String toString(DnsProvider object) {
				return object == null ? "" : object.getName();
			}

			@Override
			public DnsProvider fromString(String string) {
				return null;
			}
		});
	}

	private void updateKeySpecs(String algorithm) {
		if ("RSA".equals(algorithm)) {
			cbKeySpec.setItems(FXCollections.observableArrayList("2048", "4096"));
			cbKeySpec.getSelectionModel().select("2048");
		} else {
			cbKeySpec.setItems(FXCollections.observableArrayList("secp384r1", "secp256r1"));
			cbKeySpec.getSelectionModel().select("secp384r1");
		}
	}

	public void setDefinition(CertificateDefinition def) {
		this.definition = def;
		if (def != null) {
			lblTitle.setText("Edit Certificate Definition");
			txtName.setText(def.getName());
			txtDomain.setText(def.getDomainName());
			txtSan.setText(def.getSubjectAltNames());
			chbAutoRenew.setSelected(def.isAutoRenew());
			spnRenewDays.getValueFactory().setValue(def.getDaysBeforeExpiryToRenew());
			cbAlgorithm.getSelectionModel().select(def.getKeyAlgorithm());
			cbKeySpec.getSelectionModel().select(def.getKeySizeOrCurve());

			// Vyhledání relací v listu
			cbAcme.getItems().stream().filter(a -> a.getId() == def.getAcmeRegistrationId()).findFirst()
					.ifPresent(a -> cbAcme.getSelectionModel().select(a));
			cbDns.getItems().stream().filter(d -> d.getId() == def.getDnsProviderId()).findFirst()
					.ifPresent(d -> cbDns.getSelectionModel().select(d));
			
			chbAutoExport.setSelected(def.isAutoExport());
			txtExportPathCert.setText(def.getExportPathCert());
			txtExportPathChain.setText(def.getExportPathChain());
			txtExportPathKey.setText(def.getExportPathKey());
			
			chbExportJks.setSelected(def.isAutoExportJks());
			txtExportPathJks.setText(def.getExportPathJks());
			passJks.setText(def.getJksPassword() != null ? def.getJksPassword().value() : "");
			
			chbRunScript.setSelected(def.isRunScript());
			txtScriptPath.setText(def.getScriptPath());
			
			chbSendToWebhook.setSelected(def.isSendToWebhook());
			chbWebhookTrustAll.setSelected(def.isWebhookTrustAll());
			txtWebhookUrl.setText(def.getWebhookUrl());
			txtWebhookHeaders.setText(def.getWebhookHeaders());
			txtWebhookPayloadId.setText(def.getWebhookPayloadId());
			passWebhookPassword.setText(def.getWebhookPassword() != null ? def.getWebhookPassword().value() : "");
		} else {
			this.definition = new CertificateDefinition();
		}
	}

	@FXML
	private void handleSave() {
		if (validateInput()) {
			definition.setName(txtName.getText());
			definition.setDomainName(txtDomain.getText());
			definition.setSubjectAltNames(txtSan.getText());
			definition.setAcmeRegistrationId(cbAcme.getValue().getId());
			definition.setDnsProviderId(cbDns.getValue().getId());
			definition.setKeyAlgorithm(cbAlgorithm.getValue());
			definition.setKeySizeOrCurve(cbKeySpec.getValue());
			definition.setAutoRenew(chbAutoRenew.isSelected());
			definition.setDaysBeforeExpiryToRenew(spnRenewDays.getValue());
			definition.setDateEdit(OffsetDateTime.now());
			definition.setAutoExport(chbAutoExport.isSelected());
			definition.setExportPathCert(StringUtils.trimToEmpty(txtExportPathCert.getText()));
			definition.setExportPathChain(StringUtils.trimToEmpty(txtExportPathChain.getText()));
			definition.setExportPathKey(StringUtils.trimToEmpty(txtExportPathKey.getText()));
			definition.setRunScript(chbRunScript.isSelected());
			definition.setScriptPath(StringUtils.trimToEmpty(txtScriptPath.getText()));
			definition.setSendToWebhook(chbSendToWebhook.isSelected());
			definition.setWebhookTrustAll(chbWebhookTrustAll.isSelected());
			definition.setWebhookUrl(StringUtils.trimToEmpty(txtWebhookUrl.getText()));
			definition.setWebhookHeaders(txtWebhookHeaders.getText());
			definition.setWebhookPayloadId(StringUtils.trimToEmpty(txtWebhookPayloadId.getText()));
			definition.setWebhookPassword(new EncryptedString(passWebhookPassword.getText()));
			definition.setAutoExportJks(chbExportJks.isSelected());
			definition.setExportPathJks(txtExportPathJks.getText());
			definition.setJksPassword(new EncryptedString(passJks.getText()));
            
			CertificateDefinitionDao dao = App.getJdbi().onDemand(CertificateDefinitionDao.class);
			if (definition.getId() == 0) {
				definition.setDateWrite(OffsetDateTime.now());
				dao.insert(definition);
			} else {
				dao.update(definition);
			}

			saveClicked = true;
			((Stage) txtName.getScene().getWindow()).close();
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
		if (txtName.getText().isBlank() || txtDomain.getText().isBlank() || cbAcme.getValue() == null
				|| cbDns.getValue() == null) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setContentText("Please fill in all required fields (Name, Domain, ACME, DNS).");
			alert.showAndWait();
			return false;
		}
		return true;
	}

	public boolean isSaveClicked() {
		return saveClicked;
	}
}
