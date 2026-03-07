package ai.openclaw.client.ws.config;

import ai.openclaw.client.ws.OpenClawWsClient;
import ai.openclaw.client.ws.WsEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(OpenClawWsProperties.class)
@ConditionalOnProperty(prefix = "openclaw.ws", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenClawWsClientAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawWsClientAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public OpenClawWsClient openClawWsClient(OpenClawWsProperties properties) throws IOException {
        logger.info("Initializing OpenClaw WebSocket Client with baseUrl: {}", properties.getBaseUrl());
        
        OpenClawWsClient client = new OpenClawWsClient(
            properties.getBaseUrl(),
            properties.getToken(),
            properties.getMaxQueueCapacity(),
            properties.getDefaultRequestTimeoutMs(),
            properties.getDefaultResultTimeoutMs(),
            properties.isAutoReconnect(),
            properties.getMaxReconnectRetries(),
            properties.getReconnectInitialDelayMs(),
            properties.getReconnectMaxDelayMs(),
            properties.isHealthCheckEnabled(),
            properties.getHealthCheckIntervalMs(),
            properties.getHealthCheckTimeoutMs(),
            properties.isRetryEnabled(),
            properties.getMaxRetryCount(),
            properties.getRetryInitialDelayMs(),
            properties.getRetryMaxDelayMs(),
            properties.isCompressionEnabled(),
            properties.isSslVerifyEnabled(),
            properties.getProxyHost(),
            properties.getProxyPort() != null ? properties.getProxyPort() : 0
        );
        
        client.setRequireDevice(properties.isRequireDevice());
        
        if (properties.isAutoConnect()) {
            logger.info("Auto-connecting to OpenClaw Gateway...");
            boolean connected = client.connect();
            if (connected) {
                logger.info("Successfully connected to OpenClaw Gateway");
            } else {
                logger.warn("Failed to connect to OpenClaw Gateway");
            }
        }
        
        return client;
    }

    @Bean
    @ConditionalOnMissingBean
    public WsEventListener openClawWsEventListener() {
        return new WsEventListener() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                logger.debug("OpenClaw event: {}, payload: {}", event, payload);
            }

            @Override
            public void onAgentEvent(String runId, String status, Map<String, Object> payload) {
                logger.info("Agent event: runId={}, status={}", runId, status);
            }

            @Override
            public void onChatEvent(String runId, String sessionKey, String state, Map<String, Object> message) {
                logger.debug("Chat event: runId={}, sessionKey={}, state={}", runId, sessionKey, state);
            }

            @Override
            public void onPresenceUpdate(Map<String, Object> presence) {
                logger.debug("Presence update: {}", presence);
            }

            @Override
            public void onTick() {
                logger.trace("Tick received");
            }

            @Override
            public void onError(Throwable t) {
                logger.error("OpenClaw error: {}", t.getMessage(), t);
            }

            @Override
            public void onHealthCheck(boolean healthy, Throwable error) {
                if (healthy) {
                    logger.debug("Health check passed");
                } else {
                    logger.warn("Health check failed: {}", error != null ? error.getMessage() : "unknown");
                }
            }

            @Override
            public void onReconnected() {
                logger.info("Reconnected to OpenClaw Gateway");
            }

            @Override
            public void onReconnectFailed(Throwable t) {
                logger.error("Reconnection failed: {}", t.getMessage());
            }

            @Override
            public void onAgentStart(String runId, Map<String, Object> metadata) {
                logger.info("Agent started: runId={}", runId);
            }

            @Override
            public void onAgentComplete(String runId, String summary, Map<String, Object> result) {
                logger.info("Agent completed: runId={}, summary={}", runId, summary);
            }

            @Override
            public void onAgentError(String runId, String error) {
                logger.error("Agent error: runId={}, error={}", runId, error);
            }
        };
    }
}
