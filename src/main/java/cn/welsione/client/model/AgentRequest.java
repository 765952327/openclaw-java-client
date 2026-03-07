package cn.welsione.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentRequest {

    @JsonProperty("message")
    private String message;

    @JsonProperty("name")
    private String name;

    @JsonProperty("sessionKey")
    private String sessionKey;

    @JsonProperty("wakeMode")
    private String wakeMode = "now";

    @JsonProperty("deliver")
    private Boolean deliver = true;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("to")
    private String to;

    @JsonProperty("model")
    private String model;

    @JsonProperty("thinking")
    private String thinking;

    @JsonProperty("timeoutSeconds")
    private Integer timeoutSeconds;

    public AgentRequest() {
    }

    public AgentRequest(String message) {
        this.message = message;
    }

    public AgentRequest(String message, String name) {
        this.message = message;
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getWakeMode() {
        return wakeMode;
    }

    public void setWakeMode(String wakeMode) {
        this.wakeMode = wakeMode;
    }

    public Boolean getDeliver() {
        return deliver;
    }

    public void setDeliver(Boolean deliver) {
        this.deliver = deliver;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getThinking() {
        return thinking;
    }

    public void setThinking(String thinking) {
        this.thinking = thinking;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AgentRequest request = new AgentRequest();

        public Builder message(String message) {
            request.message = message;
            return this;
        }

        public Builder name(String name) {
            request.name = name;
            return this;
        }

        public Builder sessionKey(String sessionKey) {
            request.sessionKey = sessionKey;
            return this;
        }

        public Builder wakeMode(String wakeMode) {
            request.wakeMode = wakeMode;
            return this;
        }

        public Builder deliver(Boolean deliver) {
            request.deliver = deliver;
            return this;
        }

        public Builder channel(String channel) {
            request.channel = channel;
            return this;
        }

        public Builder to(String to) {
            request.to = to;
            return this;
        }

        public Builder model(String model) {
            request.model = model;
            return this;
        }

        public Builder thinking(String thinking) {
            request.thinking = thinking;
            return this;
        }

        public Builder timeoutSeconds(Integer timeoutSeconds) {
            request.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public AgentRequest build() {
            return request;
        }
    }
}
