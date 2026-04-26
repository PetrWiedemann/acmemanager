package net.pdynet.acmemanager.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class ConfigRecord {
	@ColumnName("id")
	private String id;
	
	@ColumnName("int_val")
	private int intValue;
	
	@ColumnName("text_val")
	private String textValue;

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the intValue
	 */
	public int getIntValue() {
		return intValue;
	}

	/**
	 * @param intValue the intValue to set
	 */
	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}

	/**
	 * @return the textValue
	 */
	public String getTextValue() {
		return textValue;
	}

	/**
	 * @param textValue the textValue to set
	 */
	public void setTextValue(String textValue) {
		this.textValue = textValue;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ConfigRecord [id=").append(id).append(", intValue=").append(intValue).append(", textValue=")
				.append(textValue).append("]");
		return builder.toString();
	}
}
