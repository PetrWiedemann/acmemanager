package net.pdynet.acmemanager.model;

import java.time.OffsetDateTime;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class DnsProvider {
	private int id;
	private String name;

	@ColumnName("dns_service_id")
	private int dnsServiceId;

	@ColumnName("date_write")
	private OffsetDateTime dateWrite;

	@ColumnName("date_edit")
	private OffsetDateTime dateEdit;

	@ColumnName("api_key")
	private EncryptedString apiKey;

	@ColumnName("secret_key")
	private EncryptedString secretKey;

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
	 * @return the dnsServiceId
	 */
	public int getDnsServiceId() {
		return dnsServiceId;
	}

	/**
	 * @param dnsServiceId the dnsServiceId to set
	 */
	public void setDnsServiceId(int dnsServiceId) {
		this.dnsServiceId = dnsServiceId;
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
	 * @return the apiKey
	 */
	public EncryptedString getApiKey() {
		return apiKey;
	}

	/**
	 * @param apiKey the apiKey to set
	 */
	public void setApiKey(EncryptedString apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * @return the secretKey
	 */
	public EncryptedString getSecretKey() {
		return secretKey;
	}

	/**
	 * @param secretKey the secretKey to set
	 */
	public void setSecretKey(EncryptedString secretKey) {
		this.secretKey = secretKey;
	}
}
