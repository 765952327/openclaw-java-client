package ai.openclaw.client.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openclaw.ws")
public class OpenClawWsProperties {

    private String baseUrl = "http://127.0.0.1:18789";
    private String token;
    private boolean requireDevice = false;
    private int maxQueueCapacity = 500;
    private long defaultRequestTimeoutMs = 60000;
    private long defaultResultTimeoutMs = 300000;
    private boolean autoConnect = false;
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

    public boolean isRequireDevice() {
        return requireDevice;
    }

    public void setRequireDevice(boolean requireDevice) {
        this.requireDevice = requireDevice;
    }

    public int getMaxQueueCapacity() {
        return maxQueueCapacity;
    }

    public void setMaxQueueCapacity(int maxQueueCapacity) {
        this.maxQueueCapacity = maxQueueCapacity;
    }

    public long getDefaultRequestTimeoutMs() {
        return defaultRequestTimeoutMs;
    }

    public void setDefaultRequestTimeoutMs(long defaultRequestTimeoutMs) {
        this.defaultRequestTimeoutMs = defaultRequestTimeoutMs;
    }

    public long getDefaultResultTimeoutMs() {
        return defaultResultTimeoutMs;
    }

    public void setDefaultResultTimeoutMs(long defaultResultTimeoutMs) {
        this.defaultResultTimeoutMs = defaultResultTimeoutMs;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
