package cn.welsione.client.ws.config;

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
    
    private boolean autoReconnect = true;
    private int maxReconnectRetries = 10;
    private long reconnectInitialDelayMs = 1000;
    private long reconnectMaxDelayMs = 30000;
    
    private boolean healthCheckEnabled = true;
    private long healthCheckIntervalMs = 30000;
    private long healthCheckTimeoutMs = 10000;
    
    private boolean retryEnabled = true;
    private int maxRetryCount = 3;
    private long retryInitialDelayMs = 500;
    private long retryMaxDelayMs = 5000;
    
    private boolean compressionEnabled = true;
    private boolean sslVerifyEnabled = true;
    private String proxyHost;
    private Integer proxyPort;

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

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public int getMaxReconnectRetries() {
        return maxReconnectRetries;
    }

    public void setMaxReconnectRetries(int maxReconnectRetries) {
        this.maxReconnectRetries = maxReconnectRetries;
    }

    public long getReconnectInitialDelayMs() {
        return reconnectInitialDelayMs;
    }

    public void setReconnectInitialDelayMs(long reconnectInitialDelayMs) {
        this.reconnectInitialDelayMs = reconnectInitialDelayMs;
    }

    public long getReconnectMaxDelayMs() {
        return reconnectMaxDelayMs;
    }

    public void setReconnectMaxDelayMs(long reconnectMaxDelayMs) {
        this.reconnectMaxDelayMs = reconnectMaxDelayMs;
    }

    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    public long getHealthCheckIntervalMs() {
        return healthCheckIntervalMs;
    }

    public void setHealthCheckIntervalMs(long healthCheckIntervalMs) {
        this.healthCheckIntervalMs = healthCheckIntervalMs;
    }

    public long getHealthCheckTimeoutMs() {
        return healthCheckTimeoutMs;
    }

    public void setHealthCheckTimeoutMs(long healthCheckTimeoutMs) {
        this.healthCheckTimeoutMs = healthCheckTimeoutMs;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public long getRetryInitialDelayMs() {
        return retryInitialDelayMs;
    }

    public void setRetryInitialDelayMs(long retryInitialDelayMs) {
        this.retryInitialDelayMs = retryInitialDelayMs;
    }

    public long getRetryMaxDelayMs() {
        return retryMaxDelayMs;
    }

    public void setRetryMaxDelayMs(long retryMaxDelayMs) {
        this.retryMaxDelayMs = retryMaxDelayMs;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public boolean isSslVerifyEnabled() {
        return sslVerifyEnabled;
    }

    public void setSslVerifyEnabled(boolean sslVerifyEnabled) {
        this.sslVerifyEnabled = sslVerifyEnabled;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }
}
