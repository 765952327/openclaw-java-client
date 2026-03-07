package cn.welsione.client.ws;

public class OpenClawWsClientBuilder {

    private String baseUrl = "http://127.0.0.1:18789";
    private String token = "";
    private int maxQueueCapacity = 500;
    private long defaultRequestTimeoutMs = 60000;
    private long defaultResultTimeoutMs = 300000;
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
    private String proxyHost = null;
    private int proxyPort = 0;

    public OpenClawWsClientBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public OpenClawWsClientBuilder token(String token) {
        this.token = token;
        return this;
    }

    public OpenClawWsClientBuilder maxQueueCapacity(int maxQueueCapacity) {
        this.maxQueueCapacity = maxQueueCapacity;
        return this;
    }

    public OpenClawWsClientBuilder defaultRequestTimeoutMs(long timeoutMs) {
        this.defaultRequestTimeoutMs = timeoutMs;
        return this;
    }

    public OpenClawWsClientBuilder defaultResultTimeoutMs(long timeoutMs) {
        this.defaultResultTimeoutMs = timeoutMs;
        return this;
    }

    public OpenClawWsClientBuilder autoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }

    public OpenClawWsClientBuilder maxReconnectRetries(int maxRetries) {
        this.maxReconnectRetries = maxRetries;
        return this;
    }

    public OpenClawWsClientBuilder reconnectInitialDelayMs(long delayMs) {
        this.reconnectInitialDelayMs = delayMs;
        return this;
    }

    public OpenClawWsClientBuilder reconnectMaxDelayMs(long delayMs) {
        this.reconnectMaxDelayMs = delayMs;
        return this;
    }

    public OpenClawWsClientBuilder healthCheckEnabled(boolean enabled) {
        this.healthCheckEnabled = enabled;
        return this;
    }

    public OpenClawWsClientBuilder healthCheckIntervalMs(long intervalMs) {
        this.healthCheckIntervalMs = intervalMs;
        return this;
    }

    public OpenClawWsClientBuilder healthCheckTimeoutMs(long timeoutMs) {
        this.healthCheckTimeoutMs = timeoutMs;
        return this;
    }

    public OpenClawWsClientBuilder retryEnabled(boolean enabled) {
        this.retryEnabled = enabled;
        return this;
    }

    public OpenClawWsClientBuilder maxRetryCount(int maxCount) {
        this.maxRetryCount = maxCount;
        return this;
    }

    public OpenClawWsClientBuilder retryInitialDelayMs(long delayMs) {
        this.retryInitialDelayMs = delayMs;
        return this;
    }

    public OpenClawWsClientBuilder retryMaxDelayMs(long delayMs) {
        this.retryMaxDelayMs = delayMs;
        return this;
    }

    public OpenClawWsClientBuilder compressionEnabled(boolean enabled) {
        this.compressionEnabled = enabled;
        return this;
    }

    public OpenClawWsClientBuilder sslVerifyEnabled(boolean enabled) {
        this.sslVerifyEnabled = enabled;
        return this;
    }

    public OpenClawWsClientBuilder proxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
        return this;
    }

    public OpenClawWsClient build() {
        return new OpenClawWsClient(
            baseUrl, token,
            maxQueueCapacity, defaultRequestTimeoutMs, defaultResultTimeoutMs,
            autoReconnect, maxReconnectRetries, reconnectInitialDelayMs, reconnectMaxDelayMs,
            healthCheckEnabled, healthCheckIntervalMs, healthCheckTimeoutMs,
            retryEnabled, maxRetryCount, retryInitialDelayMs, retryMaxDelayMs,
            compressionEnabled, sslVerifyEnabled, proxyHost, proxyPort
        );
    }

    public static OpenClawWsClientBuilder create() {
        return new OpenClawWsClientBuilder();
    }

    public static OpenClawWsClientBuilder create(String baseUrl, String token) {
        return new OpenClawWsClientBuilder().baseUrl(baseUrl).token(token);
    }
}
