package net.pdynet.acmemanager.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

public class ApiClientUtils {
	private static ObjectMapper mapper;

	public static InputStream getResponseBody(HttpResponse<InputStream> response) throws IOException {
		if (response == null) {
			return null;
		}

		InputStream body = response.body();

		if (body == null) {
			return null;
		}

		Optional<String> encoding = response.headers().firstValue("Content-Encoding");

		if (encoding.isPresent()) {
			for (String token : encoding.get().split(",")) {
				if ("gzip".equalsIgnoreCase(token.trim())) {
					return new GZIPInputStream(body, 8192);
				}
			}
		}

		return body;
	}

	public static ApiException getApiException(String operationId, HttpResponse<InputStream> response)
			throws IOException {
		InputStream responseBody = getResponseBody(response);
		String body = null;

		try {
			body = responseBody == null ? null : new String(responseBody.readAllBytes(), StandardCharsets.UTF_8);
		} finally {
			if (responseBody != null) {
				responseBody.close();
			}
		}

		String message = formatExceptionMessage(operationId, response.statusCode(), body);
		return new ApiException(response.statusCode(), message, response.headers(), body);
	}

	public static String formatExceptionMessage(String operationId, int statusCode, String body) {
		if (body == null || body.isEmpty()) {
			body = "[no body]";
		}

		return operationId + " call failed with: " + statusCode + " - " + body;
	}

	public static ObjectMapper getObjectMapper() {
		if (mapper == null) {
			JsonMapper.Builder jsonMapperBuilder = JsonMapper.builder()
					.changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_NULL))
					.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
					.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
					.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
					.enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
					;

			mapper = jsonMapperBuilder.build();
		}

		return mapper;
	}

	public static JsonNode getJsonBody(HttpResponse<InputStream> httpResponse) throws IOException {
		try (InputStream is = getResponseBody(httpResponse)) {
			JsonNode root = getObjectMapper().readTree(is);
			return root;
		}
	}

	public static String urlEncode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
