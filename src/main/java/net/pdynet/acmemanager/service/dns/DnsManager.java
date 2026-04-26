package net.pdynet.acmemanager.service.dns;

import net.pdynet.acmemanager.util.ApiException;

public interface DnsManager {
	
	/**
	 * Creates new TXT record with provided name and challenge.
	 * 
	 * @param task
	 * @throws ApiException
	 */
	public void createTxtRecord(final DnsChallengeTask task) throws ApiException;
	
	/**
	 * Delete TXT record with provided name and challenge.
	 * 
	 * @param task
	 * @throws ApiException
	 */
	public void deleteTxtRecord(final DnsChallengeTask task) throws ApiException;
}
