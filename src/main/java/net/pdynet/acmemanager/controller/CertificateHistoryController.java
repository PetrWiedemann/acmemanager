package net.pdynet.acmemanager.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.CertificateOrderDao;
import net.pdynet.acmemanager.dao.IssuedCertificateDao;
import net.pdynet.acmemanager.model.IssuedCertificateView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CertificateHistoryController {
	private static final Logger logger = LoggerFactory.getLogger(CertificateHistoryController.class);
	private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@FXML
	private TableView<IssuedCertificateView> tableHistory;
	@FXML
	private TableColumn<IssuedCertificateView, String> colDate, colSerial, colExpiry, colStatus;
	@FXML
	private Button btnExport, btnDelete;

	private int definitionId;

	@FXML
	public void initialize() {
		// Formátování datumu: Použijeme dateWrite z Orderu (kdy byla podána žádost)
		colDate.setCellValueFactory(d -> new SimpleStringProperty(
				d.getValue().getDateWrite() != null ? d.getValue().getDateWrite().format(dtf) : ""));
		
		colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrderStatus()));
	    
		// Sériové číslo a expirace mohou být null u nezdařených pokusů
		colSerial.setCellValueFactory(d -> new SimpleStringProperty(
				d.getValue().getSerialNumber() != null ? d.getValue().getSerialNumber() : "N/A"));
		
		colExpiry.setCellValueFactory(d -> new SimpleStringProperty(
				d.getValue().getNotAfter() != null ? d.getValue().getNotAfter().format(dtf) : "-"));
		
		tableHistory.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
			boolean selected = (newVal != null);
			btnDelete.setDisable(!selected);
			// Export povolit jen pokud existuje PEM data (úspěšný certifikát)
			btnExport.setDisable(!selected || newVal.getCertPem() == null);
		});		
	}

	public void initData(int definitionId) {
		this.definitionId = definitionId;
		refreshData();
	}

	private void refreshData() {
		List<IssuedCertificateView> history = App.getJdbi().withExtension(IssuedCertificateDao.class,
				dao -> dao.findHistoryByDefinitionId(definitionId));
		tableHistory.setItems(FXCollections.observableArrayList(history));
	}

	@FXML
	private void handleExport() {
		IssuedCertificateView cert = tableHistory.getSelectionModel().getSelectedItem();
		if (cert == null)
			return;

		FileChooser fc = new FileChooser();
		fc.setTitle("Save Certificate Backup (ZIP)");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Archives (*.zip)", "*.zip"));
		fc.setInitialFileName("cert_" + cert.getSerialNumber() + ".zip");
        
		File zipFile = fc.showSaveDialog(tableHistory.getScene().getWindow());

		if (zipFile != null) {
			try (OutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
                addZipEntry(zos, "cert.pem", cert.getCertPem());
                
                if (cert.getChainPem() != null && !cert.getChainPem().isBlank()) {
                	addZipEntry(zos, "chain.pem", cert.getChainPem());
                }
                
                String privateKey = cert.getPrivateKeyPem().value();
                addZipEntry(zos, "privkey.pem", privateKey);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Certificates exported successfully to\n" + zipFile.getAbsolutePath());
                alert.showAndWait();
			} catch (Exception e) {
				logger.error("ZIP Export failed", e);
				new Alert(Alert.AlertType.ERROR, "Failed to save ZIP file: " + e.getMessage()).showAndWait();
			}
		}
	}
	
	private void addZipEntry(ZipOutputStream zos, String filename, String content) throws IOException {
		ZipEntry entry = new ZipEntry(filename);
		zos.putNextEntry(entry);
		zos.write(content.getBytes(StandardCharsets.UTF_8));
		zos.closeEntry();
	}
	
	@FXML
	private void handleDelete() {
		IssuedCertificateView cert = tableHistory.getSelectionModel().getSelectedItem();
		if (cert == null) return;

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete this certificate record from history? Files already exported will not be affected.",
				ButtonType.YES, ButtonType.NO);

		if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
			int orderId = cert.getOrderId();
			
			App.getJdbi().useTransaction(handle -> {
				handle.attach(IssuedCertificateDao.class).deleteByOrderId(orderId);
				handle.attach(CertificateOrderDao.class).deleteById(orderId);
			});
			
			refreshData();
		}		
	}

	@FXML
	private void handleClose() {
		((Stage) tableHistory.getScene().getWindow()).close();
	}
}
