package net.pdynet.acmemanager.model;

public class IssuedCertificateView extends IssuedCertificate {
	private String orderStatus;
	private String orderErrorMessage;

	public String getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}

	public String getOrderErrorMessage() {
		return orderErrorMessage;
	}

	public void setOrderErrorMessage(String orderErrorMessage) {
		this.orderErrorMessage = orderErrorMessage;
	}
}
