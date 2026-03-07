package cn.welsione.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenClawResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("statusCode")
    private int statusCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("error")
    private String error;

    @JsonProperty("body")
    private String body;

    public OpenClawResponse() {
    }

    public OpenClawResponse(boolean success, int statusCode) {
        this.success = success;
        this.statusCode = statusCode;
    }

    public OpenClawResponse(boolean success, int statusCode, String body) {
        this.success = success;
        this.statusCode = statusCode;
        this.body = body;
    }

    public OpenClawResponse(boolean success, int statusCode, String message, String error) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "OpenClawResponse{" +
                "success=" + success +
                ", statusCode=" + statusCode +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
