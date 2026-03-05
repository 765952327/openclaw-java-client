package ai.openclaw.client.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AsyncAgentService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAgentService.class);

    private final OpenClawWsClient wsClient;

    public AsyncAgentService(OpenClawWsClient wsClient) {
        this.wsClient = wsClient;
        
        wsClient.addEventListener(new WsEventListener() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                if ("presence".equals(event)) {
                    handlePresenceEvent(payload);
                } else if ("tick".equals(event)) {
                    handleTick();
                } else if ("shutdown".equals(event)) {
                    handleShutdown(payload);
                }
            }
        });
    }

    private void handlePresenceEvent(Map<String, Object> payload) {
        logger.debug("Presence update: {}", payload);
    }

    private void handleTick() {
        logger.debug("Tick received");
    }

    private void handleShutdown(Map<String, Object> payload) {
        String reason = (String) payload.get("reason");
        Integer restartExpectedMs = payload.get("restartExpectedMs") != null 
            ? (Integer) payload.get("restartExpectedMs") 
            : null;
        logger.warn("Gateway shutdown: reason={}, restartExpectedMs={}", reason, restartExpectedMs);
    }

    public CompletableFuture<AgentResult> runAgentAsync(String message) {
        return runAgentAsync(message, null, null, null);
    }

    public CompletableFuture<AgentResult> runAgentAsync(String message, String agentId) {
        return runAgentAsync(message, agentId, null, null);
    }

    public CompletableFuture<AgentResult> runAgentAsync(String message, String agentId, Boolean deliver) {
        return wsClient.runAgentAsync(null, message, agentId, deliver, null, null);
    }

    public CompletableFuture<AgentResult> runAgentAsync(String message, String agentId, 
            Long requestTimeoutMs, Long resultTimeoutMs) {
        return wsClient.runAgentAsync(null, message, agentId, null, requestTimeoutMs, resultTimeoutMs);
    }

    public CompletableFuture<AgentResult> runAgentAsync(String uid, String message, String agentId, 
            Boolean deliver, Long requestTimeoutMs, Long resultTimeoutMs) {
        return wsClient.runAgentAsync(uid, message, agentId, deliver, requestTimeoutMs, resultTimeoutMs);
    }

    public int getQueueSize() {
        return wsClient.getQueueSize();
    }

    public boolean isQueueFull() {
        return wsClient.isQueueFull();
    }

    public int getMaxQueueCapacity() {
        return wsClient.getMaxQueueCapacity();
    }
}
