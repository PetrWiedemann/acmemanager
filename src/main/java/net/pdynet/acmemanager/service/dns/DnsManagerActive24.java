package net.pdynet.acmemanager.service.dns;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pdynet.acmemanager.util.ApiClientUtils;
import net.pdynet.acmemanager.util.ApiException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class DnsManagerActive24 implements DnsManager {
	private static final Logger logger = LoggerFactory.getLogger(DnsManagerActive24.class);
	private final String baseUrl = "https://rest.active24.cz";
	private final HttpClient httpClient;
	private final String apiKey;
	private final String apiSecret;
	
	private Map<String, Long> serviceMap = null;
	private Duration timeout = Duration.ofSeconds(30);
	private HmacUtils hmac;
	
	public DnsManagerActive24(final HttpClient.Builder httpBuilder, final String apiKey, final String apiSecret) {
		this.httpClient = httpBuilder.connectTimeout(timeout).build();
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}
	
	protected Long findService(final String domain) throws ApiException {
		if (serviceMap == null)
			populateServiceMap();
		
		return serviceMap.get(domain.toLowerCase());
	}
	
	protected void populateServiceMap() throws ApiException {
		Instant now = Instant.now();
		String httpMethod = "GET";
		String path = "/v1/user/self/service";
		String canonicalRequest = String.format("%s %s %d", httpMethod, path, now.getEpochSecond());
		String auth = getBasicAuthorization(canonicalRequest);
		
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + path))
				.timeout(timeout)
				.method(httpMethod, HttpRequest.BodyPublishers.noBody())
				.header("Accept", "application/json")
				.header("Authorization", "Basic " + auth)
				.header("Date", now.toString())
				.header("X-Date", now.toString())
				;
		
		try {
			HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
			int statusCode = response.statusCode();
			
			if (statusCode != 200)
				throw ApiClientUtils.getApiException(path, response);
			
			if (serviceMap == null)
				serviceMap = new HashMap<>();
			
			JsonNode jsonResponse = ApiClientUtils.getJsonBody(response);
			
			StreamSupport.stream(jsonResponse.get("items").spliterator(), false)
					.filter(node -> node.has("id") && node.has("serviceName") && node.has("name") && node.has("status")
							&& Strings.CS.equals("domain", node.get("serviceName").asString())
							&& Strings.CS.equals("active", node.get("status").asString()))
					.forEach(node -> serviceMap.put(node.get("name").asString().toLowerCase(), node.get("id").asLong()))
					;
			
			logger.debug("Found services: {}", serviceMap);
			
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}
	
	protected Long findTxtRecord(final long serviceId, final String recordName, final String recordContent) throws ApiException {
		Instant now = Instant.now();
		String httpMethod = "GET";
		String path = String.format("/v2/service/%d/dns/record", serviceId);
		String canonicalRequest = String.format("%s %s %d", httpMethod, path, now.getEpochSecond());
		String auth = getBasicAuthorization(canonicalRequest);
		
		path = new StringBuilder(512)
				.append(path)
				.append("?filters[type][0]=TXT&filters[name]=")
				.append(ApiClientUtils.urlEncode(recordName))
				.append("&filters[content]=")
				.append(ApiClientUtils.urlEncode(recordContent))
				.toString()
				;
		
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + path))
				.timeout(timeout)
				.method(httpMethod, HttpRequest.BodyPublishers.noBody())
				.header("Accept", "application/json")
				.header("Authorization", "Basic " + auth)
				.header("Date", now.toString())
				.header("X-Date", now.toString())
				;
		
		try {
			logger.info("Searching for record {} with value {}.", recordName, recordContent);
			
			HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
			int statusCode = response.statusCode();
			
			logger.info("HTTP response code {}.", statusCode);
			
			if ((statusCode / 100) != 2)
				throw ApiClientUtils.getApiException(path, response);
			
			JsonNode jsonResponse = ApiClientUtils.getJsonBody(response);
			
			String domain = serviceMap.entrySet().stream().filter(e -> e.getValue() == serviceId).map(e -> e.getKey()).findFirst().orElseThrow();
			String requiredName = recordName + "." + domain;
			
			Long recordId = StreamSupport.stream(jsonResponse.get("data").spliterator(), false)
					.filter(node -> Strings.CI.equals(node.get("name").asString(), requiredName) && Strings.CS.equals(node.get("content").asString(), recordContent))
					.map(node -> node.get("id").asLong())
					.findFirst()
					.orElse(null)
					;
			
			if (recordId == null)
				logger.info("Record not found.");
			else
				logger.info("Found record with ID {}.", recordId);
			
			return recordId;
			
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}
	
	protected void createTxtRecord(final long serviceId, final String recordName, final String recordContent) throws ApiException {
		Instant now = Instant.now();
		String httpMethod = "POST";
		String path = String.format("/v2/service/%d/dns/record", serviceId);
		String canonicalRequest = String.format("%s %s %d", httpMethod, path, now.getEpochSecond());
		String auth = getBasicAuthorization(canonicalRequest);
		
		ObjectNode dnsRecord = ApiClientUtils.getObjectMapper().createObjectNode()
				.put("type", "TXT")
				.put("name", recordName)
				.put("content", recordContent)
				.put("ttl", 300)
				;
		
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + path))
				.timeout(timeout)
				.method(httpMethod, HttpRequest.BodyPublishers.ofString(dnsRecord.toString()))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Authorization", "Basic " + auth)
				.header("Date", now.toString())
				.header("X-Date", now.toString())
				;
		
		try {
			logger.info("Creating new DNS record {} with value {}.", recordName, recordContent);
			
			HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
			int statusCode = response.statusCode();
			
			logger.info("HTTP response code {}.", statusCode);
			
			if (statusCode != 204)
				throw ApiClientUtils.getApiException(path, response);
			
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}
	
	protected void deleteTxtRecord(final long serviceId, final long recordId) throws ApiException {
		Instant now = Instant.now();
		String httpMethod = "DELETE";
		String path = String.format("/v2/service/%d/dns/record/%d", serviceId, recordId);
		String canonicalRequest = String.format("%s %s %d", httpMethod, path, now.getEpochSecond());
		String auth = getBasicAuthorization(canonicalRequest);
		
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + path))
				.timeout(timeout)
				.method(httpMethod, HttpRequest.BodyPublishers.noBody())
				.header("Accept", "application/json")
				.header("Authorization", "Basic " + auth)
				.header("Date", now.toString())
				.header("X-Date", now.toString())
				;
		
		try {
			logger.info("Deleteing DNS record with ID {}.", recordId);
			
			HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
			int statusCode = response.statusCode();
			
			logger.info("HTTP response code {}.", statusCode);
			
			if (statusCode != 204)
				throw ApiClientUtils.getApiException(path, response);
			
		} catch (IOException e) {
			throw new ApiException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiException(e);
		}
	}
	
	private HmacUtils getHmac() {
		if (hmac == null)
			hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, apiSecret);
		return hmac;
	}
	
	private String getHmacDigest(final String data) {
		return getHmac().hmacHex(data);
	}
	
	private String getBasicAuthorization(final String data) {
		String digest = getHmacDigest(data);
		return Base64.getEncoder().encodeToString((apiKey + ":" + digest).getBytes());
	}
	
	private String[] splitResourceName(final String resourceName) throws ApiException {
		String[] labels = resourceName.split("\\.");
		
		String domainName = null;
		String recordName = null;
		
		for (int i = 1; i < labels.length; i++) {
			StringBuilder sb = new StringBuilder();
			
			for (int j = labels.length - 1 - i; j < labels.length; j++) {
				if (sb.length() > 0) sb.append(".");
				sb.append(labels[j]);
				
				String candidate = sb.toString();
				Long serviceId = findService(candidate.toLowerCase());
				
				if (serviceId != null) {
					domainName = candidate;
					recordName = resourceName.substring(0, resourceName.length() - domainName.length() - 1);
					break;
				}
			}
		}
		
		return new String[] { recordName, domainName };
	}
	
	@Override
	public void createTxtRecord(final DnsChallengeTask task) throws ApiException {
		 String[] resourceParts = splitResourceName(task.getRecordName());
		 String recordName = resourceParts[0];
		 String domainName = resourceParts[1];
		 
		 if (domainName == null)
			 throw new ApiException("The domain was not found at the selected DNS service.");
		 
		 Long serviceId = findService(domainName.toLowerCase());
		 
		 Long recordId = findTxtRecord(serviceId, recordName, task.getChallenge());
		 
		 if (recordId == null)
			 createTxtRecord(serviceId, recordName, task.getChallenge());
	}

	@Override
	public void deleteTxtRecord(final DnsChallengeTask task) throws ApiException {
		 String[] resourceParts = splitResourceName(task.getRecordName());
		 String recordName = resourceParts[0];
		 String domainName = resourceParts[1];
		 
		 if (domainName == null)
			 throw new ApiException("The domain was not found at the selected DNS service.");
		 
		 Long serviceId = findService(domainName.toLowerCase());
		 
		 Long recordId = findTxtRecord(serviceId, recordName, task.getChallenge());
		 
		 if (recordId != null)
			 deleteTxtRecord(serviceId, recordId);
	}
}
