package net.pdynet.acmemanager.service.dns;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Strings;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.lookup.LookupSession;
import org.xbill.DNS.lookup.NoSuchDomainException;

import net.pdynet.acmemanager.App;
import net.pdynet.acmemanager.dao.ConfigDao;
import net.pdynet.acmemanager.model.ConfigRecord;
import net.pdynet.acmemanager.service.Threading;
import net.pdynet.acmemanager.util.ApiException;

public class DnsChallengeProcessor {
	private static final Logger logger = LoggerFactory.getLogger(DnsChallengeProcessor.class);
	private final String[] dnsServers = { null, "8.8.8.8", "8.8.4.4" };
	
	public void processChallenges(final DnsManager dnsManager, final Order order) throws ApiException, InterruptedException, AcmeException {
		List<DnsChallengeTask> tasks = new ArrayList<>();
		List<Dns01Challenge> challenges = new ArrayList<>();
		
		for (Authorization auth : order.getAuthorizations()) {
			if (auth.getStatus() == Status.VALID) {
				logger.info("Authorization for domain {} is already VALID. Skipping DNS challenge.", auth.getIdentifier().getDomain());
				continue; 
			}
			
			Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.class).orElse(null);
			
			if (challenge == null) {
				throw new AcmeException("DNS-01 challenge is not available for domain " + auth.getIdentifier().getDomain());
			}
			
			logger.debug("Challenge status {}", challenge.getStatus());

			if (challenge.getStatus() == Status.VALID) {
				logger.info("Challenge for domain {} is already VALID. Skipping trigger.", auth.getIdentifier().getDomain());
				continue;
			}
	        
			String resourceName = challenge.getRRName(auth.getIdentifier());
			String digest = challenge.getDigest();
			DnsChallengeTask task = new DnsChallengeTask(Strings.CS.removeEnd(resourceName, "."), digest);
			dnsManager.createTxtRecord(task);
			tasks.add(task);
			challenges.add(challenge);
		}
		
		// DNS popagation delay.
		ConfigRecord configRecord = App.getJdbi().withExtension(ConfigDao.class, dao -> dao.findById("dns_propagation_delay"));
		Duration dnsPropagationDelay;
		
		if (configRecord == null)
			dnsPropagationDelay = Duration.ofSeconds(3);
		else
			dnsPropagationDelay = Duration.ofSeconds(configRecord.getIntValue());
		
		logger.info("Waiting {} seconds for DNS record propagation.", dnsPropagationDelay.getSeconds());
		Thread.sleep(dnsPropagationDelay);
		
		CompletableFuture<Void> dnsVerifyFuture = verifyAllDnsPropagatedAsync(tasks);
		
