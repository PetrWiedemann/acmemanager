package net.pdynet.acmemanager.service.dns;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InternetDomainName;

import net.pdynet.acmemanager.util.ApiClientUtils;
import net.pdynet.acmemanager.util.ApiException;
import tools.jackson.databind.node.ObjectNode;

public class DnsManagerRegZone implements DnsManager {
	private static final Logger logger = LoggerFactory.getLogger(DnsManagerRegZone.class);
	private final String baseUrl = "https://api.czechia.com";
	private final HttpClient httpClient;
	private final String apiKey;

	private Duration timeout = Duration.ofSeconds(30);

	public DnsManagerRegZone(final HttpClient.Builder httpBuilder, final String apiKey) {
		this.httpClient = httpBuilder.connectTimeout(timeout).build();
		this.apiKey = apiKey;
	}

	private String[] splitResourceName(final String fullRecordName) {
		// 1. Očištění případné tečky na konci (absolutní FQDN)
		String cleanName = fullRecordName.endsWith(".")
				? fullRecordName.substring(0, fullRecordName.length() - 1)
				: fullRecordName;

		// 2. Příprava "čistého" názvu pro striktní knihovnu Guava.
		// Guava nesnese znak podtržítka '_'. Odstraníme tedy prefix z dočasného stringu.
		String parseableName = cleanName;
		if (parseableName.startsWith("_acme-challenge.")) {
			parseableName = parseableName.substring(16); // 16 = délka "_acme-challenge."
		}

		// Pro případ, že by doména obsahovala i wildcard hvězdičku '*' (kterou Guava
		// také nemá ráda)
		if (parseableName.startsWith("*.")) {
			parseableName = parseableName.substring(2);
		}

		// 3. Guava nyní analyzuje např. "test10.domena.cz" (což už projde bez chyby)
		InternetDomainName domainName = InternetDomainName.from(parseableName);
		String baseDomain = domainName.topPrivateDomain().toString();

		// 4. Název záznamu ořízneme z toho PŮVODNÍHO stringu, abychom měli správně "_acme-challenge"
		String recordName = cleanName.substring(0, cleanName.length() - baseDomain.length() - 1);

		return new String[] { recordName, baseDomain };
	}

	protected void createTxtRecord(final String baseDomain, final String recordName, final String recordContent) throws ApiException {
		String httpMethod = "POST";
		String path = String.format("/api/DNS/%s/TXT", baseDomain);

		ObjectNode dnsRecord = ApiClientUtils.getObjectMapper().createObjectNode()
				.put("hostName", recordName)
				.put("text", recordContent)
				.put("ttl", 300)
				.put("publishZone", 1)
				;

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + path))
				.timeout(timeout)
				.method(httpMethod, HttpRequest.BodyPublishers.ofString(dnsRecord.toString()))
				.header("Content-Type", "application/json")
				.header("authorizationToken", apiKey)
				;

		try {
			logger.info("Creating new DNS record {} with value {}.", recordName, recordContent);

			HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
			int statusCode = response.statusCode();

			logger.info("HTTP response code {}.", statusCode);

			if (statusCode != 200)
				throw ApiClientUtils.getApiException(path, response);

		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}

	protected void deleteTxtRecord(final String baseDomain, final String recordName, final String recordContent)
			throws ApiException {
		String httpMethod = "DELETE";
		String path = String.format("/api/DNS/%s/TXT", baseDomain);

		ObjectNode dnsRecord = ApiClientUtils.getObjectMapper().createObjectNode()
				.put("hostName", recordName)
				.put("text", recordContent)
				.put("publishZone", 1)
				;

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + path))
				.timeout(timeout)
				.method(httpMethod, HttpRequest.BodyPublishers.ofString(dnsRecord.toString()))
				.header("Content-Type", "application/json")
				.header("authorizationToken", apiKey)
				;

		try {
			logger.info("Deleteing DNS record {} with value {}.", recordName, recordContent);

			HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
			int statusCode = response.statusCode();

			logger.info("HTTP response code {}.", statusCode);

			if (statusCode != 200)
				throw ApiClientUtils.getApiException(path, response);

		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}

	@Override
	public void createTxtRecord(DnsChallengeTask task) throws ApiException {
		String[] resourceParts = splitResourceName(task.getRecordName());
		String recordName = resourceParts[0];
		String domainName = resourceParts[1];
		createTxtRecord(domainName, recordName, task.getChallenge());
	}

	@Override
	public void deleteTxtRecord(DnsChallengeTask task) throws ApiException {
		String[] resourceParts = splitResourceName(task.getRecordName());
		String recordName = resourceParts[0];
		String domainName = resourceParts[1];
		deleteTxtRecord(domainName, recordName, task.getChallenge());
	}
}
