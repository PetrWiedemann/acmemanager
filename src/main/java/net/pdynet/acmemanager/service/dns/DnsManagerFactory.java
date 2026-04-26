package net.pdynet.acmemanager.service.dns;

import java.net.http.HttpClient;

import org.apache.commons.lang3.StringUtils;

import net.pdynet.acmemanager.model.DnsProvider;

public class DnsManagerFactory {

	public static DnsManager getDnsManager(final HttpClient.Builder httpClientBuilder, final DnsProvider dnsProvider) {
		DnsManager dnsManager = switch (dnsProvider.getDnsServiceId()) {
			case 1 -> new DnsManagerActive24(httpClientBuilder,
					dnsProvider.getApiKey().value(),
					dnsProvider.getSecretKey().value());
			
			case 2 -> new DnsManagerRegZone(httpClientBuilder,
					dnsProvider.getApiKey().value());
	
			case 3 -> {
				String[] parts = dnsProvider.getApiKey().value().split(";");
				if (parts.length != 3)
					throw new IllegalArgumentException("Invalid TSIG config in API key field");
				
				yield new DnsManagerTsig(StringUtils.trimToEmpty(parts[1]),
						StringUtils.trimToEmpty(parts[2]),
						dnsProvider.getSecretKey().value(),
						StringUtils.trimToEmpty(parts[0]));
			}
			
			default -> throw new IllegalArgumentException("Unsupported DNS provider");
		};

		return dnsManager;
	}
}
