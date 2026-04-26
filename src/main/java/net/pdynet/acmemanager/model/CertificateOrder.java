package net.pdynet.acmemanager.model;

import java.time.OffsetDateTime;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class CertificateOrder {
	private int id;

	@ColumnName("definition_id")
	private int definitionId;

	private String status;

	@ColumnName("order_url")
	private String orderUrl;

	@ColumnName("date_write")
	private OffsetDateTime dateWrite;

	@ColumnName("date_edit")
	private OffsetDateTime dateEdit;

	@ColumnName("error_message")
	private String errorMessage;

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
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the orderUrl
	 */
	public String getOrderUrl() {
		return orderUrl;
	}

	/**
	 * @param orderUrl the orderUrl to set
	 */
	public void setOrderUrl(String orderUrl) {
		this.orderUrl = orderUrl;
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
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
