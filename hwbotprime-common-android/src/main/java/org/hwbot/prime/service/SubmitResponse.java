package org.hwbot.prime.service;

public class SubmitResponse {

    String url;
    String status;
    String message;
    String technicalMessage;

    @Override
    public String toString() {
        return "SubmitResponse [" + (url != null ? "url=" + url + ", " : "") + (status != null ? "status=" + status + ", " : "")
                + (message != null ? "message=" + message + ", " : "") + (technicalMessage != null ? "technicalMessage=" + technicalMessage : "") + "]";
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTechnicalMessage() {
        return technicalMessage;
    }

    public void setTechnicalMessage(String technicalMessage) {
        this.technicalMessage = technicalMessage;
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }

}
