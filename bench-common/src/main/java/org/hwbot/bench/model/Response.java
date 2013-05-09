package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
public class Response {

	private String message;
	private String status;
	private String url;
	private String technicalMessage;

	@XmlElement
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@XmlElement
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@XmlElement
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@XmlElement
	public String getTechnicalMessage() {
		return technicalMessage;
	}

	public void setTechnicalMessage(String technicalMessage) {
		this.technicalMessage = technicalMessage;
	}

	@Override
	public String toString() {
		return "Response [" + (message != null ? "message=" + message + ", " : "") + (status != null ? "status=" + status + ", " : "")
				+ (url != null ? "url=" + url : "") + "]";
	}

}
