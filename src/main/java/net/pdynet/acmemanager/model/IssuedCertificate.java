package net.pdynet.acmemanager.model;

import java.time.OffsetDateTime;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class IssuedCertificate {
	private int id;

	@ColumnName("definition_id")
	private int definitionId;

	@ColumnName("order_id")
	private Integer orderId;

	@ColumnName("serial_number")
	private String serialNumber;

	@ColumnName("not_before")
	private OffsetDateTime notBefore;

	@ColumnName("not_after")
	private OffsetDateTime notAfter;

	@ColumnName("cert_pem")
	private String certPem;

	@ColumnName("chain_pem")
	private String chainPem;

	@ColumnName("private_key_pem")
	private EncryptedString privateKeyPem;

	@ColumnName("date_write")
	private OffsetDateTime dateWrite;

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
	 * @return the definitionId
	 */
	public int getDefinitionId() {
		return definitionId;
	}

	/**
	 * @param definitionId the definitionId to set
	 */
	public void setDefinitionId(int definitionId) {
		this.definitionId = definitionId;
	}

	/**
	 * @return the orderId
	 */
	public Integer getOrderId() {
		return orderId;
	}

	/**
	 * @param orderId the orderId to set
	 */
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	/**
	 * @return the serialNumber
	 */
	public String getSerialNumber() {
		return serialNumber;
	}

	/**
	 * @param serialNumber the serialNumber to set
	 */
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	/**
	 * @return the notBefore
	 */
	public OffsetDateTime getNotBefore() {
		return notBefore;
	}

	/**
	 * @param notBefore the notBefore to set
	 */
	public void setNotBefore(OffsetDateTime notBefore) {
		this.notBefore = notBefore;
	}

	/**
	 * @return the notAfter
	 */
	public OffsetDateTime getNotAfter() {
		return notAfter;
	}

	/**
	 * @param notAfter the notAfter to set
	 */
	public void setNotAfter(OffsetDateTime notAfter) {
		this.notAfter = notAfter;
	}

	/**
	 * @return the certPem
	 */
	public String getCertPem() {
		return certPem;
	}

	/**
	 * @param certPem the certPem to set
	 */
	public void setCertPem(String certPem) {
		this.certPem = certPem;
	}

	/**
	 * @return the chainPem
	 */
	public String getChainPem() {
		return chainPem;
	}

	/**
	 * @param chainPem the chainPem to set
	 */
	public void setChainPem(String chainPem) {
		this.chainPem = chainPem;
	}

	/**
	 * @return the privateKeyPem
	 */
	public EncryptedString getPrivateKeyPem() {
		return privateKeyPem;
	}

	/**
	 * @param privateKeyPem the privateKeyPem to set
	 */
	public void setPrivateKeyPem(EncryptedString privateKeyPem) {
		this.privateKeyPem = privateKeyPem;
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
}
