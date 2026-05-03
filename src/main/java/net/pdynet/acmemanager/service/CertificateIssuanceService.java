package net.pdynet.acmemanager.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Login;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.OrderBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.connector.HttpConnector;
import org.shredzone.acme4j.connector.NetworkSettings;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.provider.AcmeProvider;
import org.shredzone.acme4j.provider.GenericAcmeProvider;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.AcmeRegistrationDao;
import net.pdynet.acmemanager.dao.CertificateDefinitionDao;
import net.pdynet.acmemanager.dao.CertificateOrderDao;
import net.pdynet.acmemanager.dao.DnsProviderDao;
import net.pdynet.acmemanager.dao.IssuedCertificateDao;
import net.pdynet.acmemanager.model.AcmeRegistration;
import net.pdynet.acmemanager.model.CertificateDefinition;
import net.pdynet.acmemanager.model.CertificateOrder;
import net.pdynet.acmemanager.model.DnsProvider;
import net.pdynet.acmemanager.model.EncryptedString;
import net.pdynet.acmemanager.model.IssuedCertificate;
import net.pdynet.acmemanager.service.dns.DnsChallengeProcessor;
import net.pdynet.acmemanager.service.dns.DnsManager;
import net.pdynet.acmemanager.service.dns.DnsManagerFactory;
import net.pdynet.acmemanager.service.dns.DnsPersistChallengeProcessor;
import net.pdynet.acmemanager.service.http.HttpChallengeProcessor;
import net.pdynet.acmemanager.util.ApiClientUtils;
import net.pdynet.acmemanager.util.BlindTrustManager;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class CertificateIssuanceService {
	private static final Logger logger = LoggerFactory.getLogger(CertificateIssuanceService.class);

	public void fetchCertificateForDefinition(int definitionId) throws Exception {
		// Načtení závislostí z DB
		CertificateDefinition definition = App.getJdbi().withExtension(CertificateDefinitionDao.class,
				dao -> dao.findById(definitionId));
		AcmeRegistration acmeReg = App.getJdbi().withExtension(AcmeRegistrationDao.class,
				dao -> dao.findById(definition.getAcmeRegistrationId()));

		OffsetDateTime now = OffsetDateTime.now();

		AcmeProvider acmeProvider = null;

		if (acmeReg.isTrustAllCertificates()) {
			TrustManager[] trustAllCerts = new TrustManager[] { new BlindTrustManager() };
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());

			HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().sslContext(sc);
			final HttpClient client = httpClientBuilder.build();

			acmeProvider = new GenericAcmeProvider() {
				protected HttpConnector createHttpConnector(NetworkSettings settings, HttpClient httpClient) {
					return new HttpConnector(settings, client);
				}
			};
		}

		// Přihlášení k existujícímu ACME účtu
		URI serverUri = URI.create(acmeReg.getServerUrl());
		URI accountUri = URI.create(acmeReg.getAccountUrl());
		Session session = null;

		if (acmeProvider == null)
			session = new Session(serverUri);
		else
			session = new Session(serverUri, acmeProvider);

		KeyPair accountKeyPair = KeyPairUtils.readKeyPair(new StringReader(acmeReg.getAccountKey().value()));
		Login login = session.login(accountUri.toURL(), accountKeyPair);
		Account account = login.getAccount();

		// Vytvoření objednávky (Order)
		final Set<String> domains = new LinkedHashSet<>();
		domains.add(definition.getDomainName());

		Pattern.compile(",")
				.splitAsStream(definition.getSubjectAltNames() != null ? definition.getSubjectAltNames() : "")
				.map(String::trim).filter(s -> !s.isBlank()).forEach(s -> domains.add(s));

		OrderBuilder orderBuilder = account.newOrder().domains(domains);

		Order order = orderBuilder.create();

		// Uložení Order do databáze (Stav PENDING)
		CertificateOrder certificateOrder = new CertificateOrder();
		certificateOrder.setDefinitionId(definition.getId());
		certificateOrder.setStatus(order.getStatus().toString());
		certificateOrder.setOrderUrl(order.getLocation().toString());
		certificateOrder.setDateWrite(now);
		certificateOrder.setDateEdit(now);

		int orderId = App.getJdbi().withExtension(CertificateOrderDao.class, dao -> dao.insert(certificateOrder));
		certificateOrder.setId(orderId);
		
		// Vyřešení Challenges
		try {
			switch (definition.getChallengeType()) {
				case "DNS-01" -> {
					DnsProvider dnsProvider = App.getJdbi().withExtension(DnsProviderDao.class, dao -> dao.findById(definition.getDnsProviderId()));
					HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
					DnsManager dnsManager = DnsManagerFactory.getDnsManager(httpClientBuilder, dnsProvider);
					DnsChallengeProcessor challengeProcessor = new DnsChallengeProcessor();
					challengeProcessor.processChallenges(dnsManager, order);
				}
				case "HTTP-01" -> {
					Path webrootPath = Paths.get(definition.getWebrootPath());
					HttpChallengeProcessor challengeProcessor = new HttpChallengeProcessor();
					challengeProcessor.processChallenges(webrootPath, order);
				}
				case "DNS-PERSIST-01" -> {
					DnsPersistChallengeProcessor challengeProcessor = new DnsPersistChallengeProcessor();
					challengeProcessor.processChallenges(order);
				}
				case null, default -> throw new IllegalArgumentException("Unknown challenge type.");
			}
		} catch (AcmeException e) {
			certificateOrder.setStatus(Status.INVALID.toString());
			certificateOrder.setErrorMessage(e.getMessage());
			certificateOrder.setDateEdit(now);
			App.getJdbi().useExtension(CertificateOrderDao.class, dao -> dao.update(certificateOrder));
			
			throw e;
		}

		// Generování klíčů pro certifikát a exekuce objednávky
		KeyPair domainKeyPair;
		if ("RSA".equalsIgnoreCase(definition.getKeyAlgorithm())) {
			domainKeyPair = KeyPairUtils.createKeyPair(Integer.parseInt(definition.getKeySizeOrCurve()));
		} else {
			domainKeyPair = KeyPairUtils.createECKeyPair(definition.getKeySizeOrCurve());
		}

		order.execute(domainKeyPair);

		// Polling statusu s updatem DB
		do {
			Thread.sleep(3000L);
			order.fetch();
			now = OffsetDateTime.now();
			certificateOrder.setStatus(order.getStatus().toString());
			certificateOrder.setDateEdit(now);
			App.getJdbi().useExtension(CertificateOrderDao.class, dao -> dao.update(certificateOrder));

			if (order.getStatus() == Status.INVALID) {
				throw new Exception("ACME Order failed. Check application logs.");
			}
		} while (!EnumSet.of(Status.VALID, Status.INVALID).contains(order.getStatus()));

		// Stažení vystaveného certifikátu
		Certificate certificate = order.getCertificate();

		IssuedCertificate issuedCertificate = new IssuedCertificate();
		issuedCertificate.setDefinitionId(definition.getId());
		issuedCertificate.setOrderId(orderId);

		X509Certificate cert = certificate.getCertificate();
		issuedCertificate.setSerialNumber(cert.getSerialNumber().toString(16).toUpperCase());
		issuedCertificate.setNotBefore(OffsetDateTime.ofInstant(cert.getNotBefore().toInstant(), ZoneOffset.UTC));
		issuedCertificate.setNotAfter(OffsetDateTime.ofInstant(cert.getNotAfter().toInstant(), ZoneOffset.UTC));
		issuedCertificate.setDateWrite(now);

		// Konverze do PEM formátu
		try (Writer w = new StringWriter()) {
			try (PemWriter pw = new PemWriter(w)) {
				PemObjectGenerator gen = new JcaMiscPEMGenerator(cert);
				pw.writeObject(gen);
			}

			issuedCertificate.setCertPem(w.toString());
		}

		List<X509Certificate> chain = certificate.getCertificateChain();
		if (chain != null && chain.size() > 0) {
			try (Writer w = new StringWriter()) {
				try (PemWriter pw = new PemWriter(w)) {
					for (X509Certificate crt : chain) {
						PemObjectGenerator gen = new JcaMiscPEMGenerator(crt);
						pw.writeObject(gen);
					}
				}

				issuedCertificate.setChainPem(w.toString());
			}
		}

		PrivateKey privateKey = domainKeyPair.getPrivate();
		try (Writer w = new StringWriter()) {
			try (PemWriter pw = new PemWriter(w)) {
				PemObjectGenerator gen = new JcaMiscPEMGenerator(privateKey);
				pw.writeObject(gen);
			}

			issuedCertificate.setPrivateKeyPem(new EncryptedString(w.toString()));
		}

		// Uložení výsledného certifikátu
		App.getJdbi().withExtension(IssuedCertificateDao.class, dao -> dao.insert(issuedCertificate));
		logger.info("Successfully issued certificate for definition ID: " + definitionId);

		// Post-Processing: Automatický export do souborů
		if (definition.isAutoExport()) {
			exportToFile(definition.getExportPathCert(), issuedCertificate.getCertPem(), "Certificate");
			exportToFile(definition.getExportPathChain(), issuedCertificate.getChainPem(), "Chain");

			String pkPem = issuedCertificate.getPrivateKeyPem().value();
			exportToFile(definition.getExportPathKey(), pkPem, "Private Key");
		}
		
		// Post-Processing: Export do JKS
		if (definition.isAutoExportJks() && StringUtils.isNotBlank(definition.getExportPathJks())) {
			exportToJks(definition, cert, chain, privateKey);
		}

		// Post-Processing: Odeslání na webhook
		if (definition.isSendToWebhook() && StringUtils.isNotBlank(definition.getWebhookUrl())) {
			sendWebhook(definition, cert, chain, privateKey);
		}
		
		// Post-Processing: Spuštění externího skriptu
		if (definition.isRunScript() && definition.getScriptPath() != null && !definition.getScriptPath().isBlank()) {
			try {
				logger.info("Executing post-issuance script: " + definition.getScriptPath());

				ProcessBuilder pb = new ProcessBuilder(definition.getScriptPath(), definition.getDomainName());
				pb.redirectErrorStream(true);
				Process process = pb.start();

				boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
				if (finished) {
					logger.info("Script finished with exit code: " + process.exitValue());
				} else {
					process.destroy();
					logger.warn("Script timed out after 60 seconds and was terminated.");
				}
			} catch (Exception e) {
				logger.error("Failed to execute script: " + definition.getScriptPath(), e);
			}
		}
		
		// Post-Processing: Smazání starých záznamů
		cleanupOldRecords(definition.getId());
	}

	private void exportToFile(String path, String content, String label) {
		if (path == null || path.isBlank() || content == null || content.isBlank())
			return;
		
		try {
			Path file = Paths.get(path);
			if (file.getParent() != null)
				Files.createDirectories(file.getParent());
			Files.writeString(file, content);
			logger.info(label + " auto-exported to: " + path);
		} catch (Exception e) {
			logger.error("Failed to export " + label + " to " + path, e);
		}
	}
	
	private void exportToJks(CertificateDefinition definition, X509Certificate cert, List<X509Certificate> chain, PrivateKey privateKey) {
		if (!definition.isAutoExportJks() || StringUtils.isBlank(definition.getExportPathJks())) {
			return;
		}

		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(null, null);

			Set<java.security.cert.Certificate> chainList = new LinkedHashSet<>();
			chainList.add(cert);
			
			if (chain != null && chain.size() > 0) {
				chain.forEach(c -> chainList.add(c));
			}
			
			String password = (definition.getJksPassword() != null) ? definition.getJksPassword().value() : "";
			char[] passwordChars = password.toCharArray();
	        
			ks.setKeyEntry("main", privateKey, passwordChars, chainList.toArray(new java.security.cert.Certificate[0]));
			
			Path saveTo = Paths.get(definition.getExportPathJks());
			try (OutputStream fos = Files.newOutputStream(saveTo)) {
				ks.store(fos, password.toCharArray());
			}

			logger.info("Certificate successfully exported to JKS: {}", definition.getExportPathJks());
		} catch (Exception e) {
			logger.error("Failed to export JKS for " + definition.getDomainName(), e);
		}
	}
	
	private void sendWebhook(CertificateDefinition definition, X509Certificate cert, List<X509Certificate> chain, PrivateKey privateKey) {
		if (!definition.isSendToWebhook() || StringUtils.isBlank(definition.getWebhookUrl()))
			return;

		try {
			logger.info("Preparing Webhook distribution for domain: {}", definition.getDomainName());
			
			// Sestavení P12 KeyStore v paměti
			KeyStore p12Store = KeyStore.getInstance("PKCS12");
			p12Store.load(null, null);

			Set<java.security.cert.Certificate> chainList = new LinkedHashSet<>();
			chainList.add(cert);
			
			if (chain != null && chain.size() > 0) {
				chain.forEach(c -> chainList.add(c));
			}
			
			// Heslo k P12
			String password = definition.getWebhookPassword() != null ? definition.getWebhookPassword().value() : "";
			char[] passwordChars = password.toCharArray();

			// Uložení klíče a chainu do P12
			p12Store.setKeyEntry("main", privateKey, passwordChars, chainList.toArray(new java.security.cert.Certificate[0]));

			// Export P12 do byte array
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			p12Store.store(bos, passwordChars);
			byte[] p12Bytes = bos.toByteArray();
			
			// Příprava JSON dat
			String hexSerial = cert.getSerialNumber().toString(16).toUpperCase();
			
			String fingerprint = "";
			try {
				byte[] encoded = cert.getEncoded();
				java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
				byte[] digest = md.digest(encoded);
				StringBuilder sb = new StringBuilder();
				for (byte b : digest) sb.append(String.format("%02X", b));
				fingerprint = sb.toString();
			} catch (Exception e) {}
			
			List<String> sans = new ArrayList<>();
			try {
				var altNames = cert.getSubjectAlternativeNames();
				if (altNames != null) {
					for (List<?> entry : altNames) {
						sans.add(entry.get(1).toString());
					}
				}
			} catch (Exception e) {}
			
			String keyAlg = privateKey.getAlgorithm(); // RSA nebo EC
			int keySize = 0;
			if (privateKey instanceof java.security.interfaces.RSAPrivateKey rsa) {
				keySize = rsa.getModulus().bitLength();
			} else if (privateKey instanceof java.security.interfaces.ECPrivateKey ec) {
				keySize = ec.getParams().getOrder().bitLength();
			}
			
			ObjectNode jsonPayload = ApiClientUtils.getObjectMapper().createObjectNode()
					.put("id", definition.getWebhookPayloadId())
					.put("cert", Base64.getEncoder().encodeToString(p12Bytes))
					.put("serial", hexSerial)
					.put("subject", cert.getSubjectX500Principal().getName())
					.put("issuer", cert.getIssuerX500Principal().getName())
					.put("notBefore", cert.getNotBefore().toInstant().toString())
					.put("notAfter", cert.getNotAfter().toInstant().toString())
					.put("fingerprintSha256", fingerprint)
					.put("keyAlgorithm", keyAlg)
					.put("keySize", keySize)
					;
			
			ArrayNode sansArray = jsonPayload.putArray("sans");
			sans.forEach(sansArray::add);
			
			// Odeslání pomocí HttpClient
			HttpClient.Builder clientBuilder = HttpClient.newBuilder();
			if (definition.isWebhookTrustAll()) {
				TrustManager[] trustAllCerts = new TrustManager[] { new BlindTrustManager() };
				SSLContext sc = SSLContext.getInstance("TLS");
				sc.init(null, trustAllCerts, new SecureRandom());
				clientBuilder.sslContext(sc);
			}

			HttpClient client = clientBuilder.build();
			HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
					.uri(URI.create(definition.getWebhookUrl()))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonPayload.toString()));

			// Přidání uživatelských hlaviček
			if (definition.getWebhookHeaders() != null && !definition.getWebhookHeaders().isBlank()) {
				String[] pairs = definition.getWebhookHeaders().split("##");
				for (String pair : pairs) {
					String[] kv = pair.split("=", 2);
					if (kv.length == 2) {
						requestBuilder.header(kv[0].trim(), kv[1].trim());
					}
				}
			}

			HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				logger.info("Webhook delivered successfully to {}. Status: {}", definition.getWebhookUrl(), response.statusCode());
			} else {
				logger.warn("Webhook delivered but server returned error. Status: {}, Body: {}", response.statusCode(), response.body());
			}
		} catch (Exception e) {
			logger.error("Failed to send webhook for domain " + definition.getDomainName(), e);
		}
	}
	
	private void cleanupOldRecords(int definitionId) {
		try {
			App.getJdbi().useTransaction(handle -> {
				CertificateOrderDao dao = handle.attach(CertificateOrderDao.class);
				List<Integer> obsoleteIds = dao.findObsoleteOrderIds(definitionId);

				if (obsoleteIds != null && !obsoleteIds.isEmpty()) {
					logger.info("Cleaning up {} obsolete history records for definition {}", obsoleteIds.size(), definitionId);
					dao.deleteIssuedCertsByOrderIds(obsoleteIds);
					dao.deleteOrdersByIds(obsoleteIds);
				}
			});
		} catch (Exception e) {
			logger.warn("Failed to clean up old certificate history for definition " + definitionId, e);
		}
	}	
}
