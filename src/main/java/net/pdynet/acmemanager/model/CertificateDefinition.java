package net.pdynet.acmemanager.model;

import java.time.OffsetDateTime;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class CertificateDefinition {
	private int id;
	private String name;

	@ColumnName("date_write")
	private OffsetDateTime dateWrite;

	@ColumnName("date_edit")
	private OffsetDateTime dateEdit;

	@ColumnName("domain_name")
	private String domainName;

	@ColumnName("subject_alt_names")
	private String subjectAltNames;

	@ColumnName("acme_registration_id")
	private int acmeRegistrationId;

	@ColumnName("dns_provider_id")
	private Integer dnsProviderId;

	@ColumnName("key_algorithm")
	private String keyAlgorithm;

	@ColumnName("key_size_or_curve")
	private String keySizeOrCurve;

	@ColumnName("auto_renew")
	private boolean autoRenew;

	@ColumnName("days_before_expiry_to_renew")
	private int daysBeforeExpiryToRenew;

	@ColumnName("auto_export")
	private boolean autoExport;

	@ColumnName("export_path_cert")
	private String exportPathCert;

	@ColumnName("export_path_chain")
	private String exportPathChain;

	@ColumnName("export_path_key")
	private String exportPathKey;

	@ColumnName("run_script")
	private boolean runScript;

	@ColumnName("script_path")
	private String scriptPath;

	@ColumnName("send_to_webhook")
	private boolean sendToWebhook;
	
	@ColumnName("webhook_url")
	private String webhookUrl;
	
	@ColumnName("webhook_headers")
	private String webhookHeaders;
	
	@ColumnName("webhook_payload_id")
	private String webhookPayloadId;
	
	@ColumnName("webhook_trust_all")
	private boolean webhookTrustAll;
	
	@ColumnName("webhook_password")
	private EncryptedString webhookPassword;
	
	@ColumnName("auto_export_jks")
	private boolean autoExportJks;
	
	@ColumnName("export_path_jks")
	private String exportPathJks;
	
	@ColumnName("jks_password")
	private EncryptedString jksPassword;
	
	@ColumnName("challenge_type")
	private String challengeType;
	
	@ColumnName("webroot_path")
	private String webrootPath;
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the dateWrite
	 */
	public OffsetDateTime getDateWrite() {
		return dateWrite;
	}

	/**
	 * @param dateWrite the dateWrite to set
	 */
	public void setDateWrite(OffsetDateTime dateWrite) {
		this.dateWrite = dateWrite;
	}

	/**
	 * @return the dateEdit
	 */
	public OffsetDateTime getDateEdit() {
		return dateEdit;
	}

	/**
	 * @param dateEdit the dateEdit to set
	 */
	public void setDateEdit(OffsetDateTime dateEdit) {
		this.dateEdit = dateEdit;
	}

	/**
	 * @return the domainName
	 */
	public String getDomainName() {
		return domainName;
	}

	/**
	 * @param domainName the domainName to set
	 */
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	/**
	 * @return the subjectAltNames
	 */
	public String getSubjectAltNames() {
		return subjectAltNames;
	}

	/**
	 * @param subjectAltNames the subjectAltNames to set
	 */
	public void setSubjectAltNames(String subjectAltNames) {
		this.subjectAltNames = subjectAltNames;
	}

	/**
	 * @return the acmeRegistrationId
	 */
	public int getAcmeRegistrationId() {
		return acmeRegistrationId;
	}

	/**
	 * @param acmeRegistrationId the acmeRegistrationId to set
	 */
	public void setAcmeRegistrationId(int acmeRegistrationId) {
		this.acmeRegistrationId = acmeRegistrationId;
	}

	/**
	 * @return the dnsProviderId
	 */
	public Integer getDnsProviderId() {
		return dnsProviderId;
	}

	/**
	 * @param dnsProviderId the dnsProviderId to set
	 */
	public void setDnsProviderId(Integer dnsProviderId) {
		this.dnsProviderId = dnsProviderId;
	}

	/**
	 * @return the keyAlgorithm
	 */
	public String getKeyAlgorithm() {
		return keyAlgorithm;
	}

	/**
	 * @param keyAlgorithm the keyAlgorithm to set
	 */
	public void setKeyAlgorithm(String keyAlgorithm) {
		this.keyAlgorithm = keyAlgorithm;
	}

	/**
	 * @return the keySizeOrCurve
	 */
	public String getKeySizeOrCurve() {
		return keySizeOrCurve;
	}

	/**
	 * @param keySizeOrCurve the keySizeOrCurve to set
	 */
	public void setKeySizeOrCurve(String keySizeOrCurve) {
		this.keySizeOrCurve = keySizeOrCurve;
	}

	/**
	 * @return the autoRenew
	 */
	public boolean isAutoRenew() {
		return autoRenew;
	}

	/**
	 * @param autoRenew the autoRenew to set
	 */
	public void setAutoRenew(boolean autoRenew) {
		this.autoRenew = autoRenew;
	}

	/**
	 * @return the daysBeforeExpiryToRenew
	 */
	public int getDaysBeforeExpiryToRenew() {
		return daysBeforeExpiryToRenew;
	}

	/**
	 * @param daysBeforeExpiryToRenew the daysBeforeExpiryToRenew to set
	 */
	public void setDaysBeforeExpiryToRenew(int daysBeforeExpiryToRenew) {
		this.daysBeforeExpiryToRenew = daysBeforeExpiryToRenew;
	}

	/**
	 * @return the autoExport
	 */
	public boolean isAutoExport() {
		return autoExport;
	}

	/**
	 * @param autoExport the autoExport to set
	 */
	public void setAutoExport(boolean autoExport) {
		this.autoExport = autoExport;
	}

	/**
	 * @return the exportPathCert
	 */
	public String getExportPathCert() {
		return exportPathCert;
	}

	/**
	 * @param exportPathCert the exportPathCert to set
	 */
	public void setExportPathCert(String exportPathCert) {
		this.exportPathCert = exportPathCert;
	}

	/**
	 * @return the exportPathChain
	 */
	public String getExportPathChain() {
		return exportPathChain;
	}

	/**
	 * @param exportPathChain the exportPathChain to set
	 */
	public void setExportPathChain(String exportPathChain) {
		this.exportPathChain = exportPathChain;
	}

	/**
	 * @return the exportPathKey
	 */
	public String getExportPathKey() {
		return exportPathKey;
	}

	/**
	 * @param exportPathKey the exportPathKey to set
	 */
	public void setExportPathKey(String exportPathKey) {
		this.exportPathKey = exportPathKey;
	}

	/**
	 * @return the runScript
	 */
	public boolean isRunScript() {
		return runScript;
	}

	/**
	 * @param runScript the runScript to set
	 */
	public void setRunScript(boolean runScript) {
		this.runScript = runScript;
	}

	/**
	 * @return the scriptPath
	 */
	public String getScriptPath() {
		return scriptPath;
	}

	/**
	 * @param scriptPath the scriptPath to set
	 */
	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}

	/**
	 * @return the sendToWebhook
	 */
	public boolean isSendToWebhook() {
		return sendToWebhook;
	}

	/**
	 * @param sendToWebhook the sendToWebhook to set
	 */
	public void setSendToWebhook(boolean sendToWebhook) {
		this.sendToWebhook = sendToWebhook;
	}

	/**
	 * @return the webhookUrl
	 */
	public String getWebhookUrl() {
		return webhookUrl;
	}

	/**
	 * @param webhookUrl the webhookUrl to set
	 */
	public void setWebhookUrl(String webhookUrl) {
		this.webhookUrl = webhookUrl;
	}

	/**
	 * @return the webhookHeaders
	 */
	public String getWebhookHeaders() {
		return webhookHeaders;
	}

	/**
	 * @param webhookHeaders the webhookHeaders to set
	 */
	public void setWebhookHeaders(String webhookHeaders) {
		this.webhookHeaders = webhookHeaders;
	}

	/**
	 * @return the webhookTrustAll
	 */
	public boolean isWebhookTrustAll() {
		return webhookTrustAll;
	}

	/**
	 * @param webhookTrustAll the webhookTrustAll to set
	 */
	public void setWebhookTrustAll(boolean webhookTrustAll) {
		this.webhookTrustAll = webhookTrustAll;
	}

	/**
	 * @return the webhookPassword
	 */
	public EncryptedString getWebhookPassword() {
		return webhookPassword;
	}

	/**
	 * @param webhookPassword the webhookPassword to set
	 */
	public void setWebhookPassword(EncryptedString webhookPassword) {
		this.webhookPassword = webhookPassword;
	}

	/**
	 * @return the webhookPayloadId
	 */
	public String getWebhookPayloadId() {
		return webhookPayloadId;
	}

	/**
	 * @param webhookPayloadId the webhookPayloadId to set
	 */
	public void setWebhookPayloadId(String webhookPayloadId) {
		this.webhookPayloadId = webhookPayloadId;
	}

	/**
	 * @return the autoExportJks
	 */
	public boolean isAutoExportJks() {
		return autoExportJks;
	}

	/**
	 * @param autoExportJks the autoExportJks to set
	 */
	public void setAutoExportJks(boolean autoExportJks) {
		this.autoExportJks = autoExportJks;
	}

	/**
	 * @return the exportPathJks
	 */
	public String getExportPathJks() {
		return exportPathJks;
	}

	/**
	 * @param exportPathJks the exportPathJks to set
	 */
	public void setExportPathJks(String exportPathJks) {
		this.exportPathJks = exportPathJks;
	}

	/**
	 * @return the jksPassword
	 */
	public EncryptedString getJksPassword() {
		return jksPassword;
	}

	/**
	 * @param jksPassword the jksPassword to set
	 */
	public void setJksPassword(EncryptedString jksPassword) {
		this.jksPassword = jksPassword;
	}

	/**
	 * @return the challengeType
	 */
	public String getChallengeType() {
		return challengeType;
	}

	/**
	 * @param challengeType the challengeType to set
	 */
	public void setChallengeType(String challengeType) {
		this.challengeType = challengeType;
	}

	/**
	 * @return the webrootPath
	 */
	public String getWebrootPath() {
		return webrootPath;
	}

	/**
	 * @param webrootPath the webrootPath to set
	 */
	public void setWebrootPath(String webrootPath) {
		this.webrootPath = webrootPath;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CertificateDefinition [id=").append(id).append(", name=").append(name).append(", dateWrite=")
				.append(dateWrite).append(", dateEdit=").append(dateEdit).append(", domainName=").append(domainName)
				.append(", subjectAltNames=").append(subjectAltNames).append(", acmeRegistrationId=")
				.append(acmeRegistrationId).append(", dnsProviderId=").append(dnsProviderId).append(", keyAlgorithm=")
				.append(keyAlgorithm).append(", keySizeOrCurve=").append(keySizeOrCurve).append(", autoRenew=")
				.append(autoRenew).append(", daysBeforeExpiryToRenew=").append(daysBeforeExpiryToRenew)
				.append(", autoExport=").append(autoExport).append(", exportPathCert=").append(exportPathCert)
				.append(", exportPathChain=").append(exportPathChain).append(", exportPathKey=").append(exportPathKey)
				.append(", runScript=").append(runScript).append(", scriptPath=").append(scriptPath)
				.append(", sendToWebhook=").append(sendToWebhook).append(", webhookUrl=").append(webhookUrl)
				.append(", webhookHeaders=").append(webhookHeaders).append(", webhookPayloadId=")
				.append(webhookPayloadId).append(", webhookTrustAll=").append(webhookTrustAll)
				.append(", webhookPassword=").append(webhookPassword).append(", autoExportJks=").append(autoExportJks)
				.append(", exportPathJks=").append(exportPathJks).append(", jksPassword=").append(jksPassword)
				.append(", challengeType=").append(challengeType).append(", webrootPath=").append(webrootPath)
				.append("]");
		return builder.toString();
	}
}
