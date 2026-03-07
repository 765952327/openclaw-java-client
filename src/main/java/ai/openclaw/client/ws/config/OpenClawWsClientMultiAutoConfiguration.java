package ai.openclaw.client.ws.config;

import ai.openclaw.client.config.MultiOpenClawProperties;
import ai.openclaw.client.ws.AgentStreamCallback;
import ai.openclaw.client.ws.ClientMetrics;
import ai.openclaw.client.ws.OpenClawWsClient;
import ai.openclaw.client.ws.OpenClawWsClientManager;
import ai.openclaw.client.ws.OpenClawWsClientPool;
import ai.openclaw.client.ws.WsEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AutoConfiguration
@EnableConfigurationProperties(MultiOpenClawProperties.class)
@ConditionalOnProperty(prefix = "openclaw", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenClawWsClientMultiAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawWsClientMultiAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public OpenClawWsClientManager openClawWsClientManager(MultiOpenClawProperties multiProperties) {
        logger.info("Initializing OpenClaw WebSocket Client Manager with {} instances", 
            multiProperties.getInstances() != null ? multiProperties.getInstances().size() : 0);
        
        Map<String, OpenClawWsClient> clients = new ConcurrentHashMap<>();
        Map<String, OpenClawWsClientPool> pools = new ConcurrentHashMap<>();
        
        if (multiProperties.getInstances() != null) {
            for (String name : multiProperties.getInstances().keySet()) {
                var config = multiProperties.getInstances().get(name);
                if (config != null && config.isEnabled() && "websocket".equalsIgnoreCase(config.getType())) {
                    var wsProps = multiProperties.getWsProperties(name);
                    if (wsProps != null) {
                        int poolSize = multiProperties.getPoolSize(name);
                        
                        // 创建连接池
                        OpenClawWsClientPool pool = new OpenClawWsClientPool(
                            wsProps.getBaseUrl(),
                            wsProps.getToken(),
                            poolSize
                        );
                        pools.put(name + "-pool", pool);
                        
                        // 创建单个客户端
                        OpenClawWsClient client = new OpenClawWsClient(
                            wsProps.getBaseUrl(),
                            wsProps.getToken(),
                            wsProps.getMaxQueueCapacity(),
                            wsProps.getDefaultRequestTimeoutMs(),
                            wsProps.getDefaultResultTimeoutMs(),
                            wsProps.isAutoReconnect(),
                            wsProps.getMaxReconnectRetries(),
                            wsProps.getReconnectInitialDelayMs(),
                            wsProps.getReconnectMaxDelayMs(),
                            wsProps.isHealthCheckEnabled(),
                            wsProps.getHealthCheckIntervalMs(),
                            wsProps.getHealthCheckTimeoutMs(),
                            wsProps.isRetryEnabled(),
                            wsProps.getMaxRetryCount(),
                            wsProps.getRetryInitialDelayMs(),
                            wsProps.getRetryMaxDelayMs(),
                            wsProps.isCompressionEnabled(),
                            wsProps.isSslVerifyEnabled(),
                            wsProps.getProxyHost(),
                            wsProps.getProxyPort() != null ? wsProps.getProxyPort() : 0
                        );
                        clients.put(name + "-ws", client);
                        
                        logger.info("Initialized WebSocket OpenClaw client: {}, pool-size: {}", name, poolSize);
                    }
                }
            }
        }
        
        OpenClawWsClientManager manager = new OpenClawWsClientManager(multiProperties, clients, pools);
        logger.info("OpenClaw WebSocket Client Manager initialized with {} clients, {} pools", 
            clients.size(), pools.size());
        
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean(name = "openClawWsClient")
    @ConditionalOnBean(MultiOpenClawProperties.class)
    public OpenClawWsClient defaultOpenClawWsClient(MultiOpenClawProperties multiProperties) {
        String defaultInstance = multiProperties.getDefaultInstance();
        if (defaultInstance == null && multiProperties.getInstances() != null) {
            defaultInstance = multiProperties.getInstances().keySet().iterator().next();
        }
        
        if (defaultInstance != null) {
            var config = multiProperties.getInstances().get(defaultInstance);
            if (config != null && "websocket".equalsIgnoreCase(config.getType())) {
                var wsProps = multiProperties.getWsProperties(defaultInstance);
                if (wsProps != null) {
                    logger.info("Creating default OpenClawWsClient for instance: {}", defaultInstance);
                    return new OpenClawWsClient(
                        wsProps.getBaseUrl(),
                        wsProps.getToken(),
                        wsProps.getMaxQueueCapacity(),
                        wsProps.getDefaultRequestTimeoutMs(),
                        wsProps.getDefaultResultTimeoutMs(),
                        wsProps.isAutoReconnect(),
                        wsProps.getMaxReconnectRetries(),
                        wsProps.getReconnectInitialDelayMs(),
                        wsProps.getReconnectMaxDelayMs(),
                        wsProps.isHealthCheckEnabled(),
                        wsProps.getHealthCheckIntervalMs(),
                        wsProps.getHealthCheckTimeoutMs(),
                        wsProps.isRetryEnabled(),
                        wsProps.getMaxRetryCount(),
                        wsProps.getRetryInitialDelayMs(),
                        wsProps.getRetryMaxDelayMs(),
                        wsProps.isCompressionEnabled(),
                        wsProps.isSslVerifyEnabled(),
                        wsProps.getProxyHost(),
                        wsProps.getProxyPort() != null ? wsProps.getProxyPort() : 0
                    );
                }
            }
        }
        
        throw new IllegalStateException("No default WebSocket OpenClaw client configured");
    }
}
