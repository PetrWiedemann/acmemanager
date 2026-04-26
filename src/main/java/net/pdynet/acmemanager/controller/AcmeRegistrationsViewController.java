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
import net.pdynet.acmemanager.dao.AcmeRegistrationDao;
import net.pdynet.acmemanager.model.AcmeRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AcmeRegistrationsViewController {
	private static final Logger logger = LoggerFactory.getLogger(AcmeRegistrationsViewController.class);

	@FXML
	private TableView<AcmeRegistration> tableRegistrations;
	@FXML
	private TableColumn<AcmeRegistration, String> colName;
	@FXML
	private TableColumn<AcmeRegistration, String> colUrl;

	@FXML
	private Button btnRename;
	@FXML
	private Button btnDelete;

	@FXML
	public void initialize() {
		colName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
		colUrl.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getServerUrl()));

		tableRegistrations.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
			boolean isSelected = (newVal != null);
			btnRename.setDisable(!isSelected);
			btnDelete.setDisable(!isSelected);
		});

		tableRegistrations.setRowFactory(tv -> {
			TableRow<AcmeRegistration> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!row.isEmpty())) {
					handleRename();
				}
			});
			return row;
		});

		refreshData();
	}

	private void refreshData() {
		var list = App.getJdbi().withExtension(AcmeRegistrationDao.class, dao -> dao.findAll());
		tableRegistrations.setItems(FXCollections.observableArrayList(list));
	}

	@FXML
	private void handleNew() {
		openEditor(null);
	}

	@FXML
	private void handleRename() {
		AcmeRegistration selected = tableRegistrations.getSelectionModel().getSelectedItem();
		if (selected != null) {
			openEditor(selected);
		}
	}

	@FXML
	private void handleDelete() {
		AcmeRegistration selected = tableRegistrations.getSelectionModel().getSelectedItem();
		if (selected == null)
			return;

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete registration '" + selected.getName() + "'?",
				ButtonType.YES, ButtonType.NO);
		
		if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
			try {
				App.getJdbi().useExtension(AcmeRegistrationDao.class, dao -> dao.deleteById(selected.getId()));
				refreshData();
			} catch (Exception e) {
				logger.error("Failed to delete registration", e);
				new Alert(Alert.AlertType.ERROR, "Cannot delete account. It is in use by certificate definitions.")
						.showAndWait();
			}
		}
	}

	private void openEditor(AcmeRegistration registration) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AcmeRegistrationEditor.fxml"));
			Parent root = loader.load();

			AcmeRegistrationEditorController controller = loader.getController();
			controller.setRegistration(registration);

			Stage stage = new Stage();
			stage.setTitle(registration == null ? "New ACME Account" : "Rename ACME Account");
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setScene(new Scene(root));
			stage.showAndWait();

			if (controller.isSaved())
				refreshData();
		} catch (IOException e) {
			logger.error("Failed to open ACME editor", e);
		}
	}
}
