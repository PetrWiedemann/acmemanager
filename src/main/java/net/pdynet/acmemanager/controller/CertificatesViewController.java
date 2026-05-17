package net.pdynet.acmemanager.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CertificatesViewController {
	private static final Logger logger = LoggerFactory.getLogger(CertificatesViewController.class);
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	
	private AtomicBoolean activeCancelToken;

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

		// Formatting display for Auto Renew (Checkbox look)
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

		// Enable/disable buttons based on table selection
		certTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			boolean isSelected = (newVal != null);
			btnEdit.setDisable(!isSelected);
			btnDelete.setDisable(!isSelected);
			btnHistory.setDisable(!isSelected);
			btnFetch.setDisable(!isSelected);
		});

		// Double-click to edit
		certTable.setRowFactory(tv -> {
			TableRow<CertificateDefinitionView> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!row.isEmpty())) {
					handleEdit();
				}
			});
			return row;
		});

		certTable.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				if (certTable.getSelectionModel().getSelectedItem() != null) {
					handleEdit();
					event.consume();
				}
			}
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
		if (activeCancelToken != null && !activeCancelToken.get()) {
			logger.info("User clicked STOP. Signaling cancellation...");
			activeCancelToken.set(true);
			btnFetch.setDisable(true);
			btnFetch.setText("Stopping...");
			return;
		}
		
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
		
		activeCancelToken = new AtomicBoolean(false);
		btnFetch.setText("Stop operation");
		btnFetch.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
		certTable.getScene().setCursor(javafx.scene.Cursor.WAIT);

		// Create a background Task
		javafx.concurrent.Task<Void> fetchTask = new javafx.concurrent.Task<>() {
			@Override
			protected Void call() throws Exception {
				CertificateIssuanceService service = new CertificateIssuanceService();
				service.fetchCertificateForDefinition(selectedView.getId(), activeCancelToken);
				return null;
			}
		};

		// What happens when the process successfully finishes
		fetchTask.setOnSucceeded(e -> finalizeProcess("Certificate successfully issued!", Alert.AlertType.INFORMATION));

		// What happens in case of an error
		fetchTask.setOnFailed(e -> {
			Throwable ex = fetchTask.getException();
			if (ex instanceof CancellationException || ex.getCause() instanceof CancellationException) {
				logger.warn("Operation was cancelled by user.");
				finalizeProcess(null, null);
			} else {
				logger.error("Failed to fetch certificate", ex);
				finalizeProcess("Failed to fetch certificate: " + ex.getMessage(), Alert.AlertType.ERROR);
			}			
		});

		// Start the thread
		Thread thread = new Thread(fetchTask);
		thread.setDaemon(true);
		thread.start();
	}

	private void finalizeProcess(String message, Alert.AlertType alertType) {
		javafx.application.Platform.runLater(() -> {
			activeCancelToken = null;
			btnFetch.setText("Fetch Certificate");
			btnFetch.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
			btnFetch.setDisable(false);
			certTable.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
			
			if (message != null) {
				new Alert(alertType, message).showAndWait();
				refreshData();
			}
		});
	}
}
