package ai.openclaw.client;

import ai.openclaw.client.config.MultiOpenClawProperties;
import ai.openclaw.client.config.OpenClawProperties;
import ai.openclaw.client.model.OpenClawResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OpenClawClientManager {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawClientManager.class);

    private final MultiOpenClawProperties multiProperties;
    private final Map<String, OpenClawClient> clients = new ConcurrentHashMap<>();
    private final String defaultInstance;

    public OpenClawClientManager(MultiOpenClawProperties multiProperties) {
        this.multiProperties = multiProperties;
        this.defaultInstance = multiProperties.getDefaultInstance();
        initializeClients();
    }

    public OpenClawClientManager(MultiOpenClawProperties multiProperties, Map<String, OpenClawClient> clients) {
        this.multiProperties = multiProperties;
        this.clients.putAll(clients);
        this.defaultInstance = multiProperties.getDefaultInstance();
    }

    private void initializeClients() {
        if (multiProperties.getInstances() != null) {
            for (String name : multiProperties.getInstances().keySet()) {
                OpenClawProperties props = multiProperties.getProperties(name);
                if (props != null) {
                    clients.put(name, new OpenClawClient(props));
                    logger.info("Initialized OpenClaw client: {}", name);
                }
            }
        }
    }

    public OpenClawClient getClient() {
        return getClient(defaultInstance);
    }

    public OpenClawClient getClient(String instanceName) {
        String name = instanceName != null ? instanceName : defaultInstance;
        OpenClawClient client = clients.get(name);
        if (client == null) {
            throw new IllegalArgumentException("OpenClaw instance not found: " + name);
        }
        return client;
    }

    public boolean hasInstance(String name) {
        return clients.containsKey(name);
    }

    public Map<String, OpenClawClient> getAllClients() {
        return clients;
    }

    public OpenClawResponse wake(String text) {
        return wake(text, "now");
    }

    public OpenClawResponse wake(String text, String mode) {
        return getClient().wake(text, mode);
    }

    public OpenClawResponse wake(String instanceName, String text, String mode) {
        return getClient(instanceName).wake(text, mode);
    }

    public OpenClawResponse runAgent(String message) {
        return getClient().runAgent(message);
    }

    public OpenClawResponse runAgent(String message, String agentName) {
        return getClient().runAgent(message, agentName);
    }

    public OpenClawResponse runAgent(String instanceName, String message, String agentName) {
        return getClient(instanceName).runAgent(message, agentName);
    }

    public void close() {
        clients.values().forEach(OpenClawClient::close);
        clients.clear();
        logger.info("All OpenClaw clients closed");
    }
}
