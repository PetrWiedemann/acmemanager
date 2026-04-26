package net.pdynet.acmemanager.service.bot;

public class Message {
	
	private String s4wConnectId;
	private Activity activity;

	/**
	 * @return the s4wConnectId
	 */
	public String getS4wConnectId() {
		return s4wConnectId;
	}

	/**
	 * @param s4wConnectId the s4wConnectId to set
	 */
	public void setS4wConnectId(String s4wConnectId) {
		this.s4wConnectId = s4wConnectId;
	}

	/**
	 * @return the activity
	 */
	public Activity getActivity() {
		return activity;
	}

	/**
	 * @param activity the activity to set
	 */
	public void setActivity(Activity activity) {
		this.activity = activity;
	}
}
