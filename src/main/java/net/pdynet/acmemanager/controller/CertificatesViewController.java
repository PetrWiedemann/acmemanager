package net.pdynet.acmemanager.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.CertificateDefinitionDao;
import net.pdynet.acmemanager.model.CertificateDefinition;
import net.pdynet.acmemanager.model.CertificateDefinitionView;
import net.pdynet.acmemanager.service.CertificateIssuanceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CertificatesViewController {
	private static final Logger logger = LoggerFactory.getLogger(CertificatesViewController.class);
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@FXML
	private TableView<CertificateDefinitionView> certTable;
	@FXML
	private TableColumn<CertificateDefinitionView, String> colName;
	@FXML
	private TableColumn<CertificateDefinitionView, String> colDomain;
	@FXML
	private TableColumn<CertificateDefinitionView, String> colAlgorithm;
	@FXML
	private TableColumn<CertificateDefinitionView, Boolean> colAutoRenew;
	@FXML
	private TableColumn<CertificateDefinitionView, String> colExpiry;
	@FXML
	private Button btnEdit;
	@FXML
	private Button btnDelete;
	@FXML
	private Button btnHistory;
	@FXML
	private Button btnFetch;

	@FXML
	public void initialize() {
		colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));

		colDomain.setCellValueFactory(
				data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDomainName()));

		colAlgorithm.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
				data.getValue().getKeyAlgorithm() + " (" + data.getValue().getKeySizeOrCurve() + ")"));

		colAutoRenew.setCellValueFactory(
				data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().isAutoRenew()));

		// Formátování zobrazení pro Auto Renew (Checkbox look)
		colAutoRenew.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(Boolean item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null)
					setText(null);
				else
					setText(item ? "Yes" : "No");
			}
		});

		colExpiry.setCellValueFactory(data -> {
			var date = data.getValue().getExpiryDate();
			return new javafx.beans.property.SimpleStringProperty(
					date != null ? date.format(dateFormatter) : "Not issued yet");
		});

		// Aktivace/deaktivace tlačítek podle výběru v tabulce
		certTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			boolean isSelected = (newVal != null);
			btnEdit.setDisable(!isSelected);
			btnDelete.setDisable(!isSelected);
			btnHistory.setDisable(!isSelected);
			btnFetch.setDisable(!isSelected);
		});

		// Double-click pro editaci
		certTable.setRowFactory(tv -> {
			TableRow<CertificateDefinitionView> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!row.isEmpty())) {
					handleEdit();
				}
			});
			return row;
		});

		refreshData();
	}

	private void refreshData() {
		List<CertificateDefinitionView> data = App.getJdbi().withExtension(CertificateDefinitionDao.class,
				dao -> dao.findAllWithExpiry());
		certTable.setItems(FXCollections.observableArrayList(data));
	}

	@FXML
	private void handleNew() {
		openEditor(null);
	}

	@FXML
	private void handleEdit() {
		CertificateDefinitionView selectedView = certTable.getSelectionModel().getSelectedItem();
		if (selectedView != null) {
			CertificateDefinition def = App.getJdbi().withExtension(CertificateDefinitionDao.class,
					dao -> dao.findById(selectedView.getId()));
			openEditor(def);
		}
	}

	@FXML
	private void handleDelete() {
		CertificateDefinitionView selectedView = certTable.getSelectionModel().getSelectedItem();
		if (selectedView == null)
			return;

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete definition '" + selectedView.getName() + "'?",
				ButtonType.YES, ButtonType.NO);
		confirm.showAndWait().ifPresent(response -> {
			if (response == ButtonType.YES) {
				try {
					App.getJdbi().useExtension(CertificateDefinitionDao.class,
							dao -> dao.deleteById(selectedView.getId()));
					refreshData();
				} catch (Exception e) {
					logger.error("Failed to delete definition", e);
					new Alert(Alert.AlertType.ERROR,
							"Cannot delete definition. It may have issued certificates in history.").showAndWait();
				}
			}
		});
	}

	@FXML
	private void handleHistory() {
		CertificateDefinitionView selectedView = certTable.getSelectionModel().getSelectedItem();
		if (selectedView == null)
			return;

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CertificateHistory.fxml"));
			Parent root = loader.load();

			CertificateHistoryController controller = loader.getController();
			controller.initData(selectedView.getId());

			Stage stage = new Stage();
			stage.setTitle("History: " + selectedView.getName());
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setScene(new Scene(root));
			stage.showAndWait();
		} catch (IOException e) {
			logger.error("Failed to open history", e);
		}
	}

	private void openEditor(CertificateDefinition def) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DefinitionEditor.fxml"));
			Parent root = loader.load();

			DefinitionEditorController controller = loader.getController();
			controller.setDefinition(def);

			Stage stage = new Stage();
			stage.setTitle(def == null ? "New Certificate" : "Edit Certificate");
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setScene(new Scene(root));
			stage.showAndWait();

			if (controller.isSaveClicked())
				refreshData();
		} catch (IOException e) {
			logger.error("Error opening editor", e);
		}
	}

	@FXML
	private void handleFetch() {
		CertificateDefinitionView selectedView = certTable.getSelectionModel().getSelectedItem();
		if (selectedView == null)
			return;

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Confirm Certificate Fetch");
		confirm.setHeaderText("Issue new certificate for: " + selectedView.getName());
		confirm.setContentText("Do you really want to fetch a new certificate for domain " + selectedView.getDomainName() + "?\n\nThis will initiate communication with the ACME server and modify your DNS records.");
		confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
		
		if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
			return;
        
		// Vypneme tlačítka a ukážeme kurzor načítání, aby uživatel neklikal znovu
		btnFetch.setDisable(true);
		btnFetch.setText("Fetching...");
		certTable.getScene().setCursor(javafx.scene.Cursor.WAIT);

		// Vytvoříme úkol na pozadí (Task)
		javafx.concurrent.Task<Void> fetchTask = new javafx.concurrent.Task<>() {
			@Override
			protected Void call() throws Exception {
				CertificateIssuanceService service = new CertificateIssuanceService();
				service.fetchCertificateForDefinition(selectedView.getId());
				return null;
			}
		};

		// Co se stane, když proces úspěšně skončí
		fetchTask.setOnSucceeded(e -> {
			resetUiState();
			refreshData();
			new Alert(Alert.AlertType.INFORMATION, "Certificate successfully issued and saved!").showAndWait();
		});

		// Co se stane při chybě
		fetchTask.setOnFailed(e -> {
			Throwable error = fetchTask.getException();
			resetUiState();
			logger.error("Failed to fetch certificate", error);
			new Alert(Alert.AlertType.ERROR, "Failed to fetch certificate: " + error.getMessage()).showAndWait();
		});

		// Spuštění vlákna
		Thread thread = new Thread(fetchTask);
		thread.setDaemon(true); // Ukončí se při zavření aplikace
		thread.start();
	}

	private void resetUiState() {
		btnFetch.setDisable(false);
		btnFetch.setText("Fetch Certificate");
		certTable.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
	}
}
