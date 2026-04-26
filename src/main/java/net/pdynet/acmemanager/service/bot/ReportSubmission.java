package net.pdynet.acmemanager.service.bot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pdynet.acmemanager.util.ApiClientUtils;

public class ReportSubmission {
	private static final Logger logger = LoggerFactory.getLogger(ReportSubmission.class);
	private final HttpClient httpClient;
	private final URI uri;
	private final String cryptKey;
	private Duration timeout = Duration.ofSeconds(30);
	
	public ReportSubmission(final HttpClient httpClient, final URI uri, final String cryptKey) {
		this.httpClient = httpClient;
		this.uri = uri;
		this.cryptKey = cryptKey;
	}
	
	public void sendMessage(final Message message) {
		Instant now = Instant.now();
		String authStamp = Long.toString(now.getEpochSecond());
		HmacUtils hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, cryptKey);
		String auth = Base64.getEncoder().encodeToString(hmac.hmac(message.getS4wConnectId() + authStamp));
		String json = ApiClientUtils.getObjectMapper().writeValueAsString(message);
		
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(uri)
				.timeout(timeout)
				.method("POST", HttpRequest.BodyPublishers.ofString(json))
				.header("Content-Type", "application/json")
				.header("X-S4W-Auth-MAC", auth)
				.header("X-S4W-Auth-Timestamp", authStamp)
				;
		
		try {
			httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
		} catch (Exception e) {
			logger.error("Report submission error", e);
		}
	}
}