		try {
			dnsVerifyFuture.join();
			
			// Informing the authority that they can start checking the records.
			for (Dns01Challenge challenge : challenges) {
				challenge.trigger();
			}
			
			// Waiting for authorization from the authority.
			CompletableFuture<Void> authorizationVerifyFuture = verifyAllAuthorizationCompleteAsync(order);
			authorizationVerifyFuture.join();
			
		} catch (CompletionException e) {
			Throwable cause = e.getCause();
			
			if (cause instanceof AcmeException) {
				throw (AcmeException) cause;
			} else if (cause instanceof ApiException) {
				throw (ApiException) cause;
			} else {
				throw new ApiException(e);
			}
		} finally {
			// Deleting a DNS records.
			for (DnsChallengeTask task : tasks) {
				try {
					dnsManager.deleteTxtRecord(task);
				} catch (ApiException e) {
					logger.error("An error occurred while deleting the DNS record.", e);
				}
			}
		}
	}
	
	public CompletableFuture<Void> verifyAllAuthorizationCompleteAsync(final Order order) {
		List<CompletableFuture<Boolean>> futures = order.getAuthorizations().stream()
				.map(task -> verifyAuthorizationCompleteAsync(task))
				.collect(Collectors.toList());
		
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
			logger.info("Authorization by the certification authority has been completed.");
		});
	}
	
	public CompletableFuture<Boolean> verifyAuthorizationCompleteAsync(final Authorization auth) {
		CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
		checkAuthorizationWithRetry(auth, resultFuture, 0);
		return resultFuture.orTimeout(10, TimeUnit.MINUTES);
	}
	
	private void checkAuthorizationWithRetry(final Authorization auth, final CompletableFuture<Boolean> future, final int attempt) {
		if (future.isDone())
			return;
		
		try {
			logger.info("Testing status of authorization URL {}. Attempt number {}.", auth.getLocation(), attempt + 1);
			auth.fetch();
			Status status = auth.getStatus();
			logger.info("Authorization status: {}.", status);
			
			if (status == Status.VALID) {
				future.complete(true);
				return;
			}
			
			if (status == Status.INVALID) {
				future.completeExceptionally(new AcmeException("Authorization of status " + auth.getIdentifier() + " failed (INVALID)."));
				return;
			}
			
			long delay = 5;
			Instant retryAfter = auth.getRetryAfter().orElse(null);
			
			if (retryAfter != null) {
				Instant now = Instant.now();
				
				if (retryAfter.isAfter(now)) {
					delay = Math.max(5, Math.min(Duration.between(now, retryAfter).toSeconds(), 60));
				}
			}
			
			logger.info("Authorization {}, retry after {} seconds.", auth.getIdentifier(), delay);
			
			Threading.getSingleThreadingScheduler().schedule(() -> {
				checkAuthorizationWithRetry(auth, future, attempt + 1);
			}, delay, TimeUnit.SECONDS);
		} catch (AcmeException e) {
			future.completeExceptionally(e);
		}
	}

	public CompletableFuture<Void> verifyAllDnsPropagatedAsync(final List<DnsChallengeTask> tasks) {
		// We will create a separate asynchronous "check" for each task.
		List<CompletableFuture<Boolean>> futures = tasks.stream()
				.map(task -> verifyDnsPropagatedAsync(task.getRecordName(), task.getChallenge()))
				.collect(Collectors.toList());

		// CompletableFuture.allOf will only complete when ALL futures in the list have finished running.
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
			logger.info("All DNS records have been verified locally. I am submitting the verification of the records to the certification authority.");
		});
	}

	public CompletableFuture<Boolean> verifyDnsPropagatedAsync(final String recordName, final String challenge) {
		CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
		checkDnsWithRetry(recordName, challenge, resultFuture, 0);
		return resultFuture.orTimeout(10, TimeUnit.MINUTES);
	}

	private void checkDnsWithRetry(final String recordName, final String expectedValue, final CompletableFuture<Boolean> future, final int attempt) {
		if (future.isDone())
			return;

		// Verifying whether a DNS record has been issued.
		String dns = dnsServers[attempt % dnsServers.length];
		if (isTxtRecordPropagated(recordName, expectedValue, dns, attempt)) {
			future.complete(true);
			return;
		}

		// If the record is not yet posted, we will schedule another attempt in 30 seconds.
		Threading.getSingleThreadingScheduler().schedule(() -> {
			checkDnsWithRetry(recordName, expectedValue, future, attempt + 1);
		}, 30, TimeUnit.SECONDS);
	}

	public boolean isTxtRecordPropagated(final String recordName, final String expectedValue, final String dnsServer, final int attempt) {
		logger.info("Verifying DNS record {} at server {}. Attempt number {}", recordName, dnsServer == null ? "[SYSTEM]" : dnsServer, attempt + 1);
		
		try {
			// Vytvoříme session BEZ cache
			LookupSession session = LookupSession.builder()
					.resolver(dnsServer != null ? new SimpleResolver(dnsServer) : new ExtendedResolver())
					.clearCaches()
					.build();

			Name name = Name.fromString(recordName, Name.root);
	        
			// Synchronní čekání na výsledek (v tvém případě asynchronního tasku ok)
			var result = session.lookupAsync(name, Type.TXT)
					.toCompletableFuture()
					.get(5, TimeUnit.SECONDS);

			for (Record record : result.getRecords()) {
				if (record instanceof TXTRecord txt) {
					for (String value : txt.getStrings()) {
						if (expectedValue.equals(value)) return true;
					}
				}
			}
		} catch (java.util.concurrent.ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof NoSuchDomainException) {
				logger.info("DNS record not found yet (NXDOMAIN). Waiting for propagation...");
			} else {
				logger.warn("DNS lookup failed: {}", cause.getMessage());
			}
		} catch (Exception e) {
			logger.error("Unexpected error during DNS lookup", e);
		}
		
		return false;
	}
}
