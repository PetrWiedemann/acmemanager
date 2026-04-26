package net.pdynet.acmemanager.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.DnsProviderDao;
import net.pdynet.acmemanager.model.DnsProvider;

public class DnsProvidersViewController {
	private static final Logger logger = LoggerFactory.getLogger(DnsProvidersViewController.class);
	
	@FXML
	private TableView<DnsProvider> tableProviders;
	@FXML
	private TableColumn<DnsProvider, String> colName;
	@FXML
	private Button btnEdit, btnDelete;

	@FXML
	public void initialize() {
		colName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));

		tableProviders.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
			boolean selected = (newVal != null);
			btnEdit.setDisable(!selected);
			btnDelete.setDisable(!selected);
		});

		tableProviders.setRowFactory(tv -> {
			TableRow<DnsProvider> row = new TableRow<>();
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
		var list = App.getJdbi().withExtension(DnsProviderDao.class, dao -> dao.findAll());
		tableProviders.setItems(FXCollections.observableArrayList(list));
	}

	@FXML
	private void handleNew() {
		openEditor(null);
	}

	@FXML
	private void handleEdit() {
		openEditor(tableProviders.getSelectionModel().getSelectedItem());
	}

	@FXML
	private void handleDelete() {
		DnsProvider selected = tableProviders.getSelectionModel().getSelectedItem();
		if (selected == null)
			return;

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete provider '" + selected.getName() + "'?");
		if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.OK) {
			try {
				App.getJdbi().useExtension(DnsProviderDao.class, dao -> dao.deleteById(selected.getId()));
				refreshData();
			} catch (Exception e) {
				logger.error("Failed to delete provider", e);
				new Alert(Alert.AlertType.ERROR, "Cannot delete provider. It is in use.").showAndWait();
			}
		}
	}

	private void openEditor(DnsProvider provider) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DnsProviderEditor.fxml"));
			Parent root = loader.load();
			DnsProviderEditorController controller = loader.getController();
			controller.setProvider(provider);
			Stage stage = new Stage();
			stage.setTitle("DNS Provider Editor");
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setScene(new Scene(root));
			stage.showAndWait();
			if (controller.isSaved())
				refreshData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
