package net.pdynet.acmemanager.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.ConfigDao;
import net.pdynet.acmemanager.model.ConfigRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConfigViewController {
	private static final Logger logger = LoggerFactory.getLogger(ConfigViewController.class);

	@FXML
	private TableView<ConfigRecord> tableConfig;
	@FXML
	private TableColumn<ConfigRecord, String> colId, colText;
	@FXML
	private TableColumn<ConfigRecord, Integer> colInt;

	@FXML
	private TextField txtId, txtIntVal, txtTextVal;
	@FXML
	private Button btnDelete;

	@FXML
	public void initialize() {
		// Mapování tabulky
		colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId()));
		colInt.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getIntValue()).asObject());
		colText.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTextValue()));

		// Listener na výběr v tabulce
		tableConfig.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			if (newSelection != null) {
				populateForm(newSelection);
				btnDelete.setDisable(false);
				txtId.setEditable(false); // ID se při editaci nemění
				txtId.setStyle("-fx-background-color: #eeeeee;");
			} else {
				btnDelete.setDisable(true);
			}
		});

		refreshData();
	}

	private void refreshData() {
		List<ConfigRecord> list = App.getJdbi().withExtension(ConfigDao.class, ConfigDao::findAll);
		tableConfig.setItems(FXCollections.observableArrayList(list));
	}

	private void populateForm(ConfigRecord record) {
		txtId.setText(record.getId());
		txtIntVal.setText(String.valueOf(record.getIntValue()));
		txtTextVal.setText(record.getTextValue());
	}

	@FXML
	private void handleNew() {
		tableConfig.getSelectionModel().clearSelection();
		txtId.setText("");
		txtId.setEditable(true);
		txtId.setStyle("");
		txtIntVal.setText("0");
		txtTextVal.setText("");
	}

	@FXML
	private void handleSave() {
		String id = txtId.getText().trim();
		if (id.isEmpty()) {
			new Alert(Alert.AlertType.WARNING, "Config Key (ID) cannot be empty!").showAndWait();
			return;
		}

		try {
			ConfigRecord record = new ConfigRecord();
			record.setId(id);
			record.setTextValue(txtTextVal.getText());

			// Bezpečná konverze čísla
			try {
				record.setIntValue(Integer.parseInt(txtIntVal.getText().trim()));
			} catch (NumberFormatException e) {
				record.setIntValue(0);
			}

			App.getJdbi().useExtension(ConfigDao.class, dao -> dao.save(record));

			refreshData();
			handleNew(); // Reset formuláře po uložení

		} catch (Exception e) {
			logger.error("Failed to save config", e);
			new Alert(Alert.AlertType.ERROR, "Save failed: " + e.getMessage()).showAndWait();
		}
	}

	@FXML
	private void handleDelete() {
		ConfigRecord selected = tableConfig.getSelectionModel().getSelectedItem();
		if (selected == null)
			return;

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete config key '" + selected.getId() + "'?",
				ButtonType.YES, ButtonType.NO);
		
		if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
			App.getJdbi().useExtension(ConfigDao.class, dao -> dao.deleteById(selected.getId()));
			refreshData();
			handleNew();
		}
	}
}
