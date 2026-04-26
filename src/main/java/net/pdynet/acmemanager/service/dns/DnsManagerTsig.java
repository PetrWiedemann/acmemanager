package net.pdynet.acmemanager.service.dns;

import java.io.IOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InternetDomainName;

import net.pdynet.acmemanager.util.ApiException;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Update;

public class DnsManagerTsig implements DnsManager {
	private static final Logger logger = LoggerFactory.getLogger(DnsManagerTsig.class);

	private final String dnsServer;
	private final String keyName;
	private final String keySecret;
	private final Name algorithm;

	/**
	 * @param dnsServer IP adresa nebo hostname Master DNS serveru (např. "ns1.mojedomena.cz" nebo "192.168.1.10")
	 * @param keyName Název TSIG klíče (např. "cert-key")
	 * @param keySecret Base64 zakódované tajemství klíče
	 * @param algorithm Řetězec identifikující algoritmus (např. "HMAC-SHA256", "HMAC-SHA512", "HMAC-MD5")
	 */
	public DnsManagerTsig(String dnsServer, String keyName, String keySecret, String algorithm) {
		this.dnsServer = dnsServer;
		this.keyName = keyName;
		this.keySecret = keySecret;
		this.algorithm = determineAlgorithm(algorithm);
	}

	@Override
	public void createTxtRecord(DnsChallengeTask task) throws ApiException {
		logger.info("Creating TSIG dynamic update for TXT record: {}", task.getRecordName());
		sendDynamicUpdate(task.getRecordName(), task.getChallenge(), true);
	}

	@Override
	public void deleteTxtRecord(DnsChallengeTask task) throws ApiException {
		logger.info("Deleting TSIG dynamic update for TXT record: {}", task.getRecordName());
		sendDynamicUpdate(task.getRecordName(), task.getChallenge(), false);
	}

	private void sendDynamicUpdate(String fullRecordName, String challenge, boolean isAdd) throws ApiException {
		try {
			// Zjištění zóny (base domain) pro Update zprávu. RFC 2136 vyžaduje zónu.
			String baseDomain = getZoneName(fullRecordName);

			// dnsjava vyžaduje u absolutních doménových jmen tečku na konci
			Name zone = Name.fromString(baseDomain + ".");
			Name record = Name.fromString(fullRecordName + ".");

			// Sestavení dynamického updatu pro danou zónu
			Update update = new Update(zone);
			
			// Vytvoření TXT záznamu (TTL nastavíme třeba na 300 vteřin)
			TXTRecord txtRecord = new TXTRecord(record, DClass.IN, 300, challenge);

			if (isAdd) {
				update.add(txtRecord);
			} else {
				update.delete(txtRecord);
			}

			// Nastavení resolveru a TSIG podepisování
			Resolver resolver = new SimpleResolver(dnsServer);
			resolver.setTimeout(Duration.ofSeconds(10));
			
			TSIG tsig = new TSIG(algorithm, keyName, keySecret);
			resolver.setTSIGKey(tsig);

			// Odeslání updatu na DNS server
			Message response = resolver.send(update);

			// Kontrola výsledku
			int rcode = response.getRcode();
			if (rcode != Rcode.NOERROR) {
				String errorMsg = Rcode.string(rcode);
				logger.error("Dynamic DNS update failed with RCODE: {}", errorMsg);
				throw new ApiException("Dynamic DNS update failed. Server returned: " + errorMsg);
			}

			logger.info("Dynamic DNS update successful.");

		} catch (IOException e) {
			logger.error("Failed to communicate with DNS server {}", dnsServer, e);
			throw new ApiException("DNS update connection error: " + e.getMessage(), e);
		}
	}

	/**
	 * Pomocná metoda pro získání kořenové zóny pomocí Guavy.
	 * DNS Update musí směřovat na autoritativní zónu.
	 */
	private String getZoneName(String fullRecordName) {
		String cleanName = fullRecordName.endsWith(".")
				? fullRecordName.substring(0, fullRecordName.length() - 1)
				: fullRecordName;

		String parseableName = cleanName;
		if (parseableName.startsWith("_acme-challenge.")) {
			parseableName = parseableName.substring(16);
		}
		if (parseableName.startsWith("*.")) {
			parseableName = parseableName.substring(2);
		}

		InternetDomainName domainName = InternetDomainName.from(parseableName);
		return domainName.topPrivateDomain().toString();
	}

	/**
	 * Překlad textového názvu algoritmu na konstantu z knihovny dnsjava.
	 */
	private Name determineAlgorithm(String algStr) {
		if (algStr == null) return TSIG.HMAC_SHA256; // Výchozí

		String upper = algStr.toUpperCase().replace("-", "_");
		return switch (upper) {
			case "HMAC_MD5", "MD5" -> TSIG.HMAC_MD5;
			case "HMAC_SHA1", "SHA1" -> TSIG.HMAC_SHA1;
			case "HMAC_SHA224" -> TSIG.HMAC_SHA224;
			case "HMAC_SHA256", "SHA256" -> TSIG.HMAC_SHA256;
			case "HMAC_SHA384" -> TSIG.HMAC_SHA384;
			case "HMAC_SHA512", "SHA512" -> TSIG.HMAC_SHA512;
			default -> TSIG.HMAC_SHA256;
		};
	}
}
