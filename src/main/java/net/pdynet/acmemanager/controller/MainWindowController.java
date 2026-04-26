package net.pdynet.acmemanager.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class MainWindowController {
	private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);

	@FXML
	private StackPane contentArea;

	@FXML
	private Button btnNavCert;
	@FXML
	private Button btnNavAcme;
	@FXML
	private Button btnNavDns;
	@FXML
	private Button btnNavConfig;

	@FXML
	public void initialize() {
		showCertificatesView();
	}

	private void setActiveNav(Button activeBtn) {
		btnNavCert.getStyleClass().remove("sidebar-btn-active");
		btnNavAcme.getStyleClass().remove("sidebar-btn-active");
		btnNavDns.getStyleClass().remove("sidebar-btn-active");
		btnNavConfig.getStyleClass().remove("sidebar-btn-active");

		if (activeBtn != null) {
			activeBtn.getStyleClass().add("sidebar-btn-active");
		}
	}

	@FXML
	private void showCertificatesView() {
		loadView("/fxml/CertificatesView.fxml");
		setActiveNav(btnNavCert);
	}

	@FXML
	private void showAcmeRegistrationsView() {
		loadView("/fxml/AcmeRegistrationsView.fxml");
		setActiveNav(btnNavAcme);
	}

	@FXML
	private void showDnsProvidersView() {
		loadView("/fxml/DnsProvidersView.fxml");
		setActiveNav(btnNavDns);
	}

	@FXML
	private void showConfigView() {
		loadView("/fxml/ConfigView.fxml");
		setActiveNav(btnNavConfig);
	}
	
	private void loadView(String fxmlPath) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
			Parent view = loader.load();
			contentArea.getChildren().setAll(view);
		} catch (IOException e) {
			logger.error("Failed to load view: " + fxmlPath, e);
		}
	}
}