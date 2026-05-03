package net.pdynet.acmemanager.service.http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pdynet.acmemanager.service.Threading;
import net.pdynet.acmemanager.util.ApiException;

public class HttpChallengeProcessor {
	private static final Logger logger = LoggerFactory.getLogger(HttpChallengeProcessor.class);
	
	public void processChallenges(final Path webrootPath, final Order order) throws InterruptedException, ApiException, AcmeException, IOException {
		List<Http01Challenge> challenges = new ArrayList<>();
		List<Path> challengeFiles = new ArrayList<>();
		
		if (!Files.isDirectory(webrootPath))
			throw new AcmeException("Webroot path is not valid folder.");
		
		boolean webrootPathWellknownCreated = false;
		Path webrootPathWellknown = webrootPath.resolve(".well-known");
		if (!Files.isDirectory(webrootPathWellknown)) {
			Files.createDirectory(webrootPathWellknown);
			webrootPathWellknownCreated = true;
		}
		
		boolean webrootPathAcmeChallengeCreated = false;
		Path webrootPathAcmeChallenge = webrootPathWellknown.resolve("acme-challenge");
		if (!Files.isDirectory(webrootPathAcmeChallenge)) {
			Files.createDirectory(webrootPathAcmeChallenge);
			webrootPathAcmeChallengeCreated = true;
		}
		
		for (Authorization auth : order.getAuthorizations()) {
			String authDomain = auth.getIdentifier().getDomain();
			
			if (authDomain.startsWith("*."))
				throw new IllegalArgumentException("Protocol error: HTTP-01 challenge cannot be used to validate wildcard domain (" + authDomain + ").");
			
			if (auth.getStatus() == Status.VALID) {
				logger.info("Authorization for domain {} is already VALID. Skipping HTTP challenge.", authDomain);
				continue; 
			}
			
			Http01Challenge challenge = auth.findChallenge(Http01Challenge.class).orElse(null);
			
			if (challenge == null) {
				throw new AcmeException("HTTP-01 challenge is not available for domain " + authDomain);
			}
			
			logger.debug("Challenge status {}", challenge.getStatus());

			if (challenge.getStatus() == Status.VALID) {
				logger.info("Challenge for domain {} is already VALID. Skipping trigger.", authDomain);
				continue;
			}
			
			String token = challenge.getToken();
			String content = challenge.getAuthorization();
			
			Path challengeFile = webrootPathAcmeChallenge.resolve(token);
			Files.writeString(challengeFile, content);
			challengeFiles.add(challengeFile);
			challenges.add(challenge);
		}
		
		try {
			for (Http01Challenge challenge : challenges) {
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
			try {
				for (Path challengeFile : challengeFiles)
					Files.delete(challengeFile);
				
				if (webrootPathAcmeChallengeCreated)
					Files.delete(webrootPathAcmeChallenge);
				
				if (webrootPathWellknownCreated)
					Files.delete(webrootPathWellknown);
				
			} catch (Exception e) {
				logger.error("An error occurred while deleting the challenge files.", e);
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
}
