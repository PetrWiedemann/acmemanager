package net.pdynet.acmemanager.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.DnsProviderDao;
import net.pdynet.acmemanager.model.DnsProvider;
import net.pdynet.acmemanager.model.DnsService;
import net.pdynet.acmemanager.model.EncryptedString;

import java.time.OffsetDateTime;

public class DnsProviderEditorController {

	@FXML
	private TextField txtName, txtApiKey, txtSecretKey;
	@FXML
	private PasswordField passApiKey, passSecretKey;
	@FXML
	private ComboBox<DnsService> cbService;
	@FXML
	private ToggleButton btnShowApiKey, btnShowSecretKey;

	private DnsProvider provider;
	private boolean saved = false;

	@FXML
	public void initialize() {
		// Synchronizace textu mezi PasswordField a TextField
		txtApiKey.textProperty().bindBidirectional(passApiKey.textProperty());
		txtSecretKey.textProperty().bindBidirectional(passSecretKey.textProperty());

		// Načtení služeb
		var services = App.getJdbi().withExtension(DnsProviderDao.class, dao -> dao.findAllServices());
		cbService.getItems().addAll(services);

		// Zjednodušený převodník pro zobrazení jména v ComboBoxu
		cbService.setConverter(new javafx.util.StringConverter<>() {
			@Override
			public String toString(DnsService obj) {
				return obj == null ? "" : obj.getName();
			}

			@Override
			public DnsService fromString(String s) {
				return null;
			}
		});
	}

	public void setProvider(DnsProvider provider) {
		this.provider = provider;
		if (provider != null) {
			txtName.setText(provider.getName());
			passApiKey.setText(provider.getApiKey().value());
			passSecretKey.setText(provider.getSecretKey() != null ? provider.getSecretKey().value() : "");

			// Vybrání správné služby v ComboBoxu na základě ID
			cbService.getItems().stream().filter(s -> s.getId() == provider.getDnsServiceId()).findFirst()
					.ifPresent(s -> cbService.getSelectionModel().select(s));
		}
	}

	@FXML
	private void toggleKeyVisibility(javafx.event.ActionEvent event) {
		ToggleButton btn = (ToggleButton) event.getSource();
		boolean show = btn.isSelected();
		if (btn == btnShowApiKey) {
			txtApiKey.setVisible(show);
			passApiKey.setVisible(!show);
		} else {
			txtSecretKey.setVisible(show);
			passSecretKey.setVisible(!show);
		}
	}

	@FXML
	private void handleSave() {
		if (txtName.getText().isBlank() || cbService.getValue() == null || passApiKey.getText().isBlank()) {
			new Alert(Alert.AlertType.WARNING, "Name, Service and API Key are required.").showAndWait();
			return;
		}

		if (provider == null)
			provider = new DnsProvider();

		provider.setName(txtName.getText().trim());
		provider.setDnsServiceId(cbService.getValue().getId()); // Získání ID napřímo
		provider.setApiKey(new EncryptedString(passApiKey.getText()));
		provider.setSecretKey(new EncryptedString(passSecretKey.getText()));
		provider.setDateEdit(OffsetDateTime.now());

		App.getJdbi().useExtension(DnsProviderDao.class, dao -> {
			if (provider.getId() == 0) {
				provider.setDateWrite(OffsetDateTime.now());
				dao.insert(provider);
			} else {
				dao.update(provider);
			}
		});

		saved = true;
		((Stage) txtName.getScene().getWindow()).close();
	}

	@FXML
	private void handleCancel() {
		((Stage) txtName.getScene().getWindow()).close();
	}

	public boolean isSaved() {
		return saved;
	}
}
