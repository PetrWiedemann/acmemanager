package net.pdynet.acmemanager.model;

import java.time.OffsetDateTime;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class AcmeRegistration {
	private int id;
	private String name;

	@ColumnName("date_write")
	private OffsetDateTime dateWrite;

	@ColumnName("date_edit")
	private OffsetDateTime dateEdit;

	@ColumnName("server_url")
	private String serverUrl;

	@ColumnName("account_url")
	private String accountUrl;

	@ColumnName("account_key")
	private EncryptedString accountKey;

	@ColumnName("trust_all_certificates")
	private boolean trustAllCertificates;
	
	@ColumnName("email")
	private String email;
	
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
	 * @return the serverUrl
	 */
	public String getServerUrl() {
		return serverUrl;
	}

	/**
	 * @param serverUrl the serverUrl to set
	 */
	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	/**
	 * @return the accountUrl
	 */
	public String getAccountUrl() {
		return accountUrl;
	}

	/**
	 * @param accountUrl the accountUrl to set
	 */
	public void setAccountUrl(String accountUrl) {
		this.accountUrl = accountUrl;
	}

	/**
	 * @return the accountKey
	 */
	public EncryptedString getAccountKey() {
		return accountKey;
	}

	/**
	 * @param accountKey the accountKey to set
	 */
	public void setAccountKey(EncryptedString accountKey) {
		this.accountKey = accountKey;
	}

	/**
	 * @return the trustAllCertificates
	 */
	public boolean isTrustAllCertificates() {
		return trustAllCertificates;
	}

	/**
	 * @param trustAllCertificates the trustAllCertificates to set
	 */
	public void setTrustAllCertificates(boolean trustAllCertificates) {
		this.trustAllCertificates = trustAllCertificates;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
}
