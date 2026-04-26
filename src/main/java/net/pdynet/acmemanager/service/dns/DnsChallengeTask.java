package net.pdynet.acmemanager.service.dns;

public class DnsChallengeTask {
	private final String recordName;
	private final String challenge;

	public DnsChallengeTask(final String recordName, final String challenge) {
		this.recordName = recordName;
		this.challenge = challenge;
	}

	public String getRecordName() {
		return recordName;
	}

	public String getChallenge() {
		return challenge;
	}
}
