package ai.openclaw.client.config;

import ai.openclaw.client.OpenClawClient;
import ai.openclaw.client.OpenClawClientManager;
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
public class OpenClawClientMultiAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawClientMultiAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public MultiOpenClawProperties multiOpenClawProperties() {
        return new MultiOpenClawProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenClawClientManager openClawClientManager(MultiOpenClawProperties multiProperties) {
        logger.info("Initializing OpenClaw Client Manager with {} instances", 
            multiProperties.getInstances() != null ? multiProperties.getInstances().size() : 0);
        
        Map<String, OpenClawClient> clients = new ConcurrentHashMap<>();
        
        if (multiProperties.getInstances() != null) {
            for (String name : multiProperties.getInstances().keySet()) {
                var config = multiProperties.getInstances().get(name);
                if (config != null && config.isEnabled() && "http".equalsIgnoreCase(config.getType())) {
                    var props = multiProperties.getProperties(name);
                    if (props != null) {
                        clients.put(name, new OpenClawClient(props));
                        logger.info("Initialized HTTP OpenClaw client: {}", name);
                    }
                }
            }
        }
        
        OpenClawClientManager manager = new OpenClawClientManager(multiProperties, clients);
        logger.info("OpenClaw Client Manager initialized with {} HTTP clients", clients.size());
        
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean(name = "openClawClient")
    @ConditionalOnBean(MultiOpenClawProperties.class)
    public OpenClawClient defaultOpenClawClient(MultiOpenClawProperties multiProperties) {
        String defaultInstance = multiProperties.getDefaultInstance();
        if (defaultInstance == null && multiProperties.getInstances() != null) {
            defaultInstance = multiProperties.getInstances().keySet().iterator().next();
        }
        
        if (defaultInstance != null) {
            var props = multiProperties.getProperties(defaultInstance);
            if (props != null) {
                logger.info("Creating default OpenClawClient for instance: {}", defaultInstance);
                return new OpenClawClient(props);
            }
        }
        
        throw new IllegalStateException("No default OpenClaw client configured");
    }
}
