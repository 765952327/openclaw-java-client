package ai.openclaw.client.ws;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsResponse {

    @JsonProperty("type")
    private String type;

    @JsonProperty("id")
    private String id;

    @JsonProperty("ok")
    private Boolean ok;

    @JsonProperty("payload")
    private Map<String, Object> payload;

    @JsonProperty("error")
    private WsError error;

    @JsonProperty("event")
    private String event;

    @JsonProperty("seq")
    private Integer seq;

    @JsonProperty("stateVersion")
    private Long stateVersion;

    @JsonProperty("method")
    private String method;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public WsError getError() {
        return error;
    }

    public void setError(WsError error) {
        this.error = error;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public Long getStateVersion() {
        return stateVersion;
    }

    public void setStateVersion(Long stateVersion) {
        this.stateVersion = stateVersion;
    }

    public boolean isResponse() {
        return "res".equals(type);
    }

    public boolean isEvent() {
        return "event".equals(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key) {
        if (payload == null) {
            return null;
        }
        return (T) payload.get(key);
    }

    public String getPayloadType() {
        return getPayloadValue("type");
    }

    public String getErrorCode() {
        return error != null ? error.getCode() : null;
    }

    public String getErrorMessage() {
        return error != null ? error.getMessage() : null;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public static class WsError {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("details")
        private Object details;

        @JsonProperty("retryable")
        private Boolean retryable;

        @JsonProperty("retryAfterMs")
        private Long retryAfterMs;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getDetails() {
            return details;
        }

        public void setDetails(Object details) {
            this.details = details;
        }

        public Boolean getRetryable() {
            return retryable;
        }

        public void setRetryable(Boolean retryable) {
            this.retryable = retryable;
        }

        public Long getRetryAfterMs() {
            return retryAfterMs;
        }

        public void setRetryAfterMs(Long retryAfterMs) {
            this.retryAfterMs = retryAfterMs;
        }
    }
}
