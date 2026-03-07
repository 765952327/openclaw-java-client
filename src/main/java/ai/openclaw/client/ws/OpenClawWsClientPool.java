package ai.openclaw.client.ws;

import ai.openclaw.client.exception.OpenClawConnectionException;
import ai.openclaw.client.exception.OpenClawErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class OpenClawWsClientPool {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawWsClientPool.class);

    private final String baseUrl;
    private final String token;
    private final int poolSize;
    private final List<OpenClawWsClient> clients;
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private final ConcurrentHashMap<String, OpenClawWsClient> clientMap = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public OpenClawWsClientPool(String baseUrl, String token, int poolSize) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.poolSize = poolSize;
        this.clients = new ArrayList<>(poolSize);
        this.executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "openclaw-pool");
            t.setDaemon(true);
            return t;
        });
        
        initializeClients();
    }

    private void initializeClients() {
        for (int i = 0; i < poolSize; i++) {
            try {
                OpenClawWsClient client = new OpenClawWsClient(baseUrl, token);
                clients.add(client);
                clientMap.put("client-" + i, client);
            } catch (Exception e) {
                logger.warn("Failed to create client {}: {}", i, e.getMessage());
            }
        }
        logger.info("Connection pool initialized with {} clients", clients.size());
    }

    public OpenClawWsClient getClient() {
        if (clients.isEmpty()) {
            throw new OpenClawConnectionException(OpenClawErrorCode.CONNECTION_FAILED, "No clients available in pool");
        }
        int index = Math.abs(roundRobinIndex.getAndIncrement() % poolSize);
        return clients.get(index);
    }

    public OpenClawWsClient getClient(String key) {
        return clientMap.get(key);
    }

    public OpenClawWsClient getClient(int index) {
        if (index < 0 || index >= clients.size()) {
            throw new IndexOutOfBoundsException("Invalid client index: " + index);
        }
        return clients.get(index);
    }

    public void connectAll() {
        for (OpenClawWsClient client : clients) {
            executor.submit(() -> {
                try {
                    client.connect();
                } catch (Exception e) {
                    logger.error("Failed to connect client: {}", e.getMessage());
                }
            });
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Pool connect timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void closeAll() {
        for (OpenClawWsClient client : clients) {
            try {
                client.close();
            } catch (Exception e) {
                logger.warn("Error closing client: {}", e.getMessage());
            }
        }
        logger.info("Connection pool closed");
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getActiveCount() {
        return (int) clients.stream().filter(OpenClawWsClient::isConnected).count();
    }

    public List<OpenClawWsClient> getClients() {
        return new ArrayList<>(clients);
    }
}
