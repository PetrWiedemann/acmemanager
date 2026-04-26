package net.pdynet.acmemanager.service;

import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.CertificateDefinitionDao;
import net.pdynet.acmemanager.model.CertificateDefinitionView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.time.OffsetDateTime;
import java.util.List;

public class RenewalService {
	private static final Logger logger = LoggerFactory.getLogger(RenewalService.class);

	public void runAutomatedRenewal() {
		logger.info("=================================================");
		logger.info("Starting automated ACME certificate renewal process");
		logger.info("=================================================");

		OffsetDateTime now = OffsetDateTime.now();

		List<CertificateDefinitionView> definitions = App.getJdbi().withExtension(CertificateDefinitionDao.class,
				dao -> dao.findDefinitionsForRenewal(now));

		if (definitions.isEmpty()) {
			logger.info("All auto-renew certificates are up to date. No action required.");
			logger.info("=================================================");
			return;
		}

		int successCount = 0;
		int failCount = 0;
		CertificateIssuanceService issuanceService = new CertificateIssuanceService();

		for (CertificateDefinitionView def : definitions) {
			String message = MessageFormatter.arrayFormat("Certificate {} for '{}' is due for renewal. (Expires: {})",
					new Object[] { def.getName(), def.getDomainName(), def.getExpiryDate() })
					.getMessage();
			
			logger.info(message);
			App.sendBotReport(message);
			
			try {
				issuanceService.fetchCertificateForDefinition(def.getId());
				successCount++;
				
				message = MessageFormatter.format("SUCCESS: Certificate for '{}' renewed successfully.", 
						def.getDomainName())
						.getMessage();
				
				logger.info(message);
				App.sendBotReport(message);
			} catch (Exception e) {
				failCount++;
				
				message = MessageFormatter.format("FAILED: Could not renew certificate for '{}'. Reason: {}", 
						def.getDomainName(),
						e.getMessage())
						.getMessage();
				
				logger.error(message, e);
				App.sendBotReport(message);
			}
		}

		logger.info("=================================================");
		logger.info("Renewal process finished. Success: {}, Failed: {}", successCount, failCount);
		logger.info("=================================================");
	}
}
