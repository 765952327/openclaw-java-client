package ai.openclaw.client.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@ConfigurationProperties(prefix = "openclaw")
public class OpenClawProperties {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawProperties.class);

    @JsonProperty("base-url")
    private String baseUrl = "http://127.0.0.1:18789";

    @JsonProperty("token")
    private String token;

    @JsonProperty("hooks-path")
    private String hooksPath = "/hooks";

    private HooksConfig hooks = new HooksConfig();

    @JsonIgnore
    private Map<String, ScheduledTaskConfig> scheduledTasks = new ConcurrentHashMap<>();

    @JsonProperty("default-timeout-seconds")
    private int defaultTimeoutSeconds = 120;

    @JsonProperty("default-wake-mode")
    private String defaultWakeMode = "now";

    @JsonProperty("connect-timeout-ms")
    private int connectTimeoutMs = 10000;

    @JsonProperty("read-timeout-ms")
    private int readTimeoutMs = 30000;

    @JsonProperty("enabled")
    private boolean enabled = true;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHooksPath() {
        return hooksPath;
    }

    public void setHooksPath(String hooksPath) {
        this.hooksPath = hooksPath;
    }

    public HooksConfig getHooks() {
        return hooks;
    }

    public void setHooks(HooksConfig hooks) {
        this.hooks = hooks;
    }

    public Map<String, ScheduledTaskConfig> getScheduledTasks() {
        return scheduledTasks;
    }

    public void setScheduledTasks(Map<String, ScheduledTaskConfig> scheduledTasks) {
        this.scheduledTasks = scheduledTasks;
    }

    public int getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    public void setDefaultTimeoutSeconds(int defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    public String getDefaultWakeMode() {
        return defaultWakeMode;
    }

    public void setDefaultWakeMode(String defaultWakeMode) {
        this.defaultWakeMode = defaultWakeMode;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void addScheduledTask(String name, ScheduledTaskConfig config) {
        this.scheduledTasks.put(name, config);
        logger.info("Added scheduled task: {}", name);
    }

    public String getFullWebhookUrl(String endpoint) {
        String basePath = hooks != null && hooks.getPath() != null ? hooks.getPath() : hooksPath;
        return baseUrl + basePath + endpoint;
    }

    public String getHooksToken() {
        return hooks != null ? hooks.getToken() : null;
    }

    public boolean isHooksEnabled() {
        return hooks != null && hooks.isEnabled();
    }

    public static class ScheduledTaskConfig {

        @JsonProperty("cron")
        private String cron;

        @JsonProperty("message")
        private String message;

        @JsonProperty("agent-name")
        private String agentName;

        @JsonProperty("session-key")
        private String sessionKey;

        @JsonProperty("wake-mode")
        private String wakeMode = "now";

        @JsonProperty("enabled")
        private boolean enabled = true;

        @JsonProperty("model")
        private String model;

        @JsonProperty("thinking")
        private String thinking;

        @JsonProperty("timeout-seconds")
        private Integer timeoutSeconds;

        @JsonProperty("channel")
        private String channel;

        @JsonProperty("to")
        private String to;

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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
    }
}
