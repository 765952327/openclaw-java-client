package ai.openclaw.client.ws;

import ai.openclaw.client.config.MultiOpenClawProperties;
import ai.openclaw.client.exception.OpenClawException;
import ai.openclaw.client.ws.config.OpenClawWsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OpenClawWsClientManager {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawWsClientManager.class);

    private final MultiOpenClawProperties multiProperties;
    private final Map<String, OpenClawWsClient> clients = new ConcurrentHashMap<>();
    private final Map<String, OpenClawWsClientPool> pools = new ConcurrentHashMap<>();
    private final String defaultInstance;

    public OpenClawWsClientManager(MultiOpenClawProperties multiProperties) {
        this.multiProperties = multiProperties;
        this.defaultInstance = multiProperties.getDefaultInstance();
    }

    public OpenClawWsClientManager(MultiOpenClawProperties multiProperties, 
                                   Map<String, OpenClawWsClient> clients,
                                   Map<String, OpenClawWsClientPool> pools) {
        this.multiProperties = multiProperties;
        this.clients.putAll(clients);
        this.pools.putAll(pools);
        this.defaultInstance = multiProperties.getDefaultInstance();
    }

    public OpenClawWsClient getClient() {
        return getClient(defaultInstance + "-ws");
    }

    public OpenClawWsClient getClient(String instanceName) {
        String key = instanceName;
        if (!key.endsWith("-ws")) {
            key = instanceName + "-ws";
        }
        
        OpenClawWsClient client = clients.get(key);
        if (client == null) {
            throw new IllegalArgumentException("OpenClaw WebSocket instance not found: " + instanceName);
        }
        return client;
    }

    public OpenClawWsClientPool getPool() {
        return getPool(defaultInstance);
    }

    public OpenClawWsClientPool getPool(String instanceName) {
        String key = instanceName + "-pool";
        OpenClawWsClientPool pool = pools.get(key);
        if (pool == null) {
            throw new IllegalArgumentException("OpenClaw WebSocket pool not found: " + instanceName);
        }
        return pool;
    }

    public boolean hasInstance(String name) {
        return clients.containsKey(name + "-ws");
    }

    public boolean hasPool(String name) {
        return pools.containsKey(name + "-pool");
    }

    public Map<String, OpenClawWsClient> getAllClients() {
        return clients;
    }

    public Map<String, OpenClawWsClientPool> getAllPools() {
        return pools;
    }

    public AgentResult runAgent(String message) throws OpenClawException {
        return getClient().runAgent(message);
    }

    public AgentResult runAgent(String message, String agentId) throws OpenClawException {
        return getClient().runAgent(message, agentId);
    }

    public AgentResult runAgent(String instanceName, String message, String agentId) throws OpenClawException {
        return getClient(instanceName).runAgent(message, agentId);
    }

    public void connectAll() {
        clients.values().forEach(client -> {
            try {
                if (!client.isConnected()) {
                    client.connect();
                }
            } catch (Exception e) {
                logger.error("Failed to connect client: {}", e.getMessage());
            }
        });
        
        pools.values().forEach(pool -> {
            try {
                pool.connectAll();
            } catch (Exception e) {
                logger.error("Failed to connect pool: {}", e.getMessage());
            }
        });
    }

    public void closeAll() {
        clients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                logger.error("Failed to close client: {}", e.getMessage());
            }
        });
        
        pools.values().forEach(pool -> {
            try {
                pool.closeAll();
            } catch (Exception e) {
                logger.error("Failed to close pool: {}", e.getMessage());
            }
        });
        
        clients.clear();
        pools.clear();
        logger.info("All OpenClaw WebSocket clients closed");
    }
}
